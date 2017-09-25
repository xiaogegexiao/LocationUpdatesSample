package com.cammy.locationupdates.fragments

import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.cammy.cammyui.activities.BaseActivity
import com.cammy.cammyui.fragments.BaseFragment
import com.cammy.locationupdates.LocationPreferences
import com.cammy.locationupdates.R
import com.cammy.locationupdates.adapters.LocationListAdapter
import com.cammy.locationupdates.dagger.AppComponent
import com.cammy.locationupdates.dagger.AppModule
import com.cammy.locationupdates.dagger.DaggerAppComponent
import kotlinx.android.synthetic.main.fragment_location_list.*
import javax.inject.Inject

/**
 * Created by xiaomei on 22/9/17.
 */
class LocationListFragment : BaseFragment() {

    companion object {
        val TAG = LocationListFragment::class.simpleName
        fun newInstance(): Fragment {
            val fragment = LocationListFragment()
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

    @Inject
    lateinit var mLocationPreferences: LocationPreferences

    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mLocationListAdapter: LocationListAdapter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        component.inject(this)
        mLinearLayoutManager = LinearLayoutManager(context)
        mLocationListAdapter = LocationListAdapter(context)

        recyclerView.layoutManager = mLinearLayoutManager
        recyclerView.adapter = mLocationListAdapter

        mLocationListAdapter?.setItems(mLocationPreferences.mLocationUpdateList)
        mLocationListAdapter?.mOnClickListener = View.OnClickListener { view ->
            run {
                var locations = view.tag as ArrayList<Location>

                val fragment = MapLocationFragment.newInstance(locations)
                (activity as BaseActivity).pushFragment(fragment, MapLocationFragment.TAG)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var rootView = inflater.inflate(R.layout.fragment_location_list, container, false)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_location_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }
}