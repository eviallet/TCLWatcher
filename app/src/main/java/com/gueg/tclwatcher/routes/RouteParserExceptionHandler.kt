package com.gueg.tclwatcher.routes

import android.util.Log
import com.gueg.tclwatcher.MainActivity
import com.gueg.tclwatcher.routes.RouteParser.ConnectionError
import com.gueg.tclwatcher.routes.RouteParser.ParseError


class RouteParserExceptionHandler(private val activity: MainActivity) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {
        activity.runOnUiThread {
            Log.w(":-:",throwable!!.javaClass.name)
            Log.w(":-:"," -> ${throwable.message}")
            for(a in throwable.stackTrace) Log.w(":-:", "\t$a")
            when (throwable) {
                is ParseError -> activity.setError(throwable.message!!)
                is ConnectionError -> activity.setError(throwable.message!!)
                else -> activity.setError("Une erreur inconnue est survenue.")
            }
        }
    }


}
