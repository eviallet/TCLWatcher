package com.gueg.tclwatcher

import android.content.Intent
import android.net.Uri
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow


class MapOverlayProvider {
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

    }
}