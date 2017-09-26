package com.cammy.locationupdates.geofence

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.fragments.GeofenceFragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationServices

/**
 * Class for connecting to Location Services and removing geofences.
 *
 *
 * **
 * Note: Clients must ensure that Google Play services is available before removing geofences.
 ** *  Use GooglePlayServicesUtil.isGooglePlayServicesAvailable() to check.
 *
 *
 * To use a GeofenceRemover, instantiate it, then call either RemoveGeofencesById() or
 * RemoveGeofencesByIntent(). Everything else is done automatically.
 */
class GeofenceRemover
/**
 * Construct a GeofenceRemover for the current Context
 *
 * @param context A valid Context
 */
(// Storage for a context from the calling client
        private val mContext: Context, // Stores the current instantiation of the location client
        private val mGoogleApiClient: GoogleApiClient?) : ResultCallback<Status>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Stores the current list of geofences
    private var mCurrentGeofenceIds: List<String>? = null

    // The PendingIntent sent in removeGeofencesByIntent
    private var mCurrentIntent: PendingIntent? = null

    /*
     *  Record the type of removal. This allows continueRemoveGeofences to call the appropriate
     *  removal request method.
     */
    private var mRequestType: GeofenceUtils.REMOVE_TYPE? = null

    /*
     * Flag that indicates whether an add or remove request is underway. Check this
     * flag before attempting to start a new request.
     */
    /**
     * Get the current in progress status.
     *
     * @return The current value of the in progress flag.
     */
    /**
     * Set the "in progress" flag from a caller. This allows callers to re-set a
     * request that failed but was later fixed.
     *
     * @param flag Turn the in progress flag on or off.
     */
    // Set the "In Progress" flag.
    var inProgressFlag: Boolean = false

    init {

        // Initialize the globals to null
        mCurrentGeofenceIds = null
        inProgressFlag = false
    }// Save the context

    val isAvailable: Boolean
        get() = mGoogleApiClient != null

    @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
    fun removeGeofenceById(geofenceId: String?) {
        // If the List is empty or null, throw an error immediately
        if (null == geofenceId) {
            throw IllegalArgumentException()

            // Set the request type, store the List, and request a location client connection.
        } else {

            // If a removal request is not already in progress, continue
            if (!inProgressFlag) {
                mRequestType = GeofenceUtils.REMOVE_TYPE.LIST
                mCurrentGeofenceIds = listOf(geofenceId)
                if (!mGoogleApiClient!!.isConnected || !mGoogleApiClient.isConnecting) {
                    requestConnection()
                } else {
                    continueRemoveGeofences()
                }

                // If a removal request is in progress, throw an exception
            } else {
                throw UnsupportedOperationException()
            }
        }
    }

    /**
     * Remove the geofences in a list of geofence IDs. To remove all current geofences associated
     * with a request, you can also call removeGeofencesByIntent.
     *
     *
     * **Note: The List must contain at least one ID, otherwise an Exception is thrown**
     *
     * @param geofenceIds A List of geofence IDs
     */
    @Throws(IllegalArgumentException::class, UnsupportedOperationException::class)
    fun removeGeofencesById(geofenceIds: List<String>?) {
        // If the List is empty or null, throw an error immediately
        if (null == geofenceIds || geofenceIds.size == 0) {
            throw IllegalArgumentException()

            // Set the request type, store the List, and request a location client connection.
        } else {

            // If a removal request is not already in progress, continue
            if (!inProgressFlag) {
                mRequestType = GeofenceUtils.REMOVE_TYPE.LIST
                mCurrentGeofenceIds = geofenceIds
                requestConnection()

                // If a removal request is in progress, throw an exception
            } else {
                throw UnsupportedOperationException()
            }
        }
    }

    /**
     * Remove the geofences associated with a PendIntent. The PendingIntent is the one used
     * in the request to add the geofences; all geofences in that request are removed. To remove
     * a subset of those geofences, call removeGeofencesById().
     *
     * @param requestIntent The PendingIntent used to request the geofences
     */
    fun removeGeofencesByIntent(requestIntent: PendingIntent) {

        // If a removal request is not in progress, continue
        if (!inProgressFlag) {
            // Set the request type, store the List, and request a location client connection.
            mRequestType = GeofenceUtils.REMOVE_TYPE.INTENT
            mCurrentIntent = requestIntent
            requestConnection()

            // If a removal request is in progress, throw an exception
        } else {

            throw UnsupportedOperationException()
        }
    }

    /**
     * Once the connection is available, send a request to remove the Geofences. The method
     * signature used depends on which type of remove request was originally received.
     */
    private fun continueRemoveGeofences() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        when (mRequestType) {
        // If removeGeofencesByIntent was called
            GeofenceUtils.REMOVE_TYPE.INTENT -> LocationServices.GeofencingApi
                    .removeGeofences(mGoogleApiClient, mCurrentIntent)
                    .setResultCallback(this)

        // If removeGeofencesById was called
            GeofenceUtils.REMOVE_TYPE.LIST -> LocationServices.GeofencingApi
                    .removeGeofences(mGoogleApiClient, mCurrentGeofenceIds)
                    .setResultCallback(this)
        }
    }

    /**
     * Request a connection to Location Services. This call returns immediately,
     * but the request is not complete until onConnected() or onConnectionFailure() is called.
     */
    private fun requestConnection() {
        mGoogleApiClient!!.registerConnectionCallbacks(this)
        mGoogleApiClient.registerConnectionFailedListener(this)
        mGoogleApiClient.connect()
    }

    /**
     * Get a location client and disconnect from Location Services
     */
    private fun requestDisconnection() {

        // A request is no longer in progress
        inProgressFlag = false
        mGoogleApiClient!!.unregisterConnectionCallbacks(this)
        mGoogleApiClient.unregisterConnectionFailedListener(this)
        mGoogleApiClient.disconnect()
        /*
         * If the request was done by PendingIntent, cancel the Intent. This prevents problems if
         * the client gets disconnected before the disconnection request finishes; the location
         * updates will still be cancelled.
         */
        if (mRequestType == GeofenceUtils.REMOVE_TYPE.INTENT) {
            mCurrentIntent!!.cancel()
        }

    }

    /*
     * Called by Location Services once the location client is connected.
     *
     * Continue by removing the requested geofences.
     */
    override fun onConnected(arg0: Bundle?) {
        // Continue the request to remove the geofences
        continueRemoveGeofences()
    }

    override fun onConnectionSuspended(cause: Int) {
        // A request is no longer in progress
        inProgressFlag = false
        mGoogleApiClient!!.unregisterConnectionCallbacks(this)
        mGoogleApiClient.unregisterConnectionFailedListener(this)
    }

    /*
     * Implementation of OnConnectionFailedListener.onConnectionFailed
     * If a connection or disconnection request fails, report the error
     * connectionResult is passed in from Location Services
     */
    override fun onConnectionFailed(connectionResult: ConnectionResult) {

        // A request is no longer in progress
        inProgressFlag = false

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {

            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(mContext as Activity,
                        GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST)

                /*
             * Thrown if Google Play services canceled the original
             * PendingIntent
             */
            } catch (e: SendIntentException) {
                // Log the error
                e.printStackTrace()
            }

            /*
         * If no resolution is available, put the error code in
         * an error Intent and broadcast it back to the main Activity.
         * The Activity then displays an error dialog.
         * is out of date.
         */
        } else {

            val errorBroadcastIntent = Intent(GeofenceUtils.ACTION_CONNECTION_ERROR)
            errorBroadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeofenceUtils.EXTRA_CONNECTION_ERROR_CODE,
                            connectionResult.errorCode)
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(errorBroadcastIntent)
        }
    }

    override fun onResult(status: Status) {
        // If removing the geofences was successful
        if (status.isSuccess) {
            // If removing the geocodes failed
            Log.d(TAG, "remove geofence successfully! " + mCurrentGeofenceIds)
        } else {
            if (status.statusCode == 1000) {
                //                Intent intent = new Intent(ErrorReceiver.ERROR_ACTION);
                //                intent.putExtra(ErrorReceiver.ERROR_TYPE, ErrorReceiver.error_location_service);
                //                mContext.sendBroadcast(intent);
            }
        }

        val intent = Intent(GeofenceFragment.ACTION_GEOFENCE_UPDATES)
        mContext.sendBroadcast(intent)
        // Disconnect the location client
        requestDisconnection()
    }

    companion object {
        val TAG = GeofenceRemover::class.java.simpleName
    }
}
