package com.gueg.tclwatcher

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.Explode
import android.view.View
import android.widget.FrameLayout
import com.gueg.tclwatcher.LoadingFragment.LoadingText.INSERT_DB
import com.gueg.tclwatcher.LoadingFragment.LoadingText.LOAD_ONLINE


class MainActivity : AppCompatActivity(), StationPicker.StationPickerListener {

    private lateinit var container: FrameLayout
    private var loadingFragment = LoadingFragment()
    private var homepageFramgent = HomepageFragment()
    private var currentFragment: Fragment ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this)
        container.id = View.generateViewId()
        setContentView(container)

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
        RouteParser.parseRoute(request, object : RouteParser.RouteParserListener {
            override fun onRouteParsed(route: Route) {
                runOnUiThread {
                    val routesFragment = RoutesFragment()
                    routesFragment.route = route
                    setFragment(routesFragment)
                }
            }
        }, uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                if(throwable is RouteParser.ParseError)
                    homepageFramgent.setError(throwable.message.toString().split(".")[0].plus("."))
                else
                    homepageFramgent.setError("Erreur inconnue")
            }
        })
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


}
