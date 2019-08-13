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
    private var homepageFragment = HomepageFragment()
    private var currentFragment: Fragment ?= null

    private var pendingRequest: String ?= null

    // ======= LISTENERS =======

    private val routeFragmentListener = object: RouteFragment.RouteFragmentListener {
        override fun onStationMap(nameFrom: String, nameTo: String) {
            val intent = Intent(applicationContext, MapActivity::class.java)
            intent.putExtra(MapActivity.EXTRA_STATION_FROM, nameFrom)
            intent.putExtra(MapActivity.EXTRA_STATION_TO, nameTo)
            startActivity(intent)
        }
        override fun onRouteMap(route: Route) {
            val intent = Intent(applicationContext, MapActivity::class.java)
            val stringArrayList = ArrayList<String>()
            for (subroute in route.get()) {
                if (subroute is Route.TCL) {
                    stringArrayList.add(subroute.from)
                    stringArrayList.add(subroute.to)
                }
            }
            startActivity(intent.putExtra(MapActivity.EXTRA_PATH, stringArrayList))
        }
        override fun onShare(request: String) {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, "Partager")
            i.putExtra(Intent.EXTRA_TEXT, request)
            startActivity(Intent.createChooser(i, "Partager avec"))
        }
    }

    private val routeParserListener = object: RouteParser.RouteParserListener {
        override fun onRouteParsed(route: Route) {
            val routesFragment = RoutesFragment()
            routesFragment.route = route
            routesFragment.routeFragmentListener = routeFragmentListener
            setFragment(routesFragment)
        }
    }

    // ======= ACTIVITY =======

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.activity_main_container)
        errorLayout = findViewById(R.id.activity_main_error_layout)
        errorText = findViewById(R.id.activity_main_error_text)

        homepageFragment.setStationPickerListener(this)

        setFragment(loadingFragment)

        loadStations()

        if (intent.data != null) {
            val intentUrl = intent.data!!.toString()
            if (intentUrl.contains("/navitia/itineraire_mobile?") || intentUrl.contains("/navitia/itineraire_mobile?"))
                pendingRequest = intentUrl
        }
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
                homepageFragment.setStations(stations)
                setFragment(homepageFragment)

                if(pendingRequest != null)
                    onExternalUrlOpened(pendingRequest!!)
            }
        }).start()
    }

    override fun onRequestEmitted(request: Request) {
        RouteParser.cancel()
        RouteParser.parseRoute(request, routeParserListener, uncaughtExceptionHandler = RouteParserExceptionHandler(this, request))
    }

    private fun onExternalUrlOpened(url: String) {
        val request = Request("","")
        RouteParser.cancel()
        RouteParser.parseRoute(request, object: RouteParser.RouteParserListener {
            override fun onRouteParsed(route: Route) {
                try {
                    runOnUiThread {
                        if(currentFragment is HomepageFragment)
                            (currentFragment as HomepageFragment).stationPicker.fillNow(route.get()[0].from, route.get()[route.get().lastIndex].to)
                        else {
                            homepageFragment.stationPicker.fillNow(route.get()[0].from, route.get()[route.get().lastIndex].to)
                            (currentFragment as RoutesFragment).tempStationPickerData = homepageFragment.stationPicker
                        }
                    }
                } catch(err: UninitializedPropertyAccessException) {
                    (currentFragment as HomepageFragment).tempFrom = route.get()[0].from
                    (currentFragment as HomepageFragment).tempTo = route.get()[route.get().lastIndex].to
                }
                routeParserListener.onRouteParsed(route)
            }
        }, uncaughtExceptionHandler = RouteParserExceptionHandler(this, request), url = url)
    }

    private fun setFragment(fragment: Fragment) {
        if((currentFragment is RoutesFragment || currentFragment is HomepageFragment) && (fragment is RoutesFragment || fragment is HomepageFragment)) {
            if(!(fragment is RoutesFragment && currentFragment is RoutesFragment)) {
                fragment.enterTransition = Explode(this, null)
                fragment.sharedElementEnterTransition = AutoTransition()
                currentFragment!!.exitTransition = Explode(this, null)
            }

            if(fragment is HomepageFragment)
                fragment.tempStationPickerData = currentFragment!!.view!!.findViewWithTag("transition_picker")
            else if(fragment is RoutesFragment)
                fragment.tempStationPickerData = currentFragment!!.view!!.findViewWithTag("transition_picker")

            supportFragmentManager.beginTransaction()
                .addSharedElement(currentFragment!!.view!!.findViewWithTag("transition_picker"), "transition_picker")
                .replace(container.id, fragment).commit()
        } else
            supportFragmentManager.beginTransaction().replace(container.id, fragment).commit()

        currentFragment = fragment
    }

    override fun onBackPressed() {
        if(currentFragment is RoutesFragment)
            setFragment(homepageFragment)
        else
            super.onBackPressed()
    }


    fun setError(text: String) {
        runOnUiThread {
            errorText.text = text
            if (!errorShown) {
                errorShown = true
                errorLayout.animate().translationYBy(-errorLayout.height.toFloat() + 2)
                Handler().postDelayed({
                    errorLayout.animate().translationY(0f).withEndAction { errorShown = false }
                }, 3000)
            }

            if(currentFragment is HomepageFragment)
                homepageFragment.onError()
            else if(currentFragment is RoutesFragment)
                (currentFragment as RoutesFragment).onError()
        }
    }

}
