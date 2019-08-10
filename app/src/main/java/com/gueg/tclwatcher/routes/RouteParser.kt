package com.gueg.tclwatcher.routes

import android.util.Log
import com.gueg.tclwatcher.routes.Route.TCL
import com.gueg.tclwatcher.routes.Route.Walk
import org.jsoup.Jsoup
import org.jsoup.select.Elements


class RouteParser {

    companion object {
        private const val TAG = "RouteParser"

        private val threads = ArrayList<Thread>()

        fun cancel() {
            for(t in threads)
                t?.interrupt()
            threads.clear()
        }

        @Throws(ParseError::class)
        fun parseRoute(request: Request, routeParserListener: RouteParserListener, url: String="",
                       uncaughtExceptionHandler: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()) {
            val t = Thread {
                var finalUrl = if(url.isEmpty()) request.toString() else url

                if (!finalUrl.contains("http://") && !finalUrl.contains("https://")) {
                    val prefix = "http://m.tcl.fr"
                    finalUrl = String.format("%s%s", prefix, finalUrl)
                }

                Log.d(":-:$TAG", finalUrl)
                val page = Jsoup.connect(finalUrl).get()

                val date = page.select("input[name=selectedDate]").first().attr("value").toString() // 2019|7|31

                val prev = page.getElementsByClass("TRAJET-prec").attr("href")
                val next = page.getElementsByClass("TRAJET-suiv").attr("href")

                val departureTime = page.getElementsByClass("HEURE-depart").text() // 15 h 49
                val arrivalTime = page.getElementsByClass("HEURE-arrivee").text() // 16 h 19
                val routeLength = page.getElementsByClass("decompte-minute").text() // 30 min

                val route = Route(
                    from = request.from, to = request.to,
                    departureTime = departureTime, arrivalTime = arrivalTime, totalDuration = routeLength, date = date,
                    prev = prev, next = next,
                    request = finalUrl
                )

                val rows: Elements
                try {
                    // if everything went as planned
                    rows = page.getElementsByClass("RESULTAT-TRAJET")[0].getElementsByClass("row")
                } catch(e: IndexOutOfBoundsException) {
                    try {
                        // if there was any other error (out of date range, same station selected...)
                        val error = page.getElementsByClass("ERROR")[0].text()
                        throw ParseError(error)
                    } catch(e: IndexOutOfBoundsException) {
                        try {
                            // if the station name was ambiguous, giving multiple results in a table
                            val tableFrom = page.getElementsByClass("TABLE-form-precise")[0]
                            val tableFromRows = tableFrom.select("tr")
                            val choicesFrom = ArrayList<String>()
                            val valuesFrom = ArrayList<String>()

                            for (tableRow in tableFromRows) {
                                if (tableRow.select("input[name=arretDepart]").first().attr("value").toString() != "#") { // "Modifier ma demande"
                                    val id = tableRow.select("input[name=arretDepart]").first().attr("id")
                                    choicesFrom.add(tableRow.select("label[for=$id]").first().text())
                                    valuesFrom.add(tableRow.select("input[name=arretDepart]").first().attr("value").toString())
                                }
                            }

                            val tableTo = page.getElementsByClass("TABLE-form-precise")[1]
                            val tableToRows = tableTo.select("tr")
                            val choicesTo = ArrayList<String>()
                            val valuesTo = ArrayList<String>()

                            for (tableRow in tableToRows) {
                                if (tableRow.select("input[name=arretArrivee]").first().attr("value").toString() != "#") { // "Modifier ma demande"
                                    val id = tableRow.select("input[name=arretArrivee]").first().attr("id")
                                    choicesTo.add(tableRow.select("label[for=$id]").first().text())
                                    valuesTo.add(tableRow.select("input[name=arretArrivee]").first().attr("value").toString())
                                }
                            }

                            throw StationConflictError(
                                choicesFrom,
                                valuesFrom,
                                choicesTo,
                                valuesTo
                            )
                        } catch(e: IndexOutOfBoundsException) {
                            throw ParseError("Erreur inconnue")
                        }
                    }
                }

                for (row in rows) {
                    // walk
                    if (row.select(".mode-de-transport.pieton").size > 0) {
                        val duration = row.select(".mode-de-transport.pieton").text() // 1min
                        val indications = row.select(".etape-indication-unique").text() // Marcher 1 min (2450m)

                        route.add(Walk(duration = duration, additionalInfos = indications))
                    }
                    // TCL
                    else if (row.select(".mode-de-transport.TCL").size > 0) {
                        val images = row.select(".largeur-image").select("img")
                        val first = images[0].absUrl("src")
                        val second = images[1].absUrl("src")

                        val duration = row.select(".mode-de-transport.TCL").text() // 5 min

                        val firstStep = row.selectFirst(".row.etape-un")
                        val departAt = firstStep.select(".chronometrage-arret").text() // 15:50
                        val from = firstStep.select(".nom-etape").text() // Jean XXIII - Maryse Bastié
                        val fromDir = firstStep.select(".nom-direction").text() // Direction  Perrache

                        val secondStep = row.selectFirst(".row.etape-deux")
                        val arriveAt = secondStep.select(".chronometrage-arret").text() // 15:55
                        val to = secondStep.select(".nom-etape2").text() // Jet d'Eau - Mendes France

                        route.add(TCL(
                            from=from, fromDir=fromDir, to=to,
                            departAt=departAt, arriveAt=arriveAt, duration=duration,
                            picLeft=first, picRight=second)
                        )
                    }
                }

                routeParserListener.onRouteParsed(route)

                threads.remove(Thread.currentThread())

            }.apply {
                this.uncaughtExceptionHandler = uncaughtExceptionHandler
                start()
            }
            threads.add(t)
        }
    }


    class ParseError(message: String) : RuntimeException(message)
    class StationConflictError(val choicesFrom: ArrayList<String>, val valuesFrom: ArrayList<String>, val choicesTo: ArrayList<String>, val valuesTo: ArrayList<String>) : RuntimeException()

    interface RouteParserListener {
        fun onRouteParsed(route: Route)
    }
}