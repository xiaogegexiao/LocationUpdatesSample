package com.cammy.locationupdates

import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import com.cammy.locationupdates.models.GeofenceEvent
import com.cammy.locationupdates.models.GeofenceModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by xiaomei on 4/9/17.
 */
class LocationPreferences {
    private val PREF_GEOFENCE_MODELS = "geofenceModels"
    private val PREF_UPDATING_LOCATIONS = "updatingLocations"

    private val PREF_GEOFENCE_EVENTS = "geofenceEvents"
    private val PREF_LOCATION_EVENTS = "locationEvents"

    var mGeofenceModelMap: MutableMap<String, GeofenceModel>? = null
    var mGeofenceEventsMap: MutableMap<String, MutableList<GeofenceEvent>>? = null
    var mLocationUpdateList: MutableList<Location> = ArrayList()
    var mIsUpdatingLocations: Boolean = false
    private var mContext: Context? = null
    private var mGson: Gson? = null

    public constructor(context: Context?) {
        mContext = context?.applicationContext
        mGson = Gson()
        load()
    }

    fun save() {
        val edit = PreferenceManager.getDefaultSharedPreferences(mContext).edit()
        edit.putString(PREF_GEOFENCE_MODELS, geofenceModelMapToString(mGeofenceModelMap))
        edit.putBoolean(PREF_UPDATING_LOCATIONS, mIsUpdatingLocations)
        edit.putString(PREF_GEOFENCE_EVENTS, geofencingEventMapToString(mGeofenceEventsMap))
        edit.putString(PREF_LOCATION_EVENTS, locationListToString(mLocationUpdateList))
        edit.apply()
    }

    fun load() {
        val pref = PreferenceManager.getDefaultSharedPreferences(mContext)
        mGeofenceModelMap = stringToGeofenceModelMap(pref.getString(PREF_GEOFENCE_MODELS, ""))
        mGeofenceEventsMap = stringToGeofencingEventMap(pref.getString(PREF_GEOFENCE_EVENTS, ""))
        mIsUpdatingLocations = pref.getBoolean(PREF_UPDATING_LOCATIONS, false)
        mLocationUpdateList.addAll(stringToLocationList(pref.getString(PREF_LOCATION_EVENTS, "")))
    }

    private fun locationListToString(list: MutableList<Location>): String {
        return mGson?.toJson(list).toString()
    }

    private fun stringToLocationList(locationListString: String?): MutableList<Location> {
        val type = object : TypeToken<MutableList<Location>>() {}.type
        var locationList: MutableList<Location>? = mGson?.fromJson(locationListString, type)
        if (locationList == null) {
            locationList = ArrayList()
        }
        return locationList
    }

    private fun geofencingEventMapToString(geofencingEventMap: Map<String, List<GeofenceEvent>>?): String {
        return mGson?.toJson(geofencingEventMap).toString()
    }

    private fun stringToGeofencingEventMap(mapString: String?): MutableMap<String, MutableList<GeofenceEvent>>? {
        val type = object : TypeToken<MutableMap<String, MutableList<GeofenceEvent>>>() {}.type
        var myMap: MutableMap<String, MutableList<GeofenceEvent>>? = mGson?.fromJson(mapString, type)
        if (myMap == null) {
            myMap = HashMap()
        }
        return myMap
    }

    private fun geofenceModelMapToString(geofenceModelMap: Map<String, GeofenceModel>?): String {
        return mGson?.toJson(geofenceModelMap).toString()
    }

    private fun stringToGeofenceModelMap(mapString: String?): MutableMap<String, GeofenceModel>? {
        val type = object : TypeToken<MutableMap<String, GeofenceModel>>() {}.type
        var myMap: MutableMap<String, GeofenceModel>? = mGson?.fromJson(mapString, type)
        if (myMap == null) {
            myMap = HashMap()
        }
        return myMap
    }
}