package com.cammy.locationupdates.dagger

import android.app.NotificationManager
import android.content.Context
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.geofence.GeofenceRemover
import com.cammy.locationupdates.geofence.GeofenceRequester
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by xiaomei on 4/9/17.
 */
@Module
class AppModule (private val context: Context) {
    @Provides
    @Singleton
    fun provideLocationPreferences(): LocationPreferences {
        return LocationPreferences(context)
    }

    @Provides
    @Singleton
    fun provideNotificationManager(): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideGoogleApiClient(): GoogleApiClient {
        return GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build()
    }

    @Provides
    @Singleton
    fun provideGeofenceRequester(googleApiClient: GoogleApiClient, locationPreferences: LocationPreferences): GeofenceRequester {
        return GeofenceRequester(context, googleApiClient, locationPreferences)
    }

    @Provides
    @Singleton
    fun provideGeofenceRemover(googleApiClient: GoogleApiClient, locationPreferences: LocationPreferences): GeofenceRemover {
        return GeofenceRemover(context, googleApiClient, locationPreferences)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient() : FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}