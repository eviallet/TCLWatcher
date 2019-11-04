/**
{
    "type": "FeatureCollection",
    "name": "tcl_sytral.tclarret",
    "features": [
        {
            "type": "Feature",
            "properties": {
                "id": "32154",
                "nom": "Jean XXIII - Maryse Bastié",            <-
                "desserte": "T2:A",
                "pmr": "true",
                "ascenseur": "false",
                "escalator": "false",
                "gid": "483",
                "last_update": "2019-07-16 02:30:58",
                "last_update_fme": "2019-07-16 06:00:19"
            },
            "geometry": {
                "type": "Point",
                "coordinates": [
                    4.874555518878215,                          <-
                    45.7401873741127                            <-
                ]
            }
        },
        ...
    ]
}
*/

package com.gueg.tclwatcher.stations

import android.os.AsyncTask
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class StationParser {

    companion object {
        private const val STATIONS_LINK = "https://download.data.grandlyon.com/wfs/rdata?SERVICE=WFS&VERSION=2.0.0&outputformat=GEOJSON&maxfeatures=100000&request=GetFeature&typename=tcl_sytral.tclarret"

        private class LoadStationsJSON : AsyncTask<String, Void, JSONObject>() {
            public override fun doInBackground(vararg items: String): JSONObject {
                val link = items[0]

                var connection: HttpURLConnection? = null

                var ans = ""

                try {
                    val url = URL(link)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connect()

                    val status = connection.responseCode

                    ans =
                        if(status in 400..500)  connection.errorStream.bufferedReader().use(BufferedReader::readText)
                        else                    connection.inputStream.bufferedReader().use(BufferedReader::readText)

                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    connection?.disconnect()
                }
                return JSONObject(ans)
            }
        }

        fun parseStations() : ArrayList<Station> {
            val stations = LoadStationsJSON()
                .execute(STATIONS_LINK).get()
            val array = stations.getJSONArray("features")
            val ans = ArrayList<Station>()

            for(i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val coordinates = obj.getJSONObject("geometry").getJSONArray("coordinates")
                val station = Station(
                    obj.getJSONObject("properties").getString("nom"),
                    coordinates.getDouble(0),
                    coordinates.getDouble(1),
                    obj.getJSONObject("properties").getInt("id")
                )
                if(!ans.contains(station))
                    ans.add(station)
            }

            return ans
        }

    }

}
