package com.cammy.locationupdates.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.cammy.locationupdates.models.GeofenceEvent
import com.cammy.locationupdates.models.GeofenceModel
import com.cammy.locationupdates.receivers.ReceiveTransitionReceiver
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import java.util.*

/**
 * Created by xiaomei on 4/9/17.
 *
 * Class for connecting to Location Services and requesting geofences.
 * **
 * Note: Clients must ensure that Google Play services is available before requesting geofences.
 ** *  Use GooglePlayServicesUtil.isGooglePlayServicesAvailable() to check.
 *
 *
 *
 *
 * To use a GeofenceRequester, instantiate it and call AddGeofence(). Everything else is done
 * automatically.
 */
class GeofenceRequester(// Storage for a reference to the calling client
        //    private final Activity mActivity;
        private val mContext: Context, // Stores the current instantiation of the location client
        private val mGoogleApiClient: GoogleApiClient?) :
        ResultCallback<Status>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    companion object {
        val TAG = GeofenceRequester::class.simpleName
    }

    // Stores the PendingIntent used to send geofence transitions back to the app
    private var mGeofencePendingIntent: PendingIntent? = null

    // Stores the current list of geofences
    private var mCurrentGeofenceModels: List<GeofenceModel>? = null

    // Stores the list of geofences which need to be manually checked
    private var mManualGeofenceModels: List<GeofenceModel>? = null

    /*
     * Flag that indicates whether an add or remove request is underway. Check this
     * flag before attempting to start a new request.
     */
    /**
     * The current in progress status.
     */
    // Set the "In Progress" flag.
    var inProgressFlag: Boolean = false

    init {

        // Initialize the globals to null
        mGeofencePendingIntent = null
        inProgressFlag = false
    }// Save the context

    val isAvailable: Boolean
        get() = mGoogleApiClient != null

    /**
     * Start adding geofences. Save the geofences, then start adding them by requesting a
     * connection
     *
     * @param geofenceModels A List of one or more geofenceModels to add
     */
    @Throws(UnsupportedOperationException::class)
    fun addGeofences(geofenceModels: List<GeofenceModel>, manualModels: List<GeofenceModel>) {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If a request is not already in progress
            if (!inProgressFlag) {

                /*
                 * Save the geofences so that they can be sent to Location Services once the
                 * connection is available.
                 */
                mCurrentGeofenceModels = geofenceModels
                mManualGeofenceModels = manualModels

                // Toggle the flag and continue
                inProgressFlag = true

                if (!mGoogleApiClient!!.isConnected || !mGoogleApiClient.isConnecting) {
                    requestConnection()
                } else {
                    checkGeofencePresence()
                    continueAddGeofences()
                }
                // Request a connection to Location Services

                // If a request is in progress
            } else {
                Log.d(TAG, "failed to add geofence")
            }
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

    private /*
         * Add Geofence objects to a List. toGeofence()
         * creates a Location Services Geofence object from a
         * flat object
         */ val geofenceRequest: GeofencingRequest
        get() {
            val builder = GeofencingRequest.Builder()
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            val currentGeofences = ArrayList<Geofence>()
            for (model in mCurrentGeofenceModels!!) {
                currentGeofences.add(model.toGeofence())
            }

            builder.addGeofences(currentGeofences)
            return builder.build()
        }

    /**
     * Once the connection is available, send a request to add the Geofences
     */
    private fun continueAddGeofences() {
        if (mCurrentGeofenceModels == null || mCurrentGeofenceModels!!.size <= 0) {
            // Disconnect the location client
            requestDisconnection()
            return
        }

        // Get a PendingIntent that Location Services issues when a geofence transition occurs
        mGeofencePendingIntent = createRequestPendingIntent()


        // Send a request to add the current geofences
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
        LocationServices.GeofencingApi
                .addGeofences(mGoogleApiClient, geofenceRequest, mGeofencePendingIntent)
                .setResultCallback(this)
    }

    private fun checkGeofencePresence() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        val location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        mManualGeofenceModels?.let {
            if (it.isNotEmpty() && location != null) {
                for (geofenceModel in it) {
                    val distance = FloatArray(1)
                    Location.distanceBetween(
                            location.latitude,
                            location.longitude,
                            geofenceModel.latitude,
                            geofenceModel.longitude,
                            distance
                    )
                    val geofenceEvent = GeofenceEvent(geofenceModel.name, if (distance[0] < geofenceModel.radius) Geofence.GEOFENCE_TRANSITION_ENTER else Geofence.GEOFENCE_TRANSITION_EXIT, location)
                    // Create an Intent pointing to the IntentService
                    val intent = Intent(mContext, ReceiveTransitionReceiver::class.java)
                    intent.putExtra(ReceiveTransitionReceiver.EXTRA_GEOFENCE_EVENT, geofenceEvent)
                    mContext.sendBroadcast(intent)
                }
            }
        }
    }

    override fun onResult(status: Status) {
        // If adding the geocodes was successful
        if (status.isSuccess) {
            // If adding the geofences failed
            var nameList = ArrayList<String>()
            mCurrentGeofenceModels?.let {
                for (geofenceModel in it) {
                    nameList.add(geofenceModel.name)
                }
            }
            Log.d(TAG, "add geofence successfully! " + nameList)
        } else {
            // TODO when result is failure
            if (status.statusCode == 1000) {
//                val intent = Intent(ErrorReceiver.ERROR_ACTION)
//                intent.putExtra(ErrorReceiver.ERROR_TYPE, ErrorReceiver.error_location_service)
//                mContext.sendBroadcast(intent)
            }
        }

        // Disconnect the location client
        requestDisconnection()
    }

    /**
     * Get a location client and disconnect from Location Services
     */
    private fun requestDisconnection() {
        // A request is no longer in progress
        Log.d(TAG, "requestDisconnection")
        inProgressFlag = false
        mGoogleApiClient!!.unregisterConnectionCallbacks(this)
        mGoogleApiClient.unregisterConnectionFailedListener(this)
        mGoogleApiClient.disconnect()
    }

    /*
         * Called by Location Services once the location client is connected.
         *
         * Continue by adding the requested geofences.
         */
    override fun onConnected(arg0: Bundle?) {
        checkGeofencePresence()
        // Continue adding the geofences
        continueAddGeofences()
    }

    /*
     * Called by Location Services once the location client is disconnected.
     */
    override fun onConnectionSuspended(cause: Int) {
        // Turn off the request flag
        inProgressFlag = false
        mGoogleApiClient?.unregisterConnectionCallbacks(this)
        mGoogleApiClient?.unregisterConnectionFailedListener(this)
    }

    /**
     * Get a PendingIntent to send with the request to add Geofences. Location Services issues
     * the Intent inside this PendingIntent whenever a geofence transition occurs for the current
     * list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private fun createRequestPendingIntent(): PendingIntent {
        mGeofencePendingIntent?.let {
            return it
        }

        // Create an Intent pointing to the IntentService
        val intent = Intent(mContext, ReceiveTransitionReceiver::class.java)
        /*
         * Return a PendingIntent to start the IntentService.
         * Always create a PendingIntent sent to Location Services
         * with FLAG_UPDATE_CURRENT, so that sending the PendingIntent
         * again updates the original. Otherwise, Location Services
         * can't match the PendingIntent to requests made with it.
         */
        return PendingIntent.getBroadcast(
                mContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /*
     * Implementation of OnConnectionFailedListener.onConnectionFailed
     * If a connection or disconnection request fails, report the error
     * connectionResult is passed in from Location Services
     */
    override fun onConnectionFailed(connectionResult: ConnectionResult) {

        // Turn off the request flag
        inProgressFlag = false

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {

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
}