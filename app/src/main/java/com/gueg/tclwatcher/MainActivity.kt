package com.gueg.tclwatcher

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.FrameLayout
import com.gueg.tclwatcher.LoadingFragment.LoadingText.INSERT_DB
import com.gueg.tclwatcher.LoadingFragment.LoadingText.LOAD_ONLINE




class MainActivity : AppCompatActivity(), HomepageFragment.HomepageListener {

    private lateinit var container: FrameLayout
    private var loadingFragment = LoadingFragment()
    private var homepageFramgent = HomepageFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        container.id = View.generateViewId()
        setContentView(container)

        homepageFramgent.setHomepageListener(this)

        supportFragmentManager.beginTransaction().add(container.id, loadingFragment).commit()

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

    override fun onRequestEmitted(from: String, to: String) {
        val request = Request(from, to)
        RouteParser.parseRoute(request, object: RouteParser.RouteParserListener {
            override fun onRouteParsed(route: Route) {
                runOnUiThread {
                    val routesFragment = RoutesFragment()
                    routesFragment.route = route
                    setFragment(routesFragment)
                }
            }
        })
    }

    private fun setFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(container.id, fragment).commit()
    }


}
