package com.gueg.tclwatcher.routes

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.VerticalDividerItemDecoration
import kotlin.math.hypot


class RouteFragment : Fragment() {

    companion object {
        fun from(route: Route): RouteFragment {
            val fragment = RouteFragment()
            fragment.route = route
            return fragment
        }
    }

    private lateinit var rootView: View
    private var rootViewFirstShown = true
    private var shoudAnimateReveal = false

    private lateinit var recyclerView: RecyclerView

    private lateinit var departAt: TextView
    private lateinit var arriveAt: TextView
    private lateinit var duration: TextView
    private lateinit var date: TextView

    private lateinit var indicator: ImageView
    private lateinit var warning: ImageButton

    private lateinit var adapter: SubRouteAdapter
    lateinit var route: Route

    lateinit var onAnimated: () -> Unit

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

        indicator = rootView.findViewById(R.id.fragment_route_recyclerview_indicator)

        rootView.findViewById<ImageButton>(R.id.fragment_route_share).setOnClickListener { routeFragmentListener.onShare(route.request) }
        rootView.findViewById<ImageButton>(R.id.fragment_route_map).setOnClickListener { routeFragmentListener.onRouteMap(route) }

        warning = rootView.findViewById(R.id.fragment_route_warning)
        if(route.warning.isNotEmpty()) {
            warning.visibility = VISIBLE
            warning.setOnClickListener {
                val dialog = AlertDialog.Builder(context!!)
                    .setView(R.layout.dialog_warning)
                    .create()
                dialog.show()
                dialog.findViewById<TextView>(R.id.dialog_warning_text)!!.text = route.warning
            }
        }

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)!!
        adapter = SubRouteAdapter(route, routeFragmentListener)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(VerticalDividerItemDecoration(context!!, 15))

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val view = recyclerView.getChildAt(recyclerView.childCount-1)
                if(view != null) {
                    if (!layoutManager.isViewPartiallyVisible(view, false, false))
                        indicator.animate().alpha(0f).scaleX(0.3f).scaleY(0.3f).withEndAction {
                            indicator.visibility = GONE
                        }
                    else if (indicator.visibility != VISIBLE) {
                        indicator.visibility = VISIBLE
                        indicator.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    }
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        indicator.setOnClickListener { recyclerView.smoothScrollToPosition(recyclerView.childCount - 1) }

        recyclerView.overScrollMode = OVER_SCROLL_NEVER

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            if(rootView.isAttachedToWindow && rootViewFirstShown && shoudAnimateReveal) {
                rootViewFirstShown = false
                val cx = rootView.width / 2
                val cy = rootView.height / 2
                val finalRadius = hypot(cx.toDouble(), cy.toDouble()).toFloat()
                ViewAnimationUtils.createCircularReveal(rootView, cx, cy, 0f, finalRadius).setDuration(500).start()
                onAnimated()
            }
        }

        return rootView
    }

    private fun addLeadingZero(date: String) = if(date.toInt() < 10) "0$date" else date

    fun with(routeFragmentListener: RouteFragmentListener): RouteFragment {
        this.routeFragmentListener = routeFragmentListener
        return this
    }

    fun shouldAnimate(bool: Boolean, onAnimated: () -> Unit): RouteFragment {
        shoudAnimateReveal = bool
        this.onAnimated = onAnimated
        return this
    }

    interface RouteFragmentListener {
        fun onStationMap(nameFrom: String, nameTo: String)
        fun onRouteMap(route: Route)
        fun onShare(request: String)
    }

}