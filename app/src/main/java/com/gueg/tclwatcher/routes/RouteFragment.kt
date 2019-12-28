package com.gueg.tclwatcher.routes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.VerticalDividerItemDecoration
import org.osmdroid.util.GeoPoint
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
        date.text = route.date

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
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SubRouteAdapter(activity!!, route, routeFragmentListener)
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
        fun onStationMap(coords: ArrayList<GeoPoint>, color: Int, tcl: String, from: String, to:String)
        fun onRouteMap(route: Route)
        fun onShare(request: String)
    }

}