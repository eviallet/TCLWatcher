package com.gueg.tclwatcher

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

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

    lateinit var stationPicker: StationPicker
    var tempStationPickerData: StationPicker ?= null
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

        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) = when (position) {
                0 -> {
                    pagerAdapter.loadPrevOrNext(PREV)
                    leftLoadingView.loading = true
                }
                pagerAdapter.count-1 -> {
                    pagerAdapter.loadPrevOrNext(NEXT)
                    rightLoadingView.loading = true
                }
                else -> {
                    leftLoadingView.loading = false
                    rightLoadingView.loading = false
                }
            }
        })

        return rootView
    }

    inner class RoutesPagerAdapter(private val activity: Activity, fm: FragmentManager, route: Route) : FragmentStatePagerAdapter(fm) {

        private val frags = ArrayList<Route>()
        private val handler = Handler()

        private fun add(pos: Int = -1, route: Route) {
            handler.post {
                if(pos==-1)
                    frags.add(route)
                else
                    frags.add(pos, route)
                activity.runOnUiThread {
                    notifyDataSetChanged()
                    if(pos==-1)
                        rightLoadingView.loading = false
                    else
                        leftLoadingView.loading = false
                }
            }
        }

        init {
            add(route=route)

            loadPrevOrNext(PREV, route)
            loadPrevOrNext(NEXT, route)
        }

        fun loadPrevOrNext(prevOrNext: Int, route: Route = frags[viewPager.currentItem]) {
            val prev = prevOrNext == PREV
            val next = prevOrNext == NEXT
            if(!prev && !next) return
            RouteParser.parseRoute(Request(route.from, route.to), object: RouteParser.RouteParserListener {
                override fun onRouteParsed(route: Route) {
                    if(prev)
                        add(pos=viewPager.currentItem, route=route)
                    else
                        add(pos=viewPager.currentItem + 1, route=route)
                }
            }, url= if(prev) route.prev else route.next)
        }

        override fun getCount() = frags.size
        override fun getItem(p0: Int): Fragment {
            val routeFragment = RouteFragment()
            routeFragment.route = frags[p0]
            return routeFragment
        }
    }

}