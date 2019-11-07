package com.gueg.tclwatcher.routes

/*
class RouteParserExceptionHandler(private val activity: MainActivity, private val request: Request, private val onRefined: ((refined: Request) -> Unit) ?= null) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {
        activity.runOnUiThread {
            Log.w(":-:",throwable!!.javaClass.name)
            Log.w(":-:"," -> ${throwable.message}")
            for(a in throwable.stackTrace) Log.w(":-:", "\t$a")
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
                                    if(onRefined != null)
                                        onRefined!!(request)
                                    else
                                        activity.onRequestEmitted(request)
                                }
                            } else {
                                // if there was only "from" conflict, emit the fixed request now
                                request.refineTo(throwable.valuesTo[0])
                                if(onRefined != null)
                                    onRefined!!(request)
                                else
                                    activity.onRequestEmitted(request)
                            }
                        }
                    } else {
                        // if there was only "to" conflict, wait for user input and fire the fixed request
                        request.refineFrom(throwable.valuesFrom[0])
                        showConflictDialog(false, request, throwable.choicesTo, throwable.valuesTo) {
                            if(onRefined != null)
                                onRefined!!(request)
                            else
                                activity.onRequestEmitted(request)
                        }
                    }
                }
                is SocketTimeoutException -> activity.setError("Le serveur ne répond pas.")
                is HttpStatusException -> activity.setError("Le serveur est hors ligne.")
                is UnknownHostException -> activity.setError("Aucune connexion internet.")
                else -> activity.setError("Une erreur inconnue est survenue.")
            }
        }
    }


}
*/