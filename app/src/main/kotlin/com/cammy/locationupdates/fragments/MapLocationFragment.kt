package com.cammy.locationupdates.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.cammyui.activities.BaseActivity
import com.cammy.cammyui.fragments.BaseFragment
import com.cammy.locationupdates.R
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.fragment_map.*
import java.util.*

/**
 * Created by xiaomei on 22/9/17.
 */
class MapLocationFragment : BaseFragment(){


    companion object {
        val TAG = MapLocationFragment::class.simpleName
        private val FINE_LOCATION_PERMISSION_REQUEST_CODE = 0
        val EXTRA_LOCATIONS = "extra_locations"
        fun newInstance(locations: ArrayList<Location>): Fragment {
            val fragment = MapLocationFragment()
            val bundle = Bundle()
            bundle.putParcelableArrayList(EXTRA_LOCATIONS, locations)
            fragment.arguments = bundle
            return fragment
        }
    }

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .appModule(AppModule(this.context))
                .build()
    }

    private var mLocationList: MutableList<Location> = ArrayList()
    private var mHandler = Handler()
    private var mAskingForPermission = false
    private var mMap: GoogleMap? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mLocationList.addAll(arguments.getParcelableArrayList(EXTRA_LOCATIONS))
        component.inject(this)

        map_view.onCreate(savedInstanceState)
        setUpMapIfRequired()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater.inflate(R.layout.fragment_map, container, false)
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        if (map_view != null) {
            map_view.onDestroy()
        }
        mMap = null
    }

    override fun onStart() {
        super.onStart()
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

                    var options = PolylineOptions()
                    var lastLocation: Location? = null
                    for (location in mLocationList) {
                        lastLocation = location
                        options.add(LatLng(location.latitude, location.longitude))
                    }
                    googleMap.addPolyline(options)
                    lastLocation?.let {
                        moveToLocation(it.latitude, it.longitude, true)
                    }
                })
            }
        }
    }

    fun moveToLocation(latitude: Double, longitude: Double, animated: Boolean) {
        if (animated) {
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16f))
        } else {
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16f))
        }
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
                    (activity as BaseActivity).popFragment()
                }
                mAskingForPermission = false
            }
        }
    }
}