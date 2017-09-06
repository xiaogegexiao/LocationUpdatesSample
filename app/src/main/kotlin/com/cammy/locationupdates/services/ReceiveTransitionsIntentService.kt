package com.cammy.locationupdates.services

import android.app.IntentService
import android.app.NotificationManager
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
import javax.inject.Inject

/**
 * Created by xiaomei on 4/9/17.
 *
 * This class receives geofence transition events from Location Services, in the
 * form of an Intent containing the transition type and geofence id(s) that triggered
 * the event.
 */
class ReceiveTransitionsIntentService : IntentService("ReceiveTransitionsIntentService") {
    val MONITOR_GEOFENCE_PRESENCE = 2

    val component: AppComponent by lazy {
        DaggerAppComponent.builder().appModule(AppModule(this)).build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mNotificationManager: NotificationManager

    /**
     * Handles incoming intents
     *
     * @param intent The Intent sent by Location Services. This Intent is provided
     * to Location Services (inside a PendingIntent) when you call addGeofences()
     */
    protected override fun onHandleIntent(intent: Intent) {

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
                            geofencingEventList.add(GeofenceEvent(event.geofenceTransition, event.triggeringLocation))
                        }
                        notifyGeofence(geofence, transition)
                    }
                    mLocationPreferences.save()
                }
            }
        }
    }

    private fun notifyGeofence(geofence: Geofence, transition: Int) {
        val title = "Location change"
        val msg = StringBuilder()
        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> msg.append("You entered")

            Geofence.GEOFENCE_TRANSITION_EXIT -> msg.append("You left")
        }
        msg.append(" the area around geofence \"").append(geofence.requestId).append("\"")
        //        DecimalFormat df = new DecimalFormat();
        //        df.setMaximumFractionDigits(2);
        //        msg.append(" - ").append(df.format(distance)).append(":").append(location.getAccuracy());
        val mBuilder =
                    NotificationCompat.Builder(applicationContext, MainApplication.GEOFENCE_CHANNEL)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                            .setContentTitle(title.toUpperCase())
                            .setContentText(msg.toString())

        Log.d(Companion.TAG, msg.toString())
        mNotificationManager.notify(System.currentTimeMillis().toString(), MONITOR_GEOFENCE_PRESENCE, mBuilder.build())
    }

    companion object {
        val TAG = this::class.simpleName
    }
}