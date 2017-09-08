package com.cammy.locationupdates.models

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

/**
 * Created by xiaomei on 6/9/17.
 */
class GeofenceEvent(var geofenceId: String, var geoTransition: Int?, var triggerLocation: Location?): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readValue(Int::class.java.classLoader) as? Int,
            parcel.readParcelable(Location::class.java.classLoader))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(geofenceId)
        parcel.writeValue(geoTransition)
        parcel.writeParcelable(triggerLocation, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GeofenceEvent> {
        override fun createFromParcel(parcel: Parcel): GeofenceEvent {
            return GeofenceEvent(parcel)
        }

        override fun newArray(size: Int): Array<GeofenceEvent?> {
            return arrayOfNulls(size)
        }
    }

}