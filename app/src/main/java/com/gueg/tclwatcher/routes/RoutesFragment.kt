package com.gueg.tclwatcher.routes

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.gueg.tclwatcher.LoadingView
import com.gueg.tclwatcher.MainActivity
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.stations.StationPicker

class RoutesFragment: Fragment() {

    companion object {
        const val PREV = -1
        const val NEXT = 1
    }

    private lateinit var rootView: View

    private lateinit var viewPager: ViewPager
    private lateinit var pagerAdapter: RoutesPagerAdapter
    lateinit var route: Route
    lateinit var leftLoadingView: LoadingView
    lateinit var rightLoadingView: LoadingView
    lateinit var routeFragmentListener: RouteFragment.RouteFragmentListener

    lateinit var stationPicker: StationPicker
    var tempStationPickerData: StationPicker?= null
        set(value) {
            if(::stationPicker.isInitialized)
                stationPicker.initFrom(value!!)
            field = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_routes, container, false)

        leftLoadingView = rootView.findViewById(R.id.fragment_routes_leftloading)
        rightLoadingView = rootView.findViewById(R.id.fragment_routes_rightloading)

        viewPager = rootView.findViewById(R.id.fragment_routes_viewpager)
        pagerAdapter = RoutesPagerAdapter(activity!!, activity!!.supportFragmentManager, route)
        viewPager.adapter = pagerAdapter

        stationPicker = rootView.findViewById(R.id.fragment_routes_stationpicker)
        if(tempStationPickerData != null)
            stationPicker.initFrom(tempStationPickerData!!)

        stationPicker.showTitle(false)

        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> {
                        pagerAdapter.loadPrevOrNext(PREV, index = position)
                        leftLoadingView.loading = true
                    }
                    pagerAdapter.count - 1 -> {
                        pagerAdapter.loadPrevOrNext(NEXT, index = position)
                        rightLoadingView.loading = true
                    }
                    else -> {
                        leftLoadingView.loading = false
                        rightLoadingView.loading = false
                    }
                }
            }
        })


        return rootView
    }

    fun onError() {
        stationPicker.setLoading(false)
    }

    inner class RoutesPagerAdapter(private val activity: Activity, fm: FragmentManager, route: Route) : FragmentStatePagerAdapter(fm) {

        private val routes = ArrayList<Route>()
        private val handler = Handler()
        private var hasBeenAnimated = false

        init {
            add(route=route)

            loadPrevOrNext(PREV, route)
            loadPrevOrNext(NEXT, route)
        }

        private fun add(pos: Int = -1, route: Route, index: Int = 0) {
            handler.post {
                if(!routes.contains(route)) {
                    val viewPagerPos = if(index!=0) index else viewPager.currentItem
                    when (pos) {
                        PREV -> routes.add(viewPagerPos, route)
                        else -> routes.add(viewPagerPos + 1, route)
                    }
                    activity.runOnUiThread {
                        if (pos == PREV) {
                            viewPager.adapter = this
                            viewPager.currentItem = viewPagerPos + 1
                        } else
                            notifyDataSetChanged()
                        when (pos) {
                            PREV -> leftLoadingView.loading = false
                            else -> rightLoadingView.loading = false
                        }
                    }
                }
            }
        }

        fun loadPrevOrNext(prevOrNext: Int, route: Route = routes[viewPager.currentItem], index: Int = 0) {
            val request =
                if(prevOrNext == PREV)
                    RouteRequest(route.from, route.to, timeMode= RouteRequest.TimeMode.ARRIVE_AT ,datetime= route.prev)
                else
                    RouteRequest(route.from, route.to, datetime= route.next)
            RouteParser.parseRoute(context!!, request,
                object : RouteParser.RouteParserListener {
                    override fun onRouteParsed(route: Route) {
                        add(prevOrNext, route, index)
                    }
                }
            ,uncaughtExceptionHandler = RouteParserExceptionHandler(activity as MainActivity))
        }

        override fun getCount() = routes.size
        override fun getItem(p0: Int) = RouteFragment
            .from(routes[p0])
            .with(routeFragmentListener)
            .shouldAnimate(routes[p0] == route && !hasBeenAnimated)
            { hasBeenAnimated = true }
    }

}