package com.cammy.locationupdates

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import javax.inject.Inject

/**
 * Created by xiaomei on 4/9/17.
 */
class MainApplication: Application() {
    @Inject
    lateinit var mNotificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
        createNotificationChannels()
        if (!BuildConfig.DEBUG) {
            val fabric = Fabric.with(applicationContext, Crashlytics())
        }
    }

    val component: AppComponent by lazy {
        DaggerAppComponent.builder().appModule(AppModule(this)).build()
    }

    public companion object {
        val LOCATION_UPDATE_CHANNEL = "LOCATION_UPDATES"
        val GEOFENCE_CHANNEL = "GEOFENCE"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val geofenceChannel = NotificationChannel(
                    GEOFENCE_CHANNEL,
                    getString(R.string.notification_channel_geofence),
                    NotificationManager.IMPORTANCE_HIGH)

            geofenceChannel.lightColor = Color.GREEN
            geofenceChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 500, 200, 500)

            // Submit the notification channel object to the notification manager
            mNotificationManager.createNotificationChannel(geofenceChannel)


            val directMsgChannel = NotificationChannel(
                    LOCATION_UPDATE_CHANNEL,
                    getString(R.string.notification_channel_location_updates),
                    NotificationManager.IMPORTANCE_DEFAULT)

            directMsgChannel.lightColor = Color.BLUE
            mNotificationManager.createNotificationChannel(directMsgChannel)
        } else {
//            TODO("VERSION.SDK_INT < O")
        }
    }
}