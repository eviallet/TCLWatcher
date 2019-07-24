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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_routes, container, false)

        leftLoadingView = rootView.findViewById(R.id.fragment_routes_leftloading)
        rightLoadingView = rootView.findViewById(R.id.fragment_routes_rightloading)

        viewPager = rootView.findViewById(R.id.fragment_routes_viewpager)
        pagerAdapter = RoutesPagerAdapter(activity!!, activity!!.supportFragmentManager, route)
        viewPager.adapter = pagerAdapter

        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) = when (position) {
                0 -> {
                    pagerAdapter.loadPrevOrNext(PREV)
                    leftLoadingView.setLoading()
                }
                pagerAdapter.count-1 -> {
                    pagerAdapter.loadPrevOrNext(NEXT)
                    rightLoadingView.setLoading()
                }
                else -> pagerAdapter.updateLoadingViews()
            }
        })

        return rootView
    }

    inner class RoutesPagerAdapter(val activity: Activity, fm: FragmentManager, route: Route) : FragmentStatePagerAdapter(fm) {

        private val frags = ArrayList<Route>()
        private val handler = Handler()

        private fun add(pos: Int = -1, route: Route) {
            if(pos==-1)
                frags.add(route)
            else
                frags.add(pos, route)
            handler.post { activity.runOnUiThread { notifyDataSetChanged() } }
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

                    activity.runOnUiThread {
                        if(prev)
                            viewPager.currentItem = viewPager.currentItem + 1
                        updateLoadingViews()
                    }
                }
            }, url= if(prev) route.prev else route.next)
        }

        fun updateLoadingViews() {
            val pos = viewPager.currentItem
            if(pos-1>=0)
                leftLoadingView.setTimes(frags[pos+PREV].departureTime, frags[pos+PREV].arrivalTime)
            if(pos+1<frags.size)
                rightLoadingView.setTimes(frags[pos+NEXT].departureTime, frags[pos+NEXT].arrivalTime)
        }

        override fun getCount() = frags.size
        override fun getItem(p0: Int): Fragment {
            val routeFragment = RouteFragment()
            routeFragment.route = frags[p0]
            return routeFragment
        }
    }

}