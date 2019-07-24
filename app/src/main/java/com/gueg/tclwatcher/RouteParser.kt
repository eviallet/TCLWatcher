package com.gueg.tclwatcher

import android.util.Log
import com.gueg.tclwatcher.Route.TCL
import com.gueg.tclwatcher.Route.Walk
import org.jsoup.Jsoup


class RouteParser {

    companion object {
        private const val TAG = "RouteParser"

        fun parseRoute(request: Request, routeParserListener: RouteParserListener, url: String="") {
            Thread {
                var finalUrl = if(url.isEmpty()) request.toString() else url

                if (!finalUrl.contains("http://") && !finalUrl.contains("https://")) {
                    val prefix = "http://m.tcl.fr"
                    finalUrl = String.format("%s%s", prefix, finalUrl)
                }

                Log.d(":-:$TAG", finalUrl)
                val page = Jsoup.connect(finalUrl).get()

                val prev = page.getElementsByClass("TRAJET-prec").attr("href")
                val next = page.getElementsByClass("TRAJET-suiv").attr("href")

                val departureTime = page.getElementsByClass("HEURE-depart").text() // 15 h 49
                val arrivalTime = page.getElementsByClass("HEURE-arrivee").text() // 16 h 19
                val routeLength = page.getElementsByClass("decompte-minute").text() // 30 min

                val route = Route(
                    from = request.from, to = request.to,
                    departureTime = departureTime, arrivalTime = arrivalTime, totalDuration = routeLength,
                    prev = prev, next = next
                )

                val rows = page.getElementsByClass("RESULTAT-TRAJET")[0].getElementsByClass("row")

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

            }.start()
        }
    }


    interface RouteParserListener {
        fun onRouteParsed(route: Route)
    }
}