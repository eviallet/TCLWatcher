package com.gueg.tclwatcher

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.Explode
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.gueg.tclwatcher.LoadingFragment.LoadingText.INSERT_DB
import com.gueg.tclwatcher.LoadingFragment.LoadingText.LOAD_ONLINE
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException


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
                Log.d(":-:",throwable.javaClass.name)
                when (throwable) {
                    is RouteParser.ParseError -> homepageFramgent.setError(throwable.message.toString().split(".")[0].plus("."))
                    is RouteParser.StationConflictError -> { // if there was any conflict
                        // if "from" field had a conflict
                        if(throwable.choicesFrom.size > 1) {
                            // wait for user choice
                            showConflictDialog(true, request, throwable.choicesFrom, throwable.valuesFrom) {
                                // if there was also a conflict on "to" field
                                if(throwable.choicesTo.size > 1) {
                                    // wait for user choice again
                                    showConflictDialog(false, request, throwable.choicesTo, throwable.valuesTo) {
                                        // and emit request
                                        onRequestEmitted(request)
                                    }
                                } else {
                                    // if there was only "from" conflict, emit the fixed request now
                                    request.refineTo(throwable.valuesTo[0])
                                    onRequestEmitted(request)
                                }
                            }
                        } else {
                            // if there was only "to" conflict, wait for user input and fire the fixed request
                            request.refineFrom(throwable.valuesFrom[0])
                            showConflictDialog(false, request, throwable.choicesTo, throwable.valuesTo) {
                                onRequestEmitted(request)
                            }
                        }
                    }
                    is SocketTimeoutException -> homepageFramgent.setError("Le serveur ne répond pas.")
                    is HttpStatusException -> homepageFramgent.setError("Le serveur est hors ligne.")
                    else -> homepageFramgent.setError("Une erreur inconnue est survenue.")
                }
            }
        })
    }

    private fun showConflictDialog(from: Boolean, request: Request, choices: ArrayList<String>, values: ArrayList<String>, onFunctionEnd: () -> Unit) {
        StationConflictDialog(
            this, object : StationConflictDialog.StationConflictListener {
                override fun onCancelled() {
                    homepageFramgent.setError("Impossible de poursuivre.")
                }
                override fun onValidated(value: String) {
                    if(from)
                        request.refineFrom(value)
                    else
                        request.refineTo(value)
                    onFunctionEnd()
                }
            }, from, choices, values
        ).show()
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
