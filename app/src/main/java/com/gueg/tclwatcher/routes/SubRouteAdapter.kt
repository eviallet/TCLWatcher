package com.gueg.tclwatcher.routes


import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import com.gueg.tclwatcher.ImageLoader
import com.gueg.tclwatcher.R


class SubRouteAdapter internal constructor(private val activity: Activity, route: Route, private val routeFragmentListener: RouteFragment.RouteFragmentListener) :
    RecyclerView.Adapter<SubRouteAdapter.ViewHolder>() {

    private val subroutes = route.get()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var container: View = v.findViewById(R.id.row_subroute_container)
        var departAt: TextView = v.findViewById(R.id.row_subroute_departat)
        var arriveAt: TextView = v.findViewById(R.id.row_subroute_arriveat)
        var duration: TextView = v.findViewById(R.id.row_subroute_duration)
        var from: TextView = v.findViewById(R.id.row_subroute_from)
        var fromDir: TextView = v.findViewById(R.id.row_subroute_fromdir)
        var to: TextView = v.findViewById(R.id.row_subroute_to)
        var web: WebView = v.findViewById(R.id.row_subroute_svg)
        var pic: ImageView = v.findViewById(R.id.row_subroute_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_subroute, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val subroute = subroutes[position]) {
            is Route.TCL -> {
                holder.container.setOnClickListener { routeFragmentListener.onStationMap(subroute.from, subroute.to) }
                holder.departAt.text = subroute.departAt
                holder.arriveAt.text = subroute.arriveAt
                holder.duration.text = subroute.duration
                holder.from.text = subroute.from
                holder.fromDir.text = subroute.fromDir
                holder.to.text = subroute.to
                holder.web.visibility = VISIBLE
                holder.pic.visibility = GONE
                ImageLoader.load(activity, subroute.pic, holder.web)
            }
            is Route.Walk -> {
                holder.departAt.text = "  "
                holder.arriveAt.visibility = GONE
                holder.from.visibility = GONE
                holder.to.visibility = GONE
                holder.duration.text = subroute.duration
                holder.fromDir.text = "Marcher ${subroute.duration}"
                holder.fromDir.textSize = 14f
                holder.web.visibility = GONE
                holder.pic.visibility = VISIBLE
                holder.pic.setImageResource(R.drawable.walk)
            }
            is Route.Wait -> {
                holder.departAt.text = "  "
                holder.arriveAt.visibility = GONE
                holder.from.visibility = GONE
                holder.to.visibility = GONE
                holder.duration.text = subroute.duration
                holder.fromDir.text = "Attendre ${subroute.duration}"
                holder.fromDir.textSize = 14f
                holder.web.visibility = GONE
                holder.pic.visibility = VISIBLE
                holder.pic.setImageResource(R.drawable.ic_clock)
            }
        }
    }

    override fun getItemCount(): Int {
        return subroutes.size
    }


}