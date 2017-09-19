package com.cammy.locationupdates.dagger

import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.activities.*
import com.cammy.locationupdates.receivers.AppReplaceBroadcastReceiver
import com.cammy.locationupdates.receivers.LocationUpdatesBroadcastReceiver
import com.cammy.locationupdates.receivers.ReceiveTransitionReceiver
import dagger.Component
import javax.inject.Singleton

/**
 * Created by xiaomei on 4/9/17.
 */
@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    fun inject(app: MainApplication)
    fun inject(activity: GeofenceActivity)
    fun inject(activity: GeofenceEventsActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: LocationListActivity)
    fun inject(activity: MapLocationActivity)
    fun inject(receiver: LocationUpdatesBroadcastReceiver)
    fun inject(receiver: AppReplaceBroadcastReceiver)
    fun inject(receiver: ReceiveTransitionReceiver)
}