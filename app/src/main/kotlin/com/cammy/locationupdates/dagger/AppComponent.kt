package com.cammy.locationupdates.dagger

import com.cammy.locationupdates.MainApplication
import com.cammy.locationupdates.activities.*
import com.cammy.locationupdates.fragments.*
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
    fun inject(activity: GeofenceFragment)
    fun inject(activity: GeofenceEventsFragment)
    fun inject(activity: MainActivity)
    fun inject(activity: LocationListFragment)
    fun inject(activity: MapLocationFragment)
    fun inject(receiver: LocationUpdatesBroadcastReceiver)
    fun inject(receiver: AppReplaceBroadcastReceiver)
    fun inject(receiver: ReceiveTransitionReceiver)

    fun inject(fragment: RootFragment)
}