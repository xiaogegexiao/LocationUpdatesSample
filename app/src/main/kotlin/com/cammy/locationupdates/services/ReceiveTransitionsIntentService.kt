package com.cammy.locationupdates.services

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import android.location.Location
import android.support.v4.app.NotificationCompat
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
import com.cammy.locationupdates.models.GeofenceEvent
import com.cammy.locationupdates.models.GeofenceModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import java.util.*
import javax.inject.Inject

/**
 * Created by xiaomei on 4/9/17.
 *
 * This class receives geofence transition events from Location Services, in the
 * form of an Intent containing the transition type and geofence id(s) that triggered
 * the event.
 */
class ReceiveTransitionsIntentService : IntentService("ReceiveTransitionsIntentService") {

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

    @Inject
    lateinit var mGeofenceRequester: GeofenceRequester

    @Inject
    lateinit var mGeofenceRemover: GeofenceRemover

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
                        if (GeofenceUtils.SIGNIFICANT_CHANGE_GEOFENCE_ID == geofence.requestId) {
                            notifyLocation(event.triggeringLocation)
                            mLocationPreferences.mLocationUpdateList.add(event.triggeringLocation)
                            reRegisterLocationUpdateGeofence(geofence.requestId, event.triggeringLocation)
                        } else {
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
                            notifyGeofence(geofence.requestId, transition)
                        }
                    }
                    mLocationPreferences.save()
                }
            } else {
                var geofenceEvent = intent.getParcelableExtra<GeofenceEvent>(EXTRA_GEOFENCE_EVENT)
                if (geofenceEvent != null){
                    // Get the type of transition (entry or exit)
                    val transition = geofenceEvent.geoTransition
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
                        notifyGeofence(geofenceEvent.geofenceId, transition)
                        mLocationPreferences.save()
                    }
                }
            }
        }
    }

    private fun reRegisterLocationUpdateGeofence(geofenceId:String, location: Location) {
        mGeofenceRemover.removeGeofenceById(geofenceId)
        var geofenceModel = GeofenceModel()
        geofenceModel.latitude = location.latitude
        geofenceModel.longitude = location.longitude
        geofenceModel.radius = GeofenceUtils.SIGNIFICANT_CHANGE_RADIUS
        geofenceModel.name = geofenceId
        mGeofenceRequester.addGeofences(Collections.singletonList(geofenceModel), Collections.emptyList())
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
        mNotificationManager.notify(System.currentTimeMillis().toString(), MONITOR_LOCATION_UPDATE, mBuilder.build())
    }

    private fun notifyGeofence(geofenceId: String, transition: Int) {
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
                    NotificationCompat.Builder(applicationContext, MainApplication.GEOFENCE_CHANNEL)
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                            .setContentTitle(title.toUpperCase())
                            .setContentText(msg.toString())

        Log.d(TAG, msg.toString())
        mNotificationManager.notify(System.currentTimeMillis().toString(), MONITOR_GEOFENCE_PRESENCE, mBuilder.build())
    }

    companion object {
        val TAG = ReceiveTransitionsIntentService::class.simpleName
        val MONITOR_LOCATION_UPDATE = 1
        val MONITOR_GEOFENCE_PRESENCE = 2
        val EXTRA_GEOFENCE_EVENT = "extra_geofence_event"
    }
}