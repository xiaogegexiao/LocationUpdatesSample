package com.cammy.locationupdates.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.R
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.cammy.locationupdates.services.ReceiveTransitionsIntentService
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    override fun onResult(status: Status) {
        Log.d(TAG, "result " + status.isSuccess)
        mLocationPreferences.mIsUpdatingLocations = !mLocationPreferences.mIsUpdatingLocations
        mLocationPreferences.save()

        // Disconnect the location client
        requestDisconnection()
    }

    override fun onConnected(p0: Bundle?) {
        Log.d(TAG, "onConnected")
        if (mLocationPreferences.mIsUpdatingLocations) {
            removeLocationUpdates()
        } else {
            registerLocationUpdates()
        }
    }

    override fun onConnectionSuspended(p0: Int) {
        Toast.makeText(this, "Suspended to connect to google api", Toast.LENGTH_SHORT).show()
        requestDisconnection()
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this, "Failed to connect to google api", Toast.LENGTH_SHORT).show()
        requestDisconnection()
    }

    companion object {
        val FINE_LOCATION_PERMISSION_REQUEST_CODE = 1

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5 * 60 * 1000

        /**
         * The fastest rate for active location updates. Exact. Updates will never be more frequent
         * than this value.
         */
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5

        private val TAG = MainActivity::class.simpleName

        val MONITOR_LOCATION_UPDATES = 1
    }

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
    }

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mGoogleApiClient: GoogleApiClient

    @Inject
    lateinit var mNotificationManager: NotificationManager

    private var mHandler = Handler()
    var mInprogress: Boolean = false
    var mLocationRequest: LocationRequest? = null
    var mLocationListener: LocationListener? = null
    var mAskingForPermission: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_main)

        createLocationRequest()
        createLocationListener()

        geofence_setup.setOnClickListener{ view ->
            val intent = Intent(this, GeofenceActivity::class.java)
            startActivity(intent)
        }

        location_trigger.text = if (mLocationPreferences.mIsUpdatingLocations) "stop updating location" else "start updating location"
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
                requestConnectionWithGoogleAPI()
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
                    mHandler.post(this::requestConnectionWithGoogleAPI)
                } else {
                    finish()
                }
                mAskingForPermission = false
            }
        }
    }

    private fun requestConnectionWithGoogleAPI() {
        if (mInprogress)
            return
        mInprogress = true
        mGoogleApiClient.registerConnectionCallbacks(this)
        mGoogleApiClient.registerConnectionFailedListener(this)
        mGoogleApiClient.connect()
    }

    /**
     * Get a location client and disconnect from Location Services
     */
    private fun requestDisconnection() {
        // A request is no longer in progress
        mGoogleApiClient.unregisterConnectionCallbacks(this)
        mGoogleApiClient.unregisterConnectionFailedListener(this)
        mGoogleApiClient.disconnect()
        mInprogress = false
        location_trigger.text = if (mLocationPreferences.mIsUpdatingLocations) "stop updating location" else "start updating location"
    }

    fun registerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            mInprogress = false
            return
        }
        Log.d(TAG, "registerLocationUpdates")
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener).setResultCallback(this)
    }

    fun removeLocationUpdates() {
        Log.d(TAG, "removeLocationUpdates")
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener)
        mLocationPreferences.mIsUpdatingLocations = false
        mLocationPreferences.save()
        requestDisconnection()
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     *
     *
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     *
     *
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private fun createLocationRequest() : LocationRequest? {
        if (mLocationRequest != null) {
            return mLocationRequest
        }
        mLocationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest?.interval = UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest?.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS

        mLocationRequest?.smallestDisplacement = 50f

        mLocationRequest?.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        return mLocationRequest
    }

    /**
     * Creates a callback for receiving location events.
     */
    private fun createLocationListener() : LocationListener? {
        if (mLocationListener != null) {
            return mLocationListener
        }
        mLocationListener = LocationListener { location ->
            mLocationPreferences.mLocationUpdateList.add(location)
            mLocationPreferences.save()
            notifyGeofence(location)
            Log.d(TAG, "received location update " + location.latitude + ", " + location.longitude)
        }
        return mLocationListener
    }

    private fun notifyGeofence(location: Location) {
        val title = "Location change"
        val msg = "received location update " + location.latitude + ", " + location.longitude
        val mBuilder =
                NotificationCompat.Builder(applicationContext, MainApplication.LOCATION_UPDATE_CHANNEL)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title.toUpperCase())
                        .setContentText(msg)

        Log.d(ReceiveTransitionsIntentService.TAG, msg)
        mNotificationManager.notify(System.currentTimeMillis().toString(), MONITOR_LOCATION_UPDATES, mBuilder.build())
    }
}
