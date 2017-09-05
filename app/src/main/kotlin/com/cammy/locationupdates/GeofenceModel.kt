package com.cammy.locationupdates

import com.google.android.gms.location.Geofence

/**
 * Created by xiaomei on 1/9/17.
 */
class GeofenceModel {
    var name: String? = null
    var latitude: Double? = 0.0
    var longitude: Double? = 0.0
    var radius: Long? = 0

    /**
     * Creates a Location Services Geofence object from a
     * GeofenceModel.
     *
     * @return A Geofence object
     */
    fun toGeofence(): Geofence {
        // Build a new Geofence object
        return Geofence.Builder()
                .setRequestId(name)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setCircularRegion(
                        (if (latitude == null) 0.0 else latitude)!!,
                        (if (longitude == null) 0.0 else longitude)!!,
                        (if (radius == null) 0 else radius)!!.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
    }
}