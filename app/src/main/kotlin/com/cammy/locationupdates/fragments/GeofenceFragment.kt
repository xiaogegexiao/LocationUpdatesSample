package com.cammy.locationupdates.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.cammy.cammyui.activities.BaseActivity
import com.cammy.cammyui.fragments.BaseFragment
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.adapters.GeofenceListAdapter
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_geofence.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by xiaomei on 22/9/17.
 */
class GeofenceFragment : BaseFragment(),
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnCameraMoveListener,
        TextView.OnEditorActionListener{
    override fun onEditorAction(p0: TextView?, p1: Int, p2: KeyEvent?): Boolean {
        if (p1 == EditorInfo.IME_ACTION_DONE) {
            hideSoftKeyboard()
            val searchLocation = search_bar.text.toString()
            if (!TextUtils.isEmpty(searchLocation)) {
                resolveAddressUsingGoogleMap(searchLocation)
            }
        }
        return false
    }

    private fun resolveAddressUsingGoogleMap(searchLocation: String) {
        Observable.just(searchLocation).flatMap<LatLng> { s ->
            val request = Request.Builder()
                    .url(String.format(GOOGLE_MAP_API, searchLocation))
                    .build()
            var responses: Response? = null

            try {
                responses = mOkHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            if (responses != null) {
                var longitude = 0.0
                var latitude = 0.0
                try {
                    val jsonData = responses.body()?.string()
                    val Jobject = JSONObject(jsonData)
                    val location = Jobject.getJSONArray("results").getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                    if (location != null) {
                        latitude = location.getDouble("lat")
                        longitude = location.getDouble("lng")
                    }

                    Observable.just(LatLng(latitude, longitude))
                } catch (e: Exception) {
                    Observable.error<LatLng>(e)
                }
            } else {
                Observable.error<LatLng>(IllegalArgumentException("google map response is null"))
            }
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ latLng ->
            mMap?.let {
                it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        }, { throwable ->
            Log.e(TAG, throwable.message, throwable)
            resolveAddressUsingGeocoder(searchLocation)
        })
    }

    private fun resolveAddressUsingGeocoder(searchLocation: String): Unit {
        val geocoder = Geocoder(activity)
        var addressList: List<Address>?
        try {
            addressList = geocoder.getFromLocationName(searchLocation, 1)
        } catch (e: IOException) {
            e.printStackTrace()
            addressList = null
        }

        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            mMap?.let {
                it.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(address.latitude, address.longitude), 16f))
            }
        } else {
            showErrorText("Address cannot be found")
        }
    }

    override fun onCameraMove() {
        val cameraPosition = mMap?.cameraPosition

//        mFenceMarker.setPosition(cameraPosition.target);
        mCurrentFenceCircle?.center = cameraPosition?.target
    }

    override fun onConnected(p0: Bundle?) {
        if (mUseCurrentLocation) {
            mUseCurrentLocation = false
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    companion object {
        val TAG = GeofenceFragment::class.simpleName
        private val GOOGLE_MAP_API = "http://maps.google.com/maps/api/geocode/json?address=%s&sensor=false"
        private val DEFAULT_RADIUS: Long = 150
        private val FINE_LOCATION_PERMISSION_REQUEST_CODE: Int = 0

        fun newInstance(): Fragment {
            val fragment = GeofenceFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .appModule(AppModule(this.context))
                .build()
    }

    @Inject @field:Named("general_okhttpclient")
    lateinit var mOkHttpClient: OkHttpClient

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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        component.inject(this)
        setHasOptionsMenu(true)
        mGeofenceModelMap = mLocationPreferences.mGeofenceModelMap
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build()
        }

        map_view.onCreate(savedInstanceState)
        map_view.onResume()
        setUpMapIfRequired()
        search_bar.setOnEditorActionListener(this)

        mLinearLayoutManager = LinearLayoutManager(context)
        var adapter = GeofenceListAdapter(context)

        adapter.mOnClickListener = View.OnClickListener { view ->
            var geofenceModel = view.tag as GeofenceModel

            var geofenceEventsFragment = GeofenceEventsFragment.newInstance(geofenceModel.name)
            (activity as BaseActivity).pushFragment(geofenceEventsFragment, GeofenceFragment.TAG)
        }
        mGeofenceAdapter = adapter
        recyclerView.layoutManager = mLinearLayoutManager
        recyclerView.adapter = mGeofenceAdapter
        mGeofenceAdapter?.setItems(mLocationPreferences.mGeofenceModelMap?.values?.toList())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater.inflate(R.layout.fragment_geofence, container, false)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        MenuInflater(context).inflate(R.menu.fragment_geofence, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.let { menuItem ->
            when (menuItem.itemId) {
                R.id.action_add_geofence -> kotlin.run {
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
                    inputLocationNameDialog.show(activity.fragmentManager, "input location name")
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
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                    MapsInitializer.initialize(context)
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
        if (map_view != null) {
            map_view.onDestroy()
        }
        mMap = null
        mCurrentFenceCircle = null
        super.onDestroy()
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
        bindView()
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

    fun askForFineLoactionPermission() {
        if (!mAskingForPermission) {
            mAskingForPermission = true
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST_CODE)
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
                    activity.finish()
                }
                mAskingForPermission = false
            }
        }
    }

    fun bindView() {
        mGeofenceAdapter?.notifyDataSetChanged()
    }


    protected fun hideSoftKeyboard() {
        if (activity != null) {
            val view = activity.currentFocus
            if (view != null) {
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }
}