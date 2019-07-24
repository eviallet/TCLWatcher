package com.gueg.tclwatcher


import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso


class SubRouteAdapter internal constructor(route:Route) :
    RecyclerView.Adapter<SubRouteAdapter.ViewHolder>() {

    private val subroutes = route.get()

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var departAt: TextView = v.findViewById(R.id.row_subroute_departat)
        var arriveAt: TextView = v.findViewById(R.id.row_subroute_arriveat)
        var duration: TextView = v.findViewById(R.id.row_subroute_duration)
        var from: TextView = v.findViewById(R.id.row_subroute_from)
        var fromDir: TextView = v.findViewById(R.id.row_subroute_fromdir)
        var to: TextView = v.findViewById(R.id.row_subroute_to)
        var picLeft: ImageView = v.findViewById(R.id.row_subroute_picleft)
        var picRight: ImageView = v.findViewById(R.id.row_subroute_picright)
        var pic: ImageView = v.findViewById(R.id.row_subroute_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_subroute, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subroute = subroutes[position]

        if(subroute is Route.TCL) {
            holder.departAt.text = subroute.departAt
            holder.arriveAt.text = subroute.arriveAt
            holder.duration.text = subroute.duration
            holder.from.text = subroute.from
            holder.fromDir.text = subroute.fromDir
            holder.to.text = subroute.to
            Picasso.get()
                .load(subroute.picLeft)
                .into(holder.picLeft)
            Picasso.get()
                .load(subroute.picRight)
                .into(holder.picRight)
        }
        else if(subroute is Route.Walk) {
            holder.departAt.text = "  "
            holder.arriveAt.visibility = GONE
            holder.from.visibility = GONE
            holder.to.visibility = GONE
            holder.picLeft.visibility = GONE
            holder.picRight.visibility = GONE
            holder.pic.visibility = VISIBLE
            holder.duration.text = subroute.duration
            holder.fromDir.text = subroute.additionalInfos.replace(" + ","\n")
            holder.fromDir.textSize = 14f
            holder.pic.setImageResource(R.drawable.walk)
        }
    }

    override fun getItemCount(): Int {
        return subroutes.size
    }

}