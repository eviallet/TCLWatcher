package com.gueg.tclwatcher.map

import android.content.Intent
import android.net.Uri
import android.widget.TextView
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.stations.Station
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow


class MapUtils {
    companion object {

        fun getMarkerFromStation(map: MapView, station: Station): Marker {
            val m = Marker(map)
            m.icon = map.context.resources.getDrawable(R.drawable.pin)
            m.relatedObject = station
            m.title = station.name
            m.position = GeoPoint(station.lat, station.lon)
            m.setInfoWindow(MarkerInfoWindow(R.layout.map_info_window, map))
            m.showInfoWindow()
            m.setOnMarkerClickListener { marker: Marker, mapView: MapView ->
                val uri = "google.navigation:q=${marker.position.latitude},${marker.position.longitude}&mode=w"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                mapView.context.startActivity(intent)
                true
            }
            return m
        }

        fun getMarkerFromIndex(map: MapView, index: Int, geoPoint: GeoPoint, color: Int): Marker {
            val m = Marker(map)
            m.icon = map.context.resources.getDrawable(R.drawable.ic_map_no_icon)
            m.title = index.toString()
            m.position = geoPoint
            m.setInfoWindow(MarkerInfoWindow(R.layout.map_path_index, map))
            m.showInfoWindow()
            m.infoWindow.view.findViewById<TextView>(R.id.bubble_title).setTextColor(color)
            return m
        }

        fun getCenterFromPath(path: Polyline): GeoPoint {
            var lat = 0.0
            var lon = 0.0

            for(geoPoint in path.points) {
                lat += geoPoint.latitude
                lon += geoPoint.longitude
            }

            return GeoPoint(lat/path.points.size, lon/path.points.size)
        }


        fun getBoundaries(stations: ArrayList<Station>): BoundingBox {
            var latMin = 90.0
            var latMax = -90.0
            var lonMin = 90.0
            var lonMax = -90.0

            for(i in 0..stations.lastIndex)
                if(stations[i].lat < latMin)
                    latMin = stations[i].lat
            for(i in 0..stations.lastIndex)
                if(stations[i].lat > latMax)
                    latMax = stations[i].lat
            for(i in 0..stations.lastIndex)
                if(stations[i].lon < lonMin)
                    lonMin = stations[i].lon
            for(i in 0..stations.lastIndex)
                if(stations[i].lon > lonMax)
                    lonMax = stations[i].lon

            return BoundingBox(latMax + 0.008, lonMax + 0.008, latMin - 0.008, lonMin - 0.008)
        }

    }
}