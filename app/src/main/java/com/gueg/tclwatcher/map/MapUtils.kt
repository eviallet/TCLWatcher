package com.gueg.tclwatcher.map

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import com.gueg.tclwatcher.ImageLoader
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.stations.Station
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme


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

        fun getFastMarkersFromStations(map: MapView, stations: List<Station>): SimpleFastPointOverlay {
            val points = ArrayList<IGeoPoint>()

            for(station in stations)
                points.add(LabelledGeoPoint(station.lat, station.lon, station.name))

            val pt = SimplePointTheme(points, true)

            val textStyle = Paint()
            textStyle.style = Paint.Style.FILL
            textStyle.color = map.context.resources.getColor(R.color.colorAccent)
            textStyle.textAlign = Paint.Align.CENTER
            textStyle.textSize = 30f
            textStyle.isFakeBoldText = true

            val opt = SimpleFastPointOverlayOptions.getDefaultStyle()
                .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                .setRadius(13f).setIsClickable(true).setTextStyle(textStyle)
                .setLabelPolicy(SimpleFastPointOverlayOptions.LabelPolicy.DENSITY_THRESHOLD).setMaxNShownLabels(20)
                .setPointStyle(textStyle)

            return SimpleFastPointOverlay(pt, opt)
        }

        fun getMarkerFromTCL(map: MapView, geoPoint: GeoPoint, activity: MapActivity, name: String): Marker {
            val m = Marker(map)
            m.title = ""
            // TODO
            m.icon = ImageLoader.loadDrawable(activity, name)
            m.position = geoPoint
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


        fun getBoundaries(coords: ArrayList<GeoPoint>): BoundingBox {
            var latitudeMin = 90.0
            var latitudeMax = -90.0
            var longitudeMin = 90.0
            var longitudeMax = -90.0

            for(i in 0..coords.lastIndex)
                if(coords[i].latitude < latitudeMin && coords[i].latitude != 0.0)
                    latitudeMin = coords[i].latitude
            for(i in 0..coords.lastIndex)
                if(coords[i].latitude > latitudeMax && coords[i].latitude != 0.0)
                    latitudeMax = coords[i].latitude
            for(i in 0..coords.lastIndex)
                if(coords[i].longitude < longitudeMin && coords[i].latitude != 0.0)
                    longitudeMin = coords[i].longitude
            for(i in 0..coords.lastIndex)
                if(coords[i].longitude > longitudeMax && coords[i].latitude != 0.0)
                    longitudeMax = coords[i].longitude

            return BoundingBox(latitudeMax + 0.008, longitudeMax + 0.008, latitudeMin - 0.008, longitudeMin - 0.008)
        }

        fun getBoundariesFromStations(stations: List<Station>): BoundingBox {
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