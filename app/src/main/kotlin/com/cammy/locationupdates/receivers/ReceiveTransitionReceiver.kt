package com.cammy.locationupdates.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.R
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.cammy.locationupdates.models.GeofenceEvent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.ArrayList
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
class ReceiveTransitionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mNotificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        val component: AppComponent by lazy {
            DaggerAppComponent
                    .builder()
                    .appModule(AppModule(context))
                    .build()
        }

        component.inject(this)

        val result = goAsync()
        val thread = object : Thread() {
            override fun run() {

                val event = GeofencingEvent.fromIntent(intent)

                // First check for errors
                if (event == null) {
                    // Do nothing !
                } else if (event.hasError()) {

                    // Get the error code
                    val errorCode = event.errorCode

                    // If there's no error, get the transition type and create a notification
                } else {
                    // Get the type of transition (entry or exit)
                    val transition = event.geofenceTransition
                    if (transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                        Log.d(TAG, "received geofence transition " + transition)
                        val geofences = event.triggeringGeofences
                        if (geofences.size > 0) {
                            for (geofence in geofences) {
                                mLocationPreferences.mGeofenceEventsMap?.let {
                                    val geofencingEventList: MutableList<GeofenceEvent>
                                    if (it.containsKey(geofence.requestId)) {
                                        geofencingEventList = it[geofence.requestId]!!
                                    } else {
                                        geofencingEventList = ArrayList()
                                        it.put(geofence.requestId, geofencingEventList)
                                    }
                                    geofencingEventList.add(GeofenceEvent(geofence.requestId, event.geofenceTransition, event.triggeringLocation))
                                }
                                notifyGeofence(context, geofence.requestId, transition)
                            }
                            mLocationPreferences.save()
                        }
                    } else {
                        var geofenceEvent = intent.getParcelableExtra<GeofenceEvent>(EXTRA_GEOFENCE_EVENT)
                        if (geofenceEvent != null){
                            // For manual check
                            val transition = geofenceEvent.geoTransition
                            Log.d(TAG, "received manual transition " + transition)
                            if (/*transition == Geofence.GEOFENCE_TRANSITION_ENTER || */transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                                mLocationPreferences.mGeofenceEventsMap?.let {
                                    val geofencingEventList: MutableList<GeofenceEvent>
                                    if (it.containsKey(geofenceEvent.geofenceId)) {
                                        geofencingEventList = it[geofenceEvent.geofenceId]!!
                                    } else {
                                        geofencingEventList = ArrayList()
                                        it.put(geofenceEvent.geofenceId, geofencingEventList)
                                    }
                                    geofencingEventList.add(geofenceEvent)
                                }
                                notifyGeofence(context, geofenceEvent.geofenceId, transition)
                                mLocationPreferences.save()
                            }
                        }
                    }
                }


                result.resultCode = 0
                result.finish()
            }
        }
        thread.start()
    }

    private fun notifyGeofence(context: Context, geofenceId: String, transition: Int) {
        val title = "Geofence event"
        val msg = StringBuilder()
        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> msg.append("You entered")

            Geofence.GEOFENCE_TRANSITION_EXIT -> msg.append("You left")
        }
        msg.append(" the area around geofence \"").append(geofenceId).append("\"")
        //        DecimalFormat df = new DecimalFormat();
        //        df.setMaximumFractionDigits(2);
        //        msg.append(" - ").append(df.format(distance)).append(":").append(location.getAccuracy());
        val mBuilder =
                NotificationCompat.Builder(context, MainApplication.GEOFENCE_CHANNEL)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                        .setContentTitle(title.toUpperCase())
                        .setContentText(msg.toString())

        Log.d(TAG, msg.toString())
        mNotificationManager.notify(System.currentTimeMillis().toString(), MONITOR_GEOFENCE_PRESENCE, mBuilder.build())
    }

    companion object {
        val TAG = ReceiveTransitionReceiver::class.simpleName
        val MONITOR_LOCATION_UPDATE = 1
        val MONITOR_GEOFENCE_PRESENCE = 2
        val EXTRA_GEOFENCE_EVENT = "extra_geofence_event"
    }
}