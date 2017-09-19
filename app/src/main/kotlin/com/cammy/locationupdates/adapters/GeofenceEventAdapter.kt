package com.cammy.locationupdates.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cammy.locationupdates.R
import com.cammy.locationupdates.models.GeofenceEvent
import com.google.android.gms.location.Geofence
import kotlinx.android.synthetic.main.list_item_geofence_event.view.*
import kotlinx.android.synthetic.main.list_item_geofence_event_header.view.*
import java.util.*
import java.util.Collections.sort

/**
 * Created by xiaomei on 6/9/17.
 */
class GeofenceEventAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder> {
    var mItems: MutableList<GeofenceEvent> = ArrayList()
    var mItemsList: MutableMap<Long, MutableList<GeofenceEvent>> = LinkedHashMap()
    var mContext: Context? = null

    constructor(context: Context?) : super() {
        this.mContext = context
    }

    fun setItems(items: List<GeofenceEvent>?) {
        mItems.clear()
        items?.let {
            mItems.addAll(it)
        }
        groupItemsByDate()
        notifyDataSetChanged()
    }

    fun groupItemsByDate() {
        sort(mItems, object : Comparator<GeofenceEvent> {
            override fun compare(p0: GeofenceEvent?, p1: GeofenceEvent?): Int {
                var location0 = p0?.let { it.triggerLocation }
                var location1 = p1?.let { it.triggerLocation }
                if (location0 == null) {
                    return 1
                }
                if (location1 == null) {
                    return -1
                }
                return (location1.time - location0.time).toInt()
            }
        })
        if (mItems.isNotEmpty()) {
            mItemsList.clear()
            var geofenceEvent = mItems[0]
            var cal = Calendar.getInstance()
            geofenceEvent.triggerLocation?.let {
                cal.timeInMillis = it.time
            }
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            var sectionDay = cal.timeInMillis
            var secionList: MutableList<GeofenceEvent> = ArrayList()
            for (ge in mItems) {
                ge.triggerLocation?.let {
                    cal.timeInMillis = it.time
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
                    secionList.add(ge)
                }
            }
            mItemsList.put(sectionDay, secionList)
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        for (mapEntry in mItemsList) {
            count += 1 + mapEntry.value.size
        }
        return count
    }

    override fun getItemViewType(position: Int): Int {
        var index = position
        for (mapEntry in mItemsList) {
            if (index == 0) {
                return TYPE_SECTION
            }
            index--
            if (index < mapEntry.value.size) {
                return TYPE_GEOFENCE_EVENT
            }
            index -= mapEntry.value.size
        }
        return super.getItemViewType(position)
    }

    fun getSection(position: Int): Long {
        var index = position
        for (mapEntry in mItemsList) {
            if (index < 1 + mapEntry.value.size) {
                return mapEntry.key
            }
            index -= 1 + mapEntry.value.size
        }
        return 0L
    }

    fun getGeofenceEvent(position: Int): GeofenceEvent? {
        var index = position
        for (mapEntry in mItemsList) {
            if (index == 0) {
                return null
            }
            index--
            if (index < mapEntry.value.size) {
                return mapEntry.value[index]
            }
            index -= mapEntry.value.size
        }
        return null
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        var itemViewType = getItemViewType(position)
        when (itemViewType) {
            TYPE_GEOFENCE_EVENT -> {
                var geofenceEvent = getGeofenceEvent(position)
                if (geofenceEvent != null) {
                    (holder as GeofenceEventViewHolder).itemView.geofence_transition.text = if (geofenceEvent.geoTransition == Geofence.GEOFENCE_TRANSITION_ENTER) "Enter" else "Exit"
                    geofenceEvent.triggerLocation?.let {
                        holder.itemView.location_latitude.text = it.latitude.toString()
                        holder.itemView.location_longitude.text = it.longitude.toString()
                        holder.itemView.location_accuracy.text = it.accuracy.toString()
                        holder.itemView.time.text = Date(it.time).toString()
                    }
                }
            }
            else -> {
                getSection(position).let {
                    (holder as GeofenceEventViewHolder).itemView.date.text = Date(it).toString()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_GEOFENCE_EVENT -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_geofence_event, parent, false)
                GeofenceEventViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.list_item_geofence_event_header, parent, false)
                GeofenceEventViewHolder(view)
            }
        }
    }


    class GeofenceEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        var TYPE_SECTION = 0
        var TYPE_GEOFENCE_EVENT = 1
    }
}