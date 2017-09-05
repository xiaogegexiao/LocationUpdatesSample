package com.cammy.locationupdates

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

/**
 * Created by xiaomei on 4/9/17.
 */
class LocationPreferences {
    private val PREF_GEOFENCE_MODELS = "geofenceModels"
    private val PREF_UPDATING_LOCATIONS = "updatingLocations"

    var mGeofenceModelMap: MutableMap<String, GeofenceModel>? = null
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
        edit.apply()
    }

    fun load() {
        val pref = PreferenceManager.getDefaultSharedPreferences(mContext)
        mGeofenceModelMap = stringToTempCameraPasswordMap(pref.getString(PREF_GEOFENCE_MODELS, ""))
        mIsUpdatingLocations = pref.getBoolean(PREF_UPDATING_LOCATIONS, false)
    }

    private fun geofenceModelMapToString(geofenceModelMap: Map<String, GeofenceModel>?): String {
        var jsonRes: JSONObject? = JSONObject(geofenceModelMap)
        val sb = StringBuilder()
        sb.append(jsonRes.toString())
        return sb.toString()
    }

    private fun stringToTempCameraPasswordMap(mapString: String?): MutableMap<String, GeofenceModel>? {
        val type = object : TypeToken<MutableMap<String, GeofenceModel>>() {}.type
        var myMap: MutableMap<String, GeofenceModel>? = mGson?.fromJson(mapString, type)
        if (myMap == null) {
            myMap = HashMap()
        }
        return myMap
    }
}