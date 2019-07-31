package com.gueg.tclwatcher

import android.util.Log
import org.jsoup.HttpStatusException
import java.net.SocketTimeoutException

class RouteParserExceptionHandler(private val activity: MainActivity, private val request: Request) : Thread.UncaughtExceptionHandler {


    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {
        activity.runOnUiThread {
            Log.d(":-:",throwable!!.javaClass.name)
            when (throwable) {
                is RouteParser.ParseError -> activity.setError(throwable.message.toString().split(".")[0].plus("."))
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
                                    activity.onRequestEmitted(request)
                                }
                            } else {
                                // if there was only "from" conflict, emit the fixed request now
                                request.refineTo(throwable.valuesTo[0])
                                activity.onRequestEmitted(request)
                            }
                        }
                    } else {
                        // if there was only "to" conflict, wait for user input and fire the fixed request
                        request.refineFrom(throwable.valuesFrom[0])
                        showConflictDialog(false, request, throwable.choicesTo, throwable.valuesTo) {
                            activity.onRequestEmitted(request)
                        }
                    }
                }
                is SocketTimeoutException -> activity.setError("Le serveur ne répond pas.")
                is HttpStatusException -> activity.setError("Le serveur est hors ligne.")
                else -> activity.setError("Une erreur inconnue est survenue.")
            }
        }
    }


    private fun showConflictDialog(from: Boolean, request: Request, choices: ArrayList<String>, values: ArrayList<String>, onFunctionEnd: () -> Unit) {
        StationConflictDialog(
            activity, object : StationConflictDialog.StationConflictListener {
                override fun onCancelled() {
                    activity.setError("Impossible de poursuivre.")
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
}