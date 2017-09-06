package com.cammy.locationupdates.adapters

import android.content.Context
import android.location.Location
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.locationupdates.R
import kotlinx.android.synthetic.main.list_item_location.view.*
import java.util.*

/**
 * Created by xiaomei on 6/9/17.
 */
class LocationListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {
    var mItems: MutableList<Location> = ArrayList()
    var mContext: Context? = null

    constructor(context: Context?) : super() {
        this.mContext = context
    }

    fun setItems(items: List<Location>?) {
        mItems.clear()
        items?.let {
            mItems.addAll(it)
        }
        Collections.sort(mItems) { x, y -> (y.time - x.time).toInt() }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        mItems[position]?.let {
            (holder as LocationViewHolder).itemView.location_latitude.text = it.latitude.toString()
            holder.itemView.location_longitude.text = it.longitude.toString()
            holder.itemView.location_accuracy.text = it.accuracy.toString()
            holder.itemView.time.text = Date(it.time).toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        var view = LayoutInflater.from(mContext).inflate(R.layout.list_item_location, parent, false)
        return LocationViewHolder(view)
    }


    class  LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}