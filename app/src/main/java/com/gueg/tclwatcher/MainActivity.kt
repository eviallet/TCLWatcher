package com.gueg.tclwatcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.Explode
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.gueg.tclwatcher.LoadingFragment.LoadingText.INSERT_DB
import com.gueg.tclwatcher.LoadingFragment.LoadingText.LOAD_ONLINE
import com.gueg.tclwatcher.map.MapActivity
import com.gueg.tclwatcher.routes.*
import com.gueg.tclwatcher.stations.StationParser
import com.gueg.tclwatcher.stations.StationPicker


class MainActivity : AppCompatActivity(), StationPicker.StationPickerListener {

    private lateinit var container: FrameLayout

    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private var errorShown: Boolean = false

    private var loadingFragment = LoadingFragment()
    private var homepageFramgent = HomepageFragment()
    private var currentFragment: Fragment ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.activity_main_container)
        errorLayout = findViewById(R.id.activity_main_error_layout)
        errorText = findViewById(R.id.activity_main_error_text)

        homepageFramgent.setStationPickerListener(this)

        setFragment(loadingFragment)

        loadStations()
    }

    private fun loadStations() {
        Thread(Runnable {
            var stations = StationDatabase.getDatabase(applicationContext).stationDao().all
            if(stations.isEmpty()) {
                runOnUiThread { loadingFragment.updateText(LOAD_ONLINE) }
                stations = StationParser.parseStations()
                runOnUiThread { loadingFragment.updateText(INSERT_DB) }
                StationDatabase.getDatabase(applicationContext).stationDao().insertAll(stations)
            }
            runOnUiThread {
                homepageFramgent.setStations(stations)
                setFragment(homepageFramgent)
            }
        }).start()
    }

    override fun onRequestEmitted(request: Request) {
        RouteParser.cancel()
        RouteParser.parseRoute(request, object : RouteParser.RouteParserListener {
            override fun onRouteParsed(route: Route) {
                runOnUiThread {
                    val routesFragment = RoutesFragment()
                    routesFragment.route = route
                    routesFragment.routeFragmentListener = object: RouteFragment.RouteFragmentListener {
                        override fun onStationMap(nameFrom: String, nameTo: String) {
                            val intent = Intent(applicationContext, MapActivity::class.java)
                            intent.putExtra(MapActivity.EXTRA_STATION_FROM, nameFrom)
                            intent.putExtra(MapActivity.EXTRA_STATION_TO, nameTo)
                            startActivity(intent)
                        }
                        override fun onRouteMap(route: Route) {
                            Thread {
                                val intent = Intent(applicationContext, MapActivity::class.java)
                                val stringArrayList = ArrayList<String>()
                                for (subroute in route.get()) {
                                    if (subroute is Route.TCL) {
                                        stringArrayList.add(subroute.from)
                                        stringArrayList.add(subroute.to)
                                    }
                                }
                                startActivity(intent.putExtra(MapActivity.EXTRA_PATH, stringArrayList))
                            }.start()
                        }
                        override fun onBookmark(route: Route) {
                        }
                        override fun onShare(route: Route) {
                        }
                    }
                    setFragment(routesFragment)
                }
            }
        }, uncaughtExceptionHandler = RouteParserExceptionHandler(this, request))
    }

    private fun setFragment(fragment: Fragment) {
        if((currentFragment is RoutesFragment || currentFragment is HomepageFragment) && (fragment is RoutesFragment || fragment is HomepageFragment)) {
            fragment.enterTransition = Explode(this, null)
            fragment.sharedElementEnterTransition = AutoTransition()
            currentFragment!!.exitTransition = Explode(this, null)

            if(fragment is HomepageFragment)
                fragment.tempStationPickerData = currentFragment!!.view!!.findViewWithTag<StationPicker>("transition_picker")
            else if(fragment is RoutesFragment)
                fragment.tempStationPickerData = currentFragment!!.view!!.findViewWithTag<StationPicker>("transition_picker")

            supportFragmentManager.beginTransaction()
                .addSharedElement(currentFragment!!.view!!.findViewWithTag("transition_picker"), "transition_picker")
                .replace(container.id, fragment).commit()
        } else
            supportFragmentManager.beginTransaction().replace(container.id, fragment).commit()

        currentFragment = fragment
    }

    override fun onBackPressed() {
        if(currentFragment is RoutesFragment)
            setFragment(homepageFramgent)
        else
            super.onBackPressed()
    }


    fun setError(text: String) {
        errorText.text = text
        if(!errorShown) {
            errorShown = true
            errorLayout.animate().translationYBy(-errorLayout.height.toFloat()+2)
            Handler().postDelayed({
                errorLayout.animate().translationY(0f).withEndAction { errorShown = false }
            }, 3000)
        }
    }

}
