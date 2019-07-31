package com.gueg.tclwatcher

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView


class RouteFragment : Fragment() {

    companion object {
        fun from(route: Route): RouteFragment {
            val fragment = RouteFragment()
            fragment.route = route
            return fragment
        }
    }

    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView

    private lateinit var departAt: TextView
    private lateinit var arriveAt: TextView
    private lateinit var duration: TextView
    private lateinit var date: TextView

    private lateinit var adapter: SubRouteAdapter
    lateinit var route:Route

    lateinit var routeFragmentListener: RouteFragmentListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_route, container, false)

        recyclerView = rootView.findViewById(R.id.fragment_route_recyclerview)
        departAt = rootView.findViewById(R.id.fragment_route_departat)
        departAt.text = route.departureTime
        arriveAt = rootView.findViewById(R.id.fragment_route_arriveat)
        arriveAt.text = route.arrivalTime
        duration = rootView.findViewById(R.id.fragment_route_duration)
        duration.text = route.totalDuration
        date = rootView.findViewById(R.id.fragment_route_date)
        @Suppress("LocalVariableName") val mon_day = route.date.replace("|", "/").split("/").subList(1, 3)
        @Suppress("LocalVariableName") val day_mon = addLeadingZero(mon_day[1]) + "/" + addLeadingZero(mon_day[0])
        date.text = day_mon

        rootView.findViewById<ImageButton>(R.id.fragment_route_share).setOnClickListener { routeFragmentListener.onShare(route) }
        rootView.findViewById<ImageButton>(R.id.fragment_route_map).setOnClickListener { routeFragmentListener.onRouteMap(route) }
        rootView.findViewById<ImageButton>(R.id.fragment_route_bookmark).setOnClickListener { routeFragmentListener.onBookmark(route) }

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SubRouteAdapter(route, routeFragmentListener)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(VerticalItemDecoration(30))

        recyclerView.overScrollMode = OVER_SCROLL_NEVER

        return rootView
    }

    private fun addLeadingZero(date: String): String {
        return if(date.toInt() < 10) "0$date"
        else date
    }

    fun with(routeFragmentListener: RouteFragmentListener): RouteFragment {
        this.routeFragmentListener = routeFragmentListener
        return this
    }

    interface RouteFragmentListener {
        fun onStationMap(nameFrom: String, nameTo: String)
        fun onRouteMap(route: Route)
        fun onBookmark(route: Route)
        fun onShare(route: Route)
    }

}