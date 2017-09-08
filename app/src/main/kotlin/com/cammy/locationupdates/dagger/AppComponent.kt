package com.cammy.locationupdates.dagger

import com.cammy.locationupdates.activities.GeofenceActivity
import com.cammy.locationupdates.activities.MainActivity
import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.activities.LocationListActivity
import com.cammy.locationupdates.activities.MapLocationActivity
import com.cammy.locationupdates.receivers.AppReplaceBroadcastReceiver
import com.cammy.locationupdates.receivers.LocationUpdatesBroadcastReceiver
import com.cammy.locationupdates.services.ReceiveTransitionsIntentService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by xiaomei on 4/9/17.
 */
@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    fun inject(app: MainApplication)
    fun inject(intentService: ReceiveTransitionsIntentService)
    fun inject(activity: GeofenceActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: LocationListActivity)
    fun inject(activity: MapLocationActivity)
    fun inject(receiver: LocationUpdatesBroadcastReceiver)
    fun inject(receiver: AppReplaceBroadcastReceiver)
}