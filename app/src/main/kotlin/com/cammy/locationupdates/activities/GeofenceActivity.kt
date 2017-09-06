package com.cammy.locationupdates.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.adapters.GeofenceListAdapter
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.cammy.locationupdates.fragments.AlertEditTextDialogFragment
import com.cammy.locationupdates.geofence.GeofenceRequester
import com.cammy.locationupdates.models.GeofenceModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_geofence.*
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap


/**
 * Created by xiaomei on 31/8/17.
 */
class GeofenceActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnCameraMoveListener {
    override fun onCameraMove() {
        val cameraPosition = mMap?.cameraPosition

//        mFenceMarker.setPosition(cameraPosition.target);
        mCurrentFenceCircle?.center = cameraPosition?.target
    }

    override fun onConnected(p0: Bundle?) {
        if (mUseCurrentLocation) {
            mUseCurrentLocation = false
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                askForFineLoactionPermission()
            } else {
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient)
                mLastLocation?.let { location ->
                    moveToLocation(parseLatLngRadiusToGeofence(location.latitude, location.longitude, DEFAULT_RADIUS), false)
                }
            }
        }
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    private val DEFAULT_RADIUS: Long = 200
    private val FINE_LOCATION_PERMISSION_REQUEST_CODE: Int = 0


    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    @Inject
    lateinit var mGeofenceRequester: GeofenceRequester

    private var mGeofenceModelMap: Map<String, GeofenceModel>? = null
    private var mHandler = Handler()
    private var mAskingForPermission = false
    private var mMap: GoogleMap? = null
    private var mCurrentFenceCircle: Circle? = null
    private var mExistingFenceCircleMap: MutableMap<String, Circle> = HashMap()
    private var mLastLocation: Location? = null
    private var mUseCurrentLocation: Boolean = true
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mGeofenceAdapter: GeofenceListAdapter? = null

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        mGeofenceModelMap = mLocationPreferences.mGeofenceModelMap
        setContentView(R.layout.activity_geofence)
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
        }

        map_view.onCreate(savedInstanceState)
        setUpMapIfRequired()

        mLinearLayoutManager = LinearLayoutManager(this)
        mGeofenceAdapter = GeofenceListAdapter(this)
        recyclerView.layoutManager = mLinearLayoutManager
        recyclerView.adapter = mGeofenceAdapter
        mGeofenceAdapter?.setItems(mLocationPreferences.mGeofenceModelMap?.values?.toList())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_geofence, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add_geofence -> runOnUiThread {
                    val inputLocationNameDialog = AlertEditTextDialogFragment.newInstance(
                            "Please input location name",
                            "", "",
                            "Ok",
                            "Cancel")
                    inputLocationNameDialog.dialogListener = DialogInterface.OnClickListener { dialogInterface, i ->
                        run{
                            when (i) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    mMap?.let { googleMap ->
                                        mCurrentFenceCircle?.let {
                                            if (googleMap.cameraPosition != null) {
                                                val geofenceName = inputLocationNameDialog.userInput.toString()
                                                var geofenceModel = GeofenceModel()
                                                geofenceModel.latitude = googleMap.cameraPosition.target.latitude
                                                geofenceModel.longitude = googleMap.cameraPosition.target.longitude
                                                geofenceModel.radius = it.radius.toLong()
                                                geofenceModel.name = geofenceName
                                                mGeofenceRequester.addGeofences(Arrays.asList(geofenceModel), Arrays.asList(geofenceModel))
                                                mLocationPreferences.mGeofenceModelMap?.put(geofenceName, geofenceModel)
                                                mLocationPreferences.save()
                                            }
                                        }
                                    }
                                }
                                else -> {
                                }
                            }
                        }
                    }
                    inputLocationNameDialog.show(fragmentManager, "input location name")
                }
                else -> {
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun <T1 : Any, T2 : Any, T3 : Any, T4 : Any, T5 : Any, T6 : Any> safeLet(p1: T1?, p2: T2?, p3: T3?, p4: T4?, p5: T5?, p6: T6?, block: (T1, T2, T3, T4, T5, T6) -> Unit) {
        if (p1 != null && p2 != null && p3 != null && p4 != null && p5 != null && p6 != null) block(p1, p2, p3, p4, p5, p6)
    }

    private fun setUpMapIfRequired() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            askForFineLoactionPermission()
        } else {
            if (mMap == null) {
                map_view.getMapAsync(OnMapReadyCallback { googleMap ->
                    mMap = googleMap
                    MapsInitializer.initialize(this)
                    googleMap.uiSettings.isRotateGesturesEnabled = false
                    googleMap.uiSettings.isTiltGesturesEnabled = false
                    googleMap.isMyLocationEnabled = true
                    googleMap.setOnCameraMoveListener(this)

                    if (mExistingFenceCircleMap.isEmpty()) {
                        createExistingGeofecenMarker(mMap)
                    }

                    if (/*mFenceMarker == null || */mCurrentFenceCircle == null) {
                        createCurrentFenceMarker(mMap)
                    }

                    if (mLastLocation == null) {
                        mGoogleApiClient?.let {
                            if (it.isConnected || it.isConnecting) {
                                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(it)
                            }
                        }
                    }

                    mLastLocation?.let { location ->
                        moveToLocation(parseLatLngRadiusToGeofence(location.latitude, location.longitude, DEFAULT_RADIUS), false)
                    }
                })
            }
        }
    }

    private fun createExistingGeofecenMarker(map: GoogleMap?) {
        map?.let { googleMap ->
            googleMap.clear()
            mExistingFenceCircleMap.clear()
            mLocationPreferences.mGeofenceModelMap?.let {

                for (model in it.values) {
                    var circle = googleMap.addCircle(CircleOptions()
                            .center(LatLng(model.latitude, model.longitude))
                            .radius(model.radius.toDouble())
                            .fillColor(0x40ff0000)
                            .strokeColor(Color.RED)
                            .strokeWidth(10f))
                    mExistingFenceCircleMap.put(model.name, circle)
                }
            }
        }
    }

    private fun createCurrentFenceMarker(map: GoogleMap?) {
        //  Instantiates a new CircleOptions object +  center/radius
        val radius = DEFAULT_RADIUS

        //  Get back the mutable Circle
        mCurrentFenceCircle = map?.addCircle(CircleOptions()
                .center(LatLng(map.cameraPosition.target.latitude, map.cameraPosition.target.longitude))
                .radius(radius.toDouble())
                .fillColor(0x40ff0000)
                .strokeColor(Color.BLACK)
                .strokeWidth(10f))
    }

    private fun resolveAddressUsingGeocoder(searchLocation: String) {
        val geocoder = Geocoder(this)
        var addressList: List<Address>?
        try {
            addressList = geocoder.getFromLocationName(searchLocation, 1)
        } catch (e: IOException) {
            e.printStackTrace()
            addressList = null
        }

        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(address.latitude, address.longitude), 16f))
        }
    }

    fun moveToLocation(geofence: GeofenceModel?, animated: Boolean) {
        safeLet(mMap, mCurrentFenceCircle, geofence, geofence?.latitude, geofence?.longitude, geofence?.radius) { googleMap, circle, geofenceModel, latitude, longitude, radius ->
            run {
                if (animated) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16f))
                } else {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16f))
                }
                circle.center = LatLng(latitude, longitude)
                circle.radius = radius.toDouble()
            }
        }
    }

    private fun parseLatLngRadiusToGeofence(lat: Double, lng: Double, radius: Long): GeofenceModel {
        val geofence = GeofenceModel()
        geofence.latitude = lat
        geofence.longitude = lng
        geofence.radius = radius
        return geofence
    }

    override fun onDestroy() {
        super.onDestroy()
        map_view.onDestroy()
        mMap = null
        mCurrentFenceCircle = null
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient?.connect()
        map_view.onStart()
    }

    override fun onStop() {
        map_view.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        map_view.onResume()
    }

    override fun onPause() {
        map_view.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        map_view.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map_view.onLowMemory()
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            askForFineLoactionPermission()
        } else {
            mMap?.isMyLocationEnabled = visible
        }
    }

    fun askForFineLoactionPermission() {
        if (!mAskingForPermission) {
            mAskingForPermission = true
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            FINE_LOCATION_PERMISSION_REQUEST_CODE -> {
                var granted = true
                if (grantResults.isNotEmpty())
                    granted = true
                for (grantResult in grantResults) {
                    granted = granted && grantResult == PackageManager.PERMISSION_GRANTED
                }
                if (granted) {
                    mHandler.post(this::setUpMapIfRequired)
                } else {
                    finish()
                }
                mAskingForPermission = false
            }
        }
    }
}