package com.gueg.tclwatcher.routes

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.android.volley.NoConnectionError
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.UnknownHostException


class RouteParser {

    companion object {
        private const val TAG = "RouteParser"

        private val threads = ArrayList<Thread>()

        fun cancel() {
            for (t in threads)
                t?.interrupt()
            threads.clear()
        }

        private fun formatTime(str: String) : String {
            // str : 20191028T151800
            val s = str.split("T")[1]      // T151800
                .substring(0,4)     // 1518
            return "${s.substring(0,2)}:${s.substring(2)}" // 15:18
        }

        private fun formatDate(str: String) : String {
            // str : 20191028T151800
            val s = str.split("T")[0]      // 20191028
                .substring(4)     // 1028
            return "${s.substring(2)}/${s.substring(0,2)}" // 28/10
        }

        private fun formatSeconds(totalSecs: Int) : String {
            val hours = totalSecs / 3600
            val min = (totalSecs % 3600) / 60

            return if(hours > 0 && min == 0)
                "$hours h"
            else if(hours == 0 && min > 0)
                "$min min"
            else if(min > 10)
                "${hours}h$min"
            else
                "${hours}h0$min"
        }

        @Throws(Exception::class)
        fun parseRoute(context: Context, request: RouteRequest, routeParserListener: RouteParserListener,
                       uncaughtExceptionHandler: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()!!) {
            val queue = Volley.newRequestQueue(context)

            queue.add(request.build(
                Response.Listener { response ->
                    val t = Thread {
                        val jsonResponse = JSONObject(response)

                        val journey : JSONObject?
                        try {
                            journey = (jsonResponse["journeys"] as JSONArray)[0] as JSONObject
                        } catch(e: JSONException) {
                            Log.d(":-:","Aucun itinéraire trouvé")
                            throw ParseError("Aucun itinéraire trouvé.")
                        }

                        val date = formatDate((journey["departure_date_time"] as String))
                        val departureTime = formatTime((journey["departure_date_time"] as String))
                        val arrivalTime = formatTime((journey["arrival_date_time"] as String))
                        val routeLength = formatSeconds((journey["durations"] as JSONObject)["total"] as Int)

                        val links = jsonResponse["links"] as JSONObject
                        val prev = (links["prev"] as JSONObject)["datetime"] as String
                        val next = (links["next"] as JSONObject)["datetime"] as String

                        val route = Route(
                            from = request.from, to = request.to,
                            departureTime = departureTime, arrivalTime = arrivalTime, totalDuration = routeLength, date = date,
                            prev = prev, next = next
                        )

                        val sections = journey["sections"] as JSONArray

                        // TODO /!\ WARNINGS

                        for(i in 0 until sections.length()) {
                            val section = sections[i] as JSONObject

                            if((section["type"] as String) == "transfer")
                                continue

                            when(section["type"] as String) {
                                "waiting" -> {
                                    val duration = section["duration"] as Int
                                    if(duration > 120)
                                        route.add(Route.Wait(duration = formatSeconds(duration)))
                                }
                                "public_transport" -> {
                                    val transportCode = (section["display_informations"] as JSONObject)["code"] as String
                                    val picUrl = "https://carte.tcl.fr/assets/images/lines/$transportCode.svg"

                                    val displayInformations = section["display_informations"] as JSONObject

                                    val geojson = section["geojson"] as JSONObject
                                    val coordinates = geojson["coordinates"] as JSONArray
                                    val coords = ArrayList<GeoPoint>()

                                    for(j in 0 until coordinates.length()) {
                                        val point = coordinates[j] as JSONArray
                                        val lon = point[0] as Double
                                        val lat = point[1] as Double
                                        coords.add(GeoPoint(lat, lon))
                                    }
                                    val color = Color.parseColor("#${displayInformations["color"] as String}")

                                    route.add(
                                        Route.TCL(
                                            from = ((section["from"] as JSONObject)["stop_point"] as JSONObject)["name"] as String,
                                            fromDir = displayInformations["direction"] as String,
                                            to = ((section["to"] as JSONObject)["stop_point"] as JSONObject)["name"] as String,
                                            departAt = formatTime(section["departure_date_time"] as String),
                                            arriveAt = formatTime(section["arrival_date_time"] as String),
                                            duration = formatSeconds(section["duration"] as Int),
                                            pic = picUrl,
                                            coords = coords, color = color
                                        )
                                    )
                                }
                                else -> {
                                    when(section["mode"] as String) {
                                        "walking" -> {
                                            val duration = section["duration"] as Int
                                            if(duration > 120)
                                                route.add(Route.Walk(duration = formatSeconds(duration)))
                                        }
                                    }
                                }
                            }
                        }

                        routeParserListener.onRouteParsed(route)

                        threads.remove(Thread.currentThread())
                    }.apply {
                        this.uncaughtExceptionHandler = uncaughtExceptionHandler
                        start()
                    }
                    threads.add(t)
                },
                Response.ErrorListener { err ->
                    when(err) {
                        is NoConnectionError -> uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), ConnectionError("Aucune connexion internet."))
                        is UnknownHostException -> uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), ConnectionError("Serveurs hors ligne."))
                        else -> uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), RuntimeException("Une erreur inconnue est survenue."))
                    }
                }))
        }

    }

    class ConnectionError(message: String) : RuntimeException(message)
    class ParseError(message: String) : RuntimeException(message)

    interface RouteParserListener {
        fun onRouteParsed(route: Route)
    }

}