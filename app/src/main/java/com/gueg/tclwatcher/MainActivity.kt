package com.gueg.tclwatcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.transition.AutoTransition
import android.transition.Explode
import android.util.Log
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.gueg.tclwatcher.LoadingFragment.LoadingText.INSERT_DB
import com.gueg.tclwatcher.LoadingFragment.LoadingText.LOAD_ONLINE
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException


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
        }, uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, throwable ->
            runOnUiThread {
                Log.d(":-:",throwable.javaClass.name)
                when (throwable) {
                    is RouteParser.ParseError -> setError(throwable.message.toString().split(".")[0].plus("."))
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
                    is SocketTimeoutException -> setError("Le serveur ne répond pas.")
                    is HttpStatusException -> setError("Le serveur est hors ligne.")
                    else -> setError("Une erreur inconnue est survenue.")
                }
            }
        })
    }

    private fun showConflictDialog(from: Boolean, request: Request, choices: ArrayList<String>, values: ArrayList<String>, onFunctionEnd: () -> Unit) {
        StationConflictDialog(
            this, object : StationConflictDialog.StationConflictListener {
                override fun onCancelled() {
                    setError("Impossible de poursuivre.")
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
