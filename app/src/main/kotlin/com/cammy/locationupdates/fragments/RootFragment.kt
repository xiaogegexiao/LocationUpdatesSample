package com.cammy.locationupdates.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.cammyui.adapters.listeners.TableViewClickListener
import com.cammy.cammyui.fragments.BaseFragment
import com.cammy.cammyui.models.ItemType
import com.cammy.cammyui.models.Section
import com.cammy.cammyui.models.TableItem
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.activities.GeofenceActivity
import com.cammy.locationupdates.activities.LocationListActivity
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.cammy.locationupdates.geofence.GeofenceUtils
import com.cammy.locationupdates.receivers.LocationUpdatesBroadcastReceiver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import kotlinx.android.synthetic.main.fragment_root.*
import java.util.*
import javax.inject.Inject

/**
 * Created by xiaomei on 22/9/17.
 */
class RootFragment: BaseFragment(), TableViewClickListener{
    override fun onTableItemClicked(p0: Int, p1: TableItem?) {
        when (p0) {
            R.id.table_item_setup_geofence -> {
                val intent = Intent(this.context, GeofenceActivity::class.java)
                startActivity(intent)
            }
            R.id.table_item_location_history -> {
                val intent = Intent(this.context, LocationListActivity::class.java)
                startActivity(intent)
            }
            else -> {}
        }
    }

    override fun onTableItemChecked(p0: Int, p1: Boolean, p2: TableItem?) {
        when (p0) {
            R.id.table_item_location_updates -> {
                if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    askForFineLoactionPermission()
                } else {
                    addLocationUpates()
                }
            }
            else -> {}
        }
    }

    override fun onTableItemTicked(p0: Int, p1: Boolean, p2: TableItem?) {
    }

    companion object {
        val FINE_LOCATION_PERMISSION_REQUEST_CODE = 1

        private val TAG = RootFragment::class.simpleName

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        public val UPDATE_INTERVAL: Long = 10 * 60 * 1000 // Every 10 mins

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value, but they may be less frequent.
         */
        public val FASTEST_UPDATE_INTERVAL: Long = 5 * 60 * 1000 // Every 5 min

        /**
         * The max time before batched results are delivered by location services. Results may be
         * delivered sooner than this interval.
         */
        public val MAX_WAIT_TIME = UPDATE_INTERVAL * 2 // Every 20 minutes.

        public val MAX_DISPLACEMENT = 100f // 100 meters change

        fun newInstance(): Fragment {
            val fragment = RootFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this.context))
                .build()
    }

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mNotificationManager: NotificationManager

    @Inject
    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private var mLocationRequest: LocationRequest? = null
    private var mHandler = Handler()
    var mAskingForPermission: Boolean = false
    private var mInProgress: Boolean = false

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        component.inject(this)
        createLocationRequest()

        val primativeItems = ArrayList<TableItem>()
        primativeItems.add(TableItem(R.id.table_item_setup_geofence, getString(R.string.label_setup_geofence), null, ItemType.DISCLOSURE_INDICATOR, 0, null, 0, false, false, 0))
        primativeItems.add(TableItem(R.id.table_item_location_updates, getString(R.string.label_uploading_location), null, ItemType.SWITCH, 0, null, 0, false, mLocationPreferences.mIsUpdatingLocations, 0).setCheckUpdateFun({
            mLocationPreferences.mIsUpdatingLocations
        }))
        primativeItems.add(TableItem(R.id.table_item_location_history, getString(R.string.label_location_history), null, ItemType.DISCLOSURE_INDICATOR, 0, null, 0, false, false, 0))
        table_view
                .addSection(Section(null, null, primativeItems))
        table_view.setTableViewClickListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater.inflate(R.layout.fragment_root, container, false)
        return rootView
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
    private fun createLocationRequest() {
        var locationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        // Note: apps running on "O" devices (regardless of targetSdkVersion) may receive updates
        // less frequently than this interval when the app is no longer in the foreground.
        locationRequest.interval = UPDATE_INTERVAL

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL

        locationRequest.smallestDisplacement = MAX_DISPLACEMENT

        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        locationRequest.maxWaitTime = MAX_WAIT_TIME
        mLocationRequest = locationRequest
    }

    fun askForFineLoactionPermission() {
        if (!mAskingForPermission) {
            mAskingForPermission = true
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST_CODE)
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
                    mHandler.post(this::addLocationUpates)
                } else {
                    activity.finish()
                }
                mAskingForPermission = false
            }
        }
    }

    fun addLocationUpates() {

        // If a request is not already in progress
        if (!mInProgress) {

            // Toggle the flag and continue
            mInProgress = true
            if (ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                requestLocationUpdates()
            } else {
                removeLocationUpdates()
            }
            // Request a connection to Location Services

            // If a request is in progress
        } else {
            Log.d(TAG, "currently registering location updates")
        }
    }

    override fun onResume() {
        super.onResume()
        bindView()
    }

    override fun onPause() {
        super.onPause()
    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    fun requestLocationUpdates() {
        try {
            Log.i(TAG, "Starting location updates")
            mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, getPendingIntent()).addOnCompleteListener({
                mLocationPreferences.mIsUpdatingLocations = true
                mLocationPreferences.save()
                mInProgress = false
                bindView()
            })
        } catch (e: SecurityException) {
            e.printStackTrace()
            mInProgress = false
        }
    }

    /**
     * Handles the Remove Updates button, and requests removal of location updates.
     */
    fun removeLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        mFusedLocationProviderClient.removeLocationUpdates(getPendingIntent()).addOnCompleteListener({
            mLocationPreferences.mIsUpdatingLocations = false
            mLocationPreferences.save()
            mInProgress = false
            bindView()
        })
    }

    private fun getPendingIntent(): PendingIntent {
        // Note: for apps targeting API level 25 ("Nougat") or lower, either
        // PendingIntent.getService() or PendingIntent.getBroadcast() may be used when requesting
        // location updates. For apps targeting API level O, only
        // PendingIntent.getBroadcast() should be used. This is due to the limits placed on services
        // started in the background in "O".

        // TODO(developer): uncomment to use PendingIntent.getService().
        //        Intent intent = new Intent(this, LocationUpdatesIntentService.class);
        //        intent.setAction(LocationUpdatesIntentService.ACTION_PROCESS_UPDATES);
        //        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        val intent = Intent(this.context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        return PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    fun bindView() {
        table_view.notifyChange()
    }
}