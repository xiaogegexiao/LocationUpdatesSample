package com.cammy.locationupdates.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.adapters.GeofenceEventAdapter
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import kotlinx.android.synthetic.main.activity_geofence_events.*
import javax.inject.Inject

class GeofenceEventsActivity : AppCompatActivity() {

    val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .build()
    }

    companion object {
        val EXTRA_GEOFENCE_ID = "extra_geofence_id"
    }

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    var mGeofenceId: String? = null
    private var mGeofenceEventsLayoutManager: LinearLayoutManager? = null
    private var mGeofenceEventsAdapter: GeofenceEventAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGeofenceId = intent.getStringExtra(EXTRA_GEOFENCE_ID)
        if (mGeofenceId == null) {
            finish()
            return
        }
        component.inject(this)
        setContentView(R.layout.activity_geofence_events)

        mGeofenceEventsLayoutManager = LinearLayoutManager(this)
        mGeofenceEventsAdapter = GeofenceEventAdapter(this)

        recyclerView.layoutManager = mGeofenceEventsLayoutManager
        recyclerView.adapter = mGeofenceEventsAdapter

        mLocationPreferences.mGeofenceEventsMap?.let {
            mGeofenceEventsAdapter?.setItems(it[mGeofenceId!!])
        }
    }
}