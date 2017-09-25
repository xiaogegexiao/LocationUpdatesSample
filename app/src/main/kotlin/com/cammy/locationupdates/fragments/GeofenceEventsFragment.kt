package com.cammy.locationupdates.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.cammyui.activities.BaseActivity
import com.cammy.cammyui.fragments.BaseFragment
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.adapters.GeofenceEventAdapter
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import kotlinx.android.synthetic.main.fragment_geofence_events.*
import javax.inject.Inject

/**
 * Created by xiaomei on 22/9/17.
 */
class GeofenceEventsFragment : BaseFragment() {

    companion object {
        val EXTRA_GEOFENCE_ID = "extra_geofence_id"

        fun newInstance(geofenceId: String): Fragment {
            val fragment = GeofenceEventsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_GEOFENCE_ID, geofenceId)
            fragment.arguments = bundle
            return fragment
        }
    }

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .appModule(AppModule(this.context))
                .build()
    }

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    var mGeofenceId: String? = null
    private var mGeofenceEventsLayoutManager: LinearLayoutManager? = null
    private var mGeofenceEventsAdapter: GeofenceEventAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        component.inject(this)
        mGeofenceId = arguments.getString(EXTRA_GEOFENCE_ID)
        if (mGeofenceId == null) {
            (activity as BaseActivity).popFragment()
            return
        }

        mGeofenceEventsLayoutManager = LinearLayoutManager(context)
        mGeofenceEventsAdapter = GeofenceEventAdapter(context)

        recyclerView.layoutManager = mGeofenceEventsLayoutManager
        recyclerView.adapter = mGeofenceEventsAdapter

        mLocationPreferences.mGeofenceEventsMap?.let {
            mGeofenceEventsAdapter?.setItems(it[mGeofenceId!!])
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater.inflate(R.layout.fragment_geofence_events, container, false)
        return rootView
    }
}