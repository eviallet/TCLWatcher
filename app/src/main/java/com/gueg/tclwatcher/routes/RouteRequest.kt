package com.gueg.tclwatcher.routes

import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

class RouteRequest(
    var from:String, var to:String,
    private var day:Int=-1, private var month:Int=-1, private var year:Int=-1, private var hour:Int=-1, private var minute:Int=-1,
    private var datetime: String = "",
    private val timeMode: TimeMode = TimeMode.DEPART_AT
) {

    @Suppress("PrivatePropertyName")
    private val LINK = "https://carte.tcl.fr/api/itinerary?datetime=#DATE#&from=#DEP#&to=#ARR#&params=#DEPARR#,metro,funiculaire,tramway,bus"

    enum class TimeMode {
        DEPART_AT,
        ARRIVE_AT
    }

    fun build(responseListener : Response.Listener<String>, errorListener : Response.ErrorListener) : StringRequest {
        val date = when {
            datetime.isNotEmpty() -> datetime
            day==-1 -> "now"
            else -> "${year}${addLeadingZero(month)}${addLeadingZero(day)}T${addLeadingZero(hour)}${addLeadingZero(minute)}00"
        }

        return object : StringRequest(
            Method.GET,
            LINK.replace("#DATE#", date).replace("#DEP#", from).replace("#ARR#", to).replace("#DEPARR#", if(timeMode == TimeMode.DEPART_AT) "departure" else "arrival"),
            responseListener,
            errorListener
        ) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["authority"] = "carte.tcl.fr"
                headers["accept"] = "application/json, text/plain, */*"
                //headers["accept-encoding"] = "gzip, deflate, br"
                headers["accept-language"] = "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7"
                headers["cookie"] = "__utma=80329874.1283960675.1568106195.1568106195.1568106195.1; __utmz=80329874.1568106195.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __cfduid=ddad22025d09750bdd5b993c427e463541572163842"
                headers["dnt"] = "1"
                headers["if-none-match"] = "W/\"9f84-oiVWZzzpp0NTfcj/iObE+Wl1qtA-gzip\""
                headers["referer"] = ("https://carte.tcl.fr/route-calculation?from=#DEP#&to=#ARR#").replace("#DEP#", from).replace("#ARR#", to)
                headers["sec-fetch-mode"] = "cors"
                headers["sec-fetch-site"] = "same-origin"
                headers["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36 OPR/63.0.3368.107"

                return headers
            }
        }
    }

    private fun addLeadingZero(num: Int): String {
        return if(num < 10)
            "0$num"
        else "$num"
    }
}