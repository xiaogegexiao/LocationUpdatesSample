package com.cammy.locationupdates.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.adapters.LocationListAdapter
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import kotlinx.android.synthetic.main.activity_location_list.*
import javax.inject.Inject

class LocationListActivity : AppCompatActivity() {

    val component: AppComponent by lazy {
        DaggerAppComponent
                .builder()
                .appModule(AppModule(this))
                .build()
    }

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mLocationListAdapter: LocationListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_location_list)

        mLinearLayoutManager = LinearLayoutManager(this)
        mLocationListAdapter = LocationListAdapter(this)

        recyclerView.layoutManager = mLinearLayoutManager
        recyclerView.adapter = mLocationListAdapter

        mLocationListAdapter?.setItems(mLocationPreferences.mLocationUpdateList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_location_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_show_on_map -> {
                val intent = Intent(this, MapLocationActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }
}