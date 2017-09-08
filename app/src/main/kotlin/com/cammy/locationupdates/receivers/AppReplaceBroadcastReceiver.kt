package com.cammy.locationupdates.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.R
import com.cammy.locationupdates.activities.MainActivity
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.cammy.locationupdates.services.ReceiveTransitionsIntentService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.tasks.OnCompleteListener
import javax.inject.Inject

/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O
 * [android.app.PendingIntent.getBroadcast] should be used when
 * requesting location updates. Due to limits on background services,
 * [android.app.PendingIntent.getService] should not be used.
 *
 * Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 * less frequently than the interval specified in the
 * [com.google.android.gms.location.LocationRequest] when the app is no longer in the
 * foreground.
 */
class AppReplaceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onReceive(context: Context, intent: Intent?) {
        val component: AppComponent by lazy {
            DaggerAppComponent.builder()
                    .appModule(AppModule(context))
                    .build()
        }

        component.inject(this)

        if (intent != null) {
            if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
                if (mLocationPreferences.mIsUpdatingLocations) {
                    requestLocationUpdates(context)
                }
            }
        }
    }

    /**
     * Handles the Request Updates button and requests start of location updates.
     */
    fun requestLocationUpdates(context: Context) {
        try {
            Log.i(TAG, "Starting location updates ")
            mFusedLocationProviderClient.requestLocationUpdates(createLocationRequest(), getPendingIntent(context))
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
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
    private fun createLocationRequest(): LocationRequest {
        var locationRequest = LocationRequest()

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        // Note: apps running on "O" devices (regardless of targetSdkVersion) may receive updates
        // less frequently than this interval when the app is no longer in the foreground.
        locationRequest.interval = MainActivity.UPDATE_INTERVAL

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.fastestInterval = MainActivity.FASTEST_UPDATE_INTERVAL

        locationRequest.smallestDisplacement = MainActivity.MAX_DISPLACEMENT

        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        locationRequest.maxWaitTime = MainActivity.MAX_WAIT_TIME
        return locationRequest
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        // Note: for apps targeting API level 25 ("Nougat") or lower, either
        // PendingIntent.getService() or PendingIntent.getBroadcast() may be used when requesting
        // location updates. For apps targeting API level O, only
        // PendingIntent.getBroadcast() should be used. This is due to the limits placed on services
        // started in the background in "O".

        // TODO(developer): uncomment to use PendingIntent.getService().
        //        Intent intent = new Intent(this, LocationUpdatesIntentService.class);
        //        intent.setAction(LocationUpdatesIntentService.ACTION_PROCESS_UPDATES);
        //        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        val intent = Intent(context, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private val TAG = "AppReplaceReceiver"
    }
}