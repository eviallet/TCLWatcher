package com.gueg.tclwatcher.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.StationDatabase
import com.gueg.tclwatcher.stations.Station
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import kotlin.random.Random


@Suppress("PrivatePropertyName")
class MapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "com.gueg.tclwatcher.mapactivity.extra_path"
        const val EXTRA_STATION_FROM = "com.gueg.tclwatcher.mapactivity.extra_station_from"
        const val EXTRA_STATION_TO = "com.gueg.tclwatcher.mapactivity.extra_station_to"
    }

    private lateinit var map: MapView

    private val COLORS = arrayOf(
        "#b32424",
        "#cf660a",
        "#d4e024",
        "#8ddb2e",
        "#389632",
        "#0aa65d",
        "#2a9da1",
        "#b51058"
    )

    private fun loadActivity() {
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.activity_map_map)

        // Map configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val extras = intent.extras
        if(extras != null) {
            val stringArray = extras.getStringArrayList(EXTRA_PATH)
            val from = extras.getString(EXTRA_STATION_FROM)
            val to = extras.getString(EXTRA_STATION_TO)
            if(stringArray != null) {
                Thread {
                    val stations = ArrayList<Station>()
                    for (stationName in stringArray)
                        stations.add(StationDatabase.getDatabase(applicationContext).stationDao().findByName(stationName))

                    val markers = HashMap<String, Marker>()
                    runOnUiThread {
                        for (i in 0..stations.lastIndex step 2) {
                            val path = Polyline()
                            val color = Color.parseColor(COLORS[abs(Random.nextInt()) % COLORS.size])
                            path.color = color
                            path.addPoint(GeoPoint(stations[i].lat, stations[i].lon))
                            path.addPoint(GeoPoint(stations[i + 1].lat, stations[i + 1].lon))
                            map.overlayManager.add(path)
                            map.overlayManager.add(
                                MapUtils.getMarkerFromIndex(
                                    map,
                                    (i + 2) / 2,
                                    MapUtils.getCenterFromPath(path),
                                    color
                                )
                            )

                            if(markers[stations[i].name] == null)
                                markers[stations[i].name] =
                                    MapUtils.getMarkerFromStation(map, stations[i])
                            if(markers[stations[i + 1].name] == null)
                                markers[stations[i + 1].name] =
                                    MapUtils.getMarkerFromStation(
                                        map,
                                        stations[i + 1]
                                    )
                        }
                        for(marker in markers)
                            map.overlayManager.add(marker.value)
                        map.zoomToBoundingBox(MapUtils.getBoundaries(stations), true)
                    }
                }.start()
            } else if(from != null) {
                Thread {
                    val stationFrom = StationDatabase.getDatabase(this).stationDao().findByName(from)
                    val stationTo = StationDatabase.getDatabase(this).stationDao().findByName(to!!)
                    runOnUiThread {
                        val markerFrom =
                            MapUtils.getMarkerFromStation(map, stationFrom)
                        val markerTo = MapUtils.getMarkerFromStation(map, stationTo)
                        map.overlays.add(markerFrom)
                        map.overlays.add(markerTo)
                        map.invalidate()
                        map.zoomToBoundingBox(
                            MapUtils.getBoundaries(
                                arrayListOf(
                                    stationFrom,
                                    stationTo
                                )
                            ), true)
                    }
                }.start()
            }
        } else {
            Thread {
                val stations = StationDatabase.getDatabase(this).stationDao().all
                map.overlays.add(MapUtils.getFastMarkersFromStations(map, stations))
                runOnUiThread {
                    map.invalidate()
                    map.zoomToBoundingBox(MapUtils.getBoundaries(stations), true)
                }
            }.start()
        }

        // Position overlay
        val location = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        location.enableMyLocation()
        val myLocation = location.myLocation
        map.overlays.add(location)

        val mapController = map.controller
        mapController.setZoom(18.0)

        when {
            myLocation != null -> mapController.setCenter(myLocation)
            else -> mapController.setCenter(GeoPoint(45.749939, 4.864925))
        }
    }





    // ===================== PERMISSIONS STUFF =====================

    private val PERMISSION_REQUEST_WRITE = 0
    private val PERMISSION_REQUEST_LOCALISATION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkExternalStoragePermission()
    }

    private fun checkExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_WRITE)
        } else
            checkLocalisationPermission()
    }

    private fun checkLocalisationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCALISATION)
        } else
            loadActivity()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_WRITE ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    checkLocalisationPermission()
                else
                    finish()
            PERMISSION_REQUEST_LOCALISATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadActivity()
            }
        }
    }


    // ===================== ACTIVITY LIFECYCLE STUFF =====================

    public override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    public override fun onPause() {
        super.onPause()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Configuration.getInstance().save(this, prefs)
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }


}