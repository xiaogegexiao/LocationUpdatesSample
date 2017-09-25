package com.cammy.locationupdates.dagger

import android.app.NotificationManager
import android.content.Context
import com.cammy.locationupdates.BuildConfig
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.geofence.GeofenceRemover
import com.cammy.locationupdates.geofence.GeofenceRequester
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
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
    fun provideGeofenceRequester(googleApiClient: GoogleApiClient): GeofenceRequester {
        return GeofenceRequester(context, googleApiClient)
    }

    @Provides
    @Singleton
    fun provideGeofenceRemover(googleApiClient: GoogleApiClient): GeofenceRemover {
        return GeofenceRemover(context, googleApiClient)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient() : FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    @Named("log_interceptor")
    fun provideHttpLogInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        if (!BuildConfig.DEBUG) {
            interceptor.level = HttpLoggingInterceptor.Level.NONE
        } else {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        }
        return interceptor
    }

    @Provides
    @Singleton
    @Named("general_okhttpclient")
    fun provideGeneralOkHttpClient(@Named("log_interceptor") httpLogInterceptor: HttpLoggingInterceptor): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient.Builder()

        okHttpClientBuilder
                .connectTimeout(CONNECT_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
                .cache(Cache(context.cacheDir, 1024))
                .addInterceptor(httpLogInterceptor)
        return okHttpClientBuilder.build()
    }

    companion object {
        private val CONNECT_TIMEOUT_MILLIS = 15 * 1000 // 15s
        private val READ_TIMEOUT_MILLIS = 20 * 1000 // 20s
    }
}