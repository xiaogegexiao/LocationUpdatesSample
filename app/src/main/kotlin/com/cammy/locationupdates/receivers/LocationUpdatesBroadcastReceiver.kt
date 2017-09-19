/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cammy.locationupdates.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
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
import com.google.android.gms.location.LocationResult
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
class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mNotificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent?) {
        val component: AppComponent by lazy {
            DaggerAppComponent
                    .builder()
                    .appModule(AppModule(context))
                    .build()
        }

        component.inject(this)

        if (intent != null) {
            val action = intent.action
            if (ACTION_PROCESS_UPDATES == action) {
                val result = LocationResult.extractResult(intent)
                if (result != null) {
                    val location = result.lastLocation
                    notifyLocation(context, location)
                    mLocationPreferences.mLocationUpdateList.add(location)
                    mLocationPreferences.save()
                }
            }
        }
    }

    companion object {
        private val TAG = "LUBroadcastReceiver"

        internal val ACTION_PROCESS_UPDATES = "com.cammy.locationupdates.action.PROCESS_UPDATES"
    }

    private fun notifyLocation(context: Context, location: Location) {
        val title = "Location change"
        val msg = "new Location " + location.latitude + ", " + location.longitude
        val mBuilder =
                NotificationCompat.Builder(context, MainApplication.LOCATION_UPDATE_CHANNEL)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
                        .setContentTitle(title.toUpperCase())
                        .setContentText(msg)

        Log.d(TAG, msg)
        mNotificationManager.notify(System.currentTimeMillis().toString(), ReceiveTransitionReceiver.MONITOR_LOCATION_UPDATE, mBuilder.build())
    }
}
