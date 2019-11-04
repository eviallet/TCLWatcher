package com.gueg.tclwatcher.routes

import android.content.Context
import android.util.Log
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.gueg.tclwatcher.stations.StationConflictDialog
import com.gueg.tclwatcher.stations.stations_db.StationDatabaseUtility
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class RouteRequestBuilder(private val context: Context) {

    companion object {
        fun with(context: Context) : RouteRequestBuilder {
            return RouteRequestBuilder(context)
        }
    }

    private val queue = Volley.newRequestQueue(context)

    private lateinit var fromStr : String
    private lateinit var toStr : String

    fun from(str: String) : RouteRequestBuilder {
        fromStr = str
        return this
    }

    fun to(str: String) : RouteRequestBuilder {
        toStr = str
        return this
    }

    fun build(then : (from: String, to: String) -> Unit) {
        StationDatabaseUtility.findDataByName(context, fromStr) { fromId ->
            if(fromId.isNotEmpty()) {
                fromStr = fromId
                searchTo(then)
            } else {
                queue.add(
                    PlacesParser(fromStr) { results, values, types ->
                        if(results.size == 1) {
                            fromStr = values[0]
                            searchTo(then)
                        } else {
                            StationConflictDialog(context, object: StationConflictDialog.StationConflictListener {
                                override fun onValidated(value: String) {
                                    fromStr = value
                                    searchTo(then)
                                }

                                override fun onCancelled() { then("","") }
                            }, from=true, choices=results, values=values, types=types).show()
                        }
                    }
                )
            }
        }
    }

    fun searchTo(then : (from: String, to: String) -> Unit) {
        StationDatabaseUtility.findDataByName(context, toStr) { toId ->
            if(toId.isNotEmpty()) {
                toStr = toId
                then(fromStr, toStr)
            } else {
                queue.add(
                    PlacesParser(toStr) { results, values, types ->
                        if(results.size == 1) {
                            fromStr = values[0]
                            searchTo(then)
                        } else {
                            StationConflictDialog(context, object: StationConflictDialog.StationConflictListener {
                                override fun onValidated(value: String) {
                                    toStr = value
                                    then(fromStr, toStr)
                                }

                                override fun onCancelled() { then("","") }
                            }, from=false, choices=results, values=values, types=types).show()
                        }
                    }
                )
            }
        }
    }


    inner class PlacesParser(query: String, then: (resultsNames: ArrayList<String>, values: ArrayList<String>, types: ArrayList<ResultType>) -> Unit) : StringRequest(
        Method.GET,
        "https://www.tcl.fr/api/navitia/search-places?q=${URLEncoder.encode(query)}",
        Response.Listener<String> { response ->
            val array = JSONArray(response)
            val resultsNames = ArrayList<String>()
            val values = ArrayList<String>()
            val types = ArrayList<ResultType>()

            for(i in 0 until array.length()) {
                val obj = array[i] as JSONObject

                resultsNames.add(obj["label"] as String)
                values.add(obj["id"] as String)
                types.add(
                    when(obj["type"] as String) {
                        "address" -> ResultType.ADDRESS
                        "poi" -> ResultType.POI
                        "stop_area" -> ResultType.STOP_AREA
                        else -> ResultType.ADDRESS
                    }
                )
            }

            then(resultsNames, values, types)
        },
        Response.ErrorListener { err ->
            Log.d(":-:","RouteRequestBuilder.PlacesParser.ErrorListener")
            err.printStackTrace()
        }
    ) {
        override fun getHeaders(): Map<String, String> {
            val headers = HashMap<String, String>()
            headers["accept"] = "*/*"
            headers["accept-language"] = "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7"
            headers["cookie"] = "__utma=80329874.1283960675.1568106195.1568106195.1568106195.1; __utmz=80329874.1568106195.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __cfduid=ddad22025d09750bdd5b993c427e463541572163842"
            headers["dnt"] = "1"
            headers["referer"] = ("https://www.tcl.fr/")
            headers["sec-fetch-mode"] = "cors"
            headers["sec-fetch-site"] = "same-origin"
            headers["user-agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36 OPR/63.0.3368.107"

            return headers
        }
    }

    enum class ResultType {
        STOP_AREA,
        POI,
        ADDRESS
    }
}
