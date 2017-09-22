/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cammy.locationupdates.geofence

/**
 * This class defines constants used by location sample apps.
 */
object GeofenceUtils {

    // Used to track what type of geofence removal request was made.
    enum class REMOVE_TYPE {
        INTENT, LIST
    }

    // Used to track what type of request is in process
    enum class REQUEST_TYPE {
        ADD, REMOVE
    }

    val SIGNIFICANT_CHANGE_GEOFENCE_ID = "significant_change_geofence_id"

    var SIGNIFICANT_CHANGE_RADIUS = 100L

    // Intent actions
    val ACTION_CONNECTION_ERROR = "com.cammy.cammy.geofence.ACTION_CONNECTION_ERROR"

    val ACTION_CONNECTION_SUCCESS = "com.cammy.cammy.geofence.ACTION_CONNECTION_SUCCESS"

    val ACTION_GEOFENCES_ADDED = "com.cammy.cammy.geofence.ACTION_GEOFENCES_ADDED"

    val ACTION_GEOFENCES_REMOVED = "com.cammy.cammy.geofence.ACTION_GEOFENCES_DELETED"

    val ACTION_GEOFENCE_ERROR = "com.cammy.cammy.geofence.ACTION_GEOFENCES_ERROR"

    val ACTION_GEOFENCE_TRANSITION = "com.cammy.cammy.geofence.ACTION_GEOFENCE_TRANSITION"

    val ACTION_GEOFENCE_TRANSITION_ERROR = "com.cammy.cammy.geofence.ACTION_GEOFENCE_TRANSITION_ERROR"

    // The Intent category used by all Location Services sample apps
    val CATEGORY_LOCATION_SERVICES = "com.cammy.cammy.geofence.CATEGORY_LOCATION_SERVICES"

    // Keys for extended data in Intents
    val EXTRA_CONNECTION_CODE = "com.cammy.cammy.EXTRA_CONNECTION_CODE"

    val EXTRA_CONNECTION_ERROR_CODE = "com.cammy.cammy.geofence.EXTRA_CONNECTION_ERROR_CODE"

    val EXTRA_CONNECTION_ERROR_MESSAGE = "com.cammy.cammy.geofence.EXTRA_CONNECTION_ERROR_MESSAGE"

    val EXTRA_GEOFENCE_STATUS = "com.cammy.cammy.geofence.EXTRA_GEOFENCE_STATUS"

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    val CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000

}
