package com.cammy.locationupdates.dagger

import android.app.Application
import com.cammy.locationupdates.GeofenceActivity
import com.cammy.locationupdates.MainActivity
import com.cammy.locationupdates.services.ReceiveTransitionsIntentService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by xiaomei on 4/9/17.
 */
@Singleton
@Component(modules = arrayOf(AppModule::class))
interface AppComponent {
    fun inject(app: Application)
    fun inject(intentService: ReceiveTransitionsIntentService)
    fun inject(activity: GeofenceActivity)
    fun inject(activity: MainActivity)
}