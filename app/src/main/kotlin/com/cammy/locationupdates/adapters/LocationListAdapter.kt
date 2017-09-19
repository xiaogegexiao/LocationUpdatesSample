package com.cammy.locationupdates.adapters

import android.content.Context
import android.location.Location
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.locationupdates.R
import kotlinx.android.synthetic.main.list_item_location.view.*
import kotlinx.android.synthetic.main.list_item_location_header.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * Created by xiaomei on 6/9/17.
 */
class LocationListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {
    var mItems: MutableList<Location> = ArrayList()
    var mItemsList: MutableMap<Long, MutableList<Location>> = LinkedHashMap()
    var mContext: Context? = null

    constructor(context: Context?) : super() {
        this.mContext = context
    }

    fun setItems(items: List<Location>?) {
        mItems.clear()
        items?.let {
            mItems.addAll(it)
        }
        groupItemsByDate()
        notifyDataSetChanged()
    }

    fun groupItemsByDate() {
        Collections.sort(mItems) { x, y -> (y.time - x.time).toInt() }
        if (mItems.isNotEmpty()) {
            mItemsList.clear()
            var location = mItems[0]
            var cal = Calendar.getInstance()
            cal.timeInMillis = location.time
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            var sectionDay = cal.timeInMillis
            var secionList: MutableList<Location> = ArrayList()
            for (loc in mItems) {
                cal.timeInMillis = loc.time
                cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                var locDay = cal.timeInMillis
                if (locDay != sectionDay) {
                    mItemsList.put(sectionDay, secionList)
                    sectionDay = locDay
                    secionList = ArrayList()
                }
                secionList.add(loc)
            }
            mItemsList.put(sectionDay, secionList)
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        for (mapEntry in mItemsList) {
            count += mapEntry.value.size + 1
        }
        return count
    }

    fun getLocation(position: Int): Location? {
        var index = position
        for (mapEntry in mItemsList) {
            if (index == 0) {
                return null
            }
            index -= 1
            if (index < mapEntry.value.size) {
                return mapEntry.value[index]
            }
            index -= mapEntry.value.size
        }
        return null
    }

    fun getSection(position: Int): Long {
        var index = position
        for (mapEntry in mItemsList) {
            if (index < mapEntry.value.size + 1) {
                return mapEntry.key
            }
            index -= mapEntry.value.size + 1
        }
        return 0L
    }

    override fun getItemViewType(position: Int): Int {
        var index = position
        for (mapEntry in mItemsList) {
            if (index == 0) {
                return TYPE_SECTION
            }
            index --
            if (index < mapEntry.value.size) {
                return TYPE_LOCATION
            }
            index -= mapEntry.value.size
        }
        return super.getItemViewType(position)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        var itemViewType = getItemViewType(position)
        when (itemViewType) {
            TYPE_LOCATION -> {
                getLocation(position)?.let {
                    (holder as LocationViewHolder).itemView.location_latitude.text = it.latitude.toString()
                    holder.itemView.location_longitude.text = it.longitude.toString()
                    holder.itemView.location_accuracy.text = it.accuracy.toString()
                    holder.itemView.time.text = Date(it.time).toString()
                }
            }
            else -> {
                getSection(position).let {
                    (holder as LocationViewHolder).itemView.date.text = Date(it).toString()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LOCATION -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_location, parent, false)
                LocationViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_location_header, parent, false)
                LocationViewHolder(view)
            }
        }
    }


    class  LocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        var TYPE_LOCATION = 0
        var TYPE_SECTION = 1
    }
}