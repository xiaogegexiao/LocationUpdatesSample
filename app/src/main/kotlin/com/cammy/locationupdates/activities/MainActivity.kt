package com.cammy.locationupdates.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.R
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.cammy.locationupdates.geofence.GeofenceRemover
import com.cammy.locationupdates.geofence.GeofenceRequester
import com.cammy.locationupdates.geofence.GeofenceUtils
import com.cammy.locationupdates.models.GeofenceModel
import com.cammy.locationupdates.services.ReceiveTransitionsIntentService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    override fun onConnectionFailed(p0: ConnectionResult) {
        // Turn off the request flag
        mInProgress = false
    }

    override fun onConnected(p0: Bundle?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            mInProgress = false
            return
        }
        if (!mLocationPreferences.mIsUpdatingLocations) {
            val location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
            notifyLocation(location)
            reRegisterLocationUpdateGeofence(GeofenceUtils.SIGNIFICANT_CHANGE_GEOFENCE_ID, location)
        } else {
            removeLocationUpdateGeofence(GeofenceUtils.SIGNIFICANT_CHANGE_GEOFENCE_ID)
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        // Turn off the request flag
        mInProgress = false
        mGoogleApiClient.unregisterConnectionCallbacks(this)
        mGoogleApiClient.unregisterConnectionFailedListener(this)
    }

    companion object {
        val FINE_LOCATION_PERMISSION_REQUEST_CODE = 1

        private val TAG = MainActivity::class.simpleName
    }

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
    }

    @Inject
    lateinit var mGeofenceRequester: GeofenceRequester

    @Inject
    lateinit var mGeofenceRemover: GeofenceRemover

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mGoogleApiClient: GoogleApiClient

    @Inject
    lateinit var mNotificationManager: NotificationManager

    private var mHandler = Handler()
    var mAskingForPermission: Boolean = false
    private var mInProgress: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_main)

        geofence_setup.setOnClickListener{ view ->
            val intent = Intent(this, GeofenceActivity::class.java)
            startActivity(intent)
        }

        location_trigger.setOnClickListener { view ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                askForFineLoactionPermission()
            } else {
                addLocationUpateGeofence()
            }
        }

        location_history.setOnClickListener({
            val intent = Intent(this, LocationListActivity::class.java)
            startActivity(intent)
        })
    }

    fun askForFineLoactionPermission() {
        if (!mAskingForPermission) {
            mAskingForPermission = true
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST_CODE -> {
                var granted = true
                if (grantResults.isNotEmpty())
                    granted = true
                for (grantResult in grantResults) {
                    granted = granted && grantResult == PackageManager.PERMISSION_GRANTED
                }
                if (granted) {
                    mHandler.post(this::addLocationUpateGeofence)
                } else {
                    finish()
                }
                mAskingForPermission = false
            }
        }
    }

    fun addLocationUpateGeofence() {
        // If a request is not already in progress
        if (!mInProgress) {

            // Toggle the flag and continue
            mInProgress = true

            if (!mGoogleApiClient.isConnected || !mGoogleApiClient.isConnecting) {
                requestConnection()
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    mInProgress = false
                    return
                }
                if (!mLocationPreferences.mIsUpdatingLocations) {
                    val location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
                    notifyLocation(location)
                    reRegisterLocationUpdateGeofence(GeofenceUtils.SIGNIFICANT_CHANGE_GEOFENCE_ID, location)
                } else {
                    removeLocationUpdateGeofence(GeofenceUtils.SIGNIFICANT_CHANGE_GEOFENCE_ID)
                }
            }
            // Request a connection to Location Services

            // If a request is in progress
        } else {
            Log.d(TAG, "currently registering location updates")
        }
    }

    /**
     * Request a connection to Location Services. This call returns immediately,
     * but the request is not complete until onConnected() or onConnectionFailure() is called.
     */
    private fun requestConnection() {
        mGoogleApiClient.registerConnectionCallbacks(this)
        mGoogleApiClient.registerConnectionFailedListener(this)
        mGoogleApiClient.connect()
    }

    private fun removeLocationUpdateGeofence(geofenceId: String) {
        mGeofenceRemover.removeGeofenceById(geofenceId)
        mInProgress = false
    }

    private fun reRegisterLocationUpdateGeofence(geofenceId:String, location: Location) {
        var geofenceModel = GeofenceModel()
        geofenceModel.latitude = location.latitude
        geofenceModel.longitude = location.longitude
        geofenceModel.radius = GeofenceUtils.SIGNIFICANT_CHANGE_RADIUS
        geofenceModel.name = geofenceId
        mGeofenceRequester.addGeofences(Collections.singletonList(geofenceModel), Collections.emptyList())
        mInProgress = false
    }

    private fun notifyLocation(location: Location) {
        val title = "Location change"
        val msg = "new Location " + location.latitude + ", " + location.longitude
        val mBuilder =
                NotificationCompat.Builder(applicationContext, MainApplication.LOCATION_UPDATE_CHANNEL)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                        .setContentTitle(title.toUpperCase())
                        .setContentText(msg)

        Log.d(TAG, msg)
        mNotificationManager.notify(System.currentTimeMillis().toString(), ReceiveTransitionsIntentService.MONITOR_LOCATION_UPDATE, mBuilder.build())
    }

    override fun onResume() {
        super.onResume()
        // Register mMessageReceiver to receive messages.
        val intentFilter = IntentFilter()
        intentFilter.addAction(GeofenceUtils.ACTION_LOCATION_UPDATE_STATUS)
        registerReceiver(mMessageReceiver, intentFilter)
        bindView()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mMessageReceiver)
    }

    // handler for received Intents for the "my-event" event
    private val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Extract data included in the Intent
            val action = intent.action
            when (action) {
                GeofenceUtils.ACTION_LOCATION_UPDATE_STATUS-> mHandler.post { bindView() }
                else -> {}
            }
        }
    }

    fun bindView() {
        location_trigger.text = if (mLocationPreferences.mIsUpdatingLocations) "stop updating location" else "start updating location"
    }
}
