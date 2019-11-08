package com.gueg.tclwatcher.routes


import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.gueg.tclwatcher.ImageLoader
import com.gueg.tclwatcher.ImageViewWithCache
import com.gueg.tclwatcher.R


class SubRouteAdapter internal constructor(private val activity: Activity, route: Route, private val routeFragmentListener: RouteFragment.RouteFragmentListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val subroutes = route.get()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var container: View = v.findViewById(R.id.row_subroute_container)
        var departAt: TextView = v.findViewById(R.id.row_subroute_departat)
        var arriveAt: TextView = v.findViewById(R.id.row_subroute_arriveat)
        var duration: TextView = v.findViewById(R.id.row_subroute_duration)
        var from: TextView = v.findViewById(R.id.row_subroute_from)
        var fromDir: TextView = v.findViewById(R.id.row_subroute_fromdir)
        var to: TextView = v.findViewById(R.id.row_subroute_to)
        var pic: ImageViewWithCache = v.findViewById(R.id.row_subroute_pic)
    }

    inner class SmallViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var duration: TextView = v.findViewById(R.id.row_subroute_small_duration)
        var infos: TextView = v.findViewById(R.id.row_subroute_small_infos)
        var pic: ImageView = v.findViewById(R.id.row_subroute_small_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when(viewType) {
        0 -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_subroute, parent, false))
        else -> SmallViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_subroute_small, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            // TCL item
            0 -> {
                val holder = holder as ViewHolder
                val subroute = subroutes[position]
                holder.container.setOnClickListener {
                    routeFragmentListener.onStationMap(subroute.coords!!, subroute.color, subroute.lineName(), subroute.from, subroute.to)
                }
                holder.departAt.text = subroute.departAt
                holder.arriveAt.text = subroute.arriveAt
                holder.duration.text = subroute.duration
                holder.from.text = subroute.from
                holder.fromDir.text = subroute.fromDir
                holder.to.text = subroute.to
                ImageLoader.load(activity, subroute.pic, holder.pic)
            }
            // Wait or Walk item = SmallViewHolder
            else -> {
                val holder = holder as SmallViewHolder
                when(val subroute = subroutes[position]) {
                    is Route.Walk -> {
                        holder.infos.text = "Marcher "
                        holder.duration.text = subroute.duration
                        holder.pic.setImageResource(R.drawable.walk)
                    }
                    is Route.Wait -> {
                        holder.infos.text = "Attendre "
                        holder.duration.text = subroute.duration
                        holder.pic.setImageResource(R.drawable.ic_clock)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(subroutes[position] is Route.TCL) 0 else 1
    }

    override fun getItemCount(): Int {
        return subroutes.size
    }


}