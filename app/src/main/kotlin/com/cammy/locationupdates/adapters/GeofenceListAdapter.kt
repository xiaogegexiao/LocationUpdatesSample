package com.cammy.locationupdates.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.locationupdates.GeofenceModel
import com.cammy.locationupdates.R
import kotlinx.android.synthetic.main.list_item_geofence.view.*

/**
 * Created by xiaomei on 6/9/17.
 */
class GeofenceListAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder> {
    var mItems: MutableList<GeofenceModel> = ArrayList()
    var mContext: Context? = null

    constructor(context: Context?) : super() {
        this.mContext = context
    }

    fun setItems(items: List<GeofenceModel>?) {
        mItems.clear()
        items?.let {
            mItems.addAll(it)
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        mItems[position]?.let {
            (holder as GeofenceViewHolder).itemView.geofence_name.text = it.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        var view = LayoutInflater.from(mContext).inflate(R.layout.list_item_geofence, parent, false)
        return GeofenceViewHolder(view)
    }


    class  GeofenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}