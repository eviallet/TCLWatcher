package com.gueg.tclwatcher.map

import android.Manifest
import android.content.pm.PackageManager
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
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


@Suppress("PrivatePropertyName")
class MapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "com.gueg.tclwatcher.mapactivity.extra_path"
        const val EXTRA_COORDS = "com.gueg.tclwatcher.mapactivity.extra_coords"
        const val EXTRA_COLOR = "com.gueg.tclwatcher.mapactivity.extra_color"
        const val EXTRA_COLORS = "com.gueg.tclwatcher.mapactivity.extra_colors"
        const val EXTRA_TCL_NAME = "com.gueg.tclwatcher.mapactivity.extra_tcl_name"
        const val EXTRA_TCL_NAMES = "com.gueg.tclwatcher.mapactivity.extra_tcl_names"
        const val EXTRA_STATION_NAME_FROM = "com.gueg.tclwatcher.mapactivity.extra_station_name_from"
        const val EXTRA_STATION_NAME_TO = "com.gueg.tclwatcher.mapactivity.extra_station_name_to"
        const val EXTRA_STATIONS_NAME_FROM = "com.gueg.tclwatcher.mapactivity.extra_stations_name_from"
        const val EXTRA_STATIONS_NAME_TO = "com.gueg.tclwatcher.mapactivity.extra_stations_name_to"
    }

    private lateinit var map: MapView
    private var isInit = false

    private fun loadActivity() {
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.activity_map_map)
        isInit = true

        // Map configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val extras = intent.extras
        if(extras != null) {
            val coords = extras.getParcelableArrayList<GeoPoint>(EXTRA_COORDS)
            if(coords != null) {
                val color = extras.getInt(EXTRA_COLOR)
                val tcl = extras.getString(EXTRA_TCL_NAME)!!
                val stationFrom = extras.getString(EXTRA_STATION_NAME_FROM)!!
                val stationTo = extras.getString(EXTRA_STATION_NAME_TO)!!
                Thread {
                    val from = StationDatabase.getDatabase(applicationContext).stationDao().findByName(stationFrom)
                    val to = StationDatabase.getDatabase(applicationContext).stationDao().findByName(stationTo)

                    runOnUiThread {
                        map.overlayManager.add(MapUtils.getMarkerFromStation(map, from))
                        map.overlayManager.add(MapUtils.getMarkerFromStation(map, to))

                        val path = Polyline()
                        path.color = color
                        for (i in 0..coords.lastIndex)
                            path.addPoint(coords[i])

                        map.overlayManager.add(
                            MapUtils.getMarkerFromTCL(
                                map,
                                MapUtils.getCenterFromPath(path),
                                this,
                                tcl
                            )
                        )

                        map.overlayManager.add(path)

                        map.zoomToBoundingBox(MapUtils.getBoundaries(coords), true)
                    }
                }.start()
            } else {
                val fullPath = extras.getParcelableArrayList<GeoPoint>(EXTRA_PATH)!!
                val colors = extras.getIntegerArrayList(EXTRA_COLORS)!!
                val tclNames = extras.getStringArrayList(EXTRA_TCL_NAMES)!!
                val fromNames = extras.getStringArrayList(EXTRA_STATIONS_NAME_FROM)!!
                val toNames = extras.getStringArrayList(EXTRA_STATIONS_NAME_TO)!!
                val markerData = ArrayList<GeoPoint>()

                Thread {
                    val separator = GeoPoint(0,0)
                    var sections = 0
                    var j = 0
                    val paths = ArrayList<Polyline>()

                    for(i in 0 until fullPath.size - 1) {
                        if(i<j)
                            continue

                        val color = colors[sections]
                        sections++
                        val path = Polyline()
                        path.color = color
                        while(fullPath[j] != separator && j < fullPath.size - 1) {
                            path.addPoint(fullPath[j])
                            j++
                        }
                        markerData.add(MapUtils.getCenterFromPath(path))
                        paths.add(path)
                        j++
                    }

                    val stations = ArrayList<Station>()

                    for (fromName in fromNames)
                        if(!contains(stations, fromName))
                            stations.add(StationDatabase.getDatabase(applicationContext).stationDao().findByName(fromName))
                    for (toName in toNames)
                        if(!contains(stations, toName))
                            stations.add(StationDatabase.getDatabase(applicationContext).stationDao().findByName(toName))

                    runOnUiThread {
                        for(i in 0..tclNames.lastIndex) {
                            map.overlayManager.add(
                                MapUtils.getMarkerFromTCL(
                                    map,
                                    markerData[i],
                                    this,
                                    tclNames[i]
                                )
                            )
                        }
                        for(station in stations)
                            map.overlayManager.add(MapUtils.getMarkerFromStation(map, station))
                        for(path in paths)
                            map.overlayManager.add(path)
                        map.invalidate()
                        map.zoomToBoundingBox(MapUtils.getBoundaries(fullPath), true)
                    }
                }.start()
            }
        } else {
            Thread {
                val stations = StationDatabase.getDatabase(this).stationDao().all
                map.overlays.add(MapUtils.getFastMarkersFromStations(map, stations))
                runOnUiThread {
                    map.invalidate()
                    map.zoomToBoundingBox(MapUtils.getBoundariesFromStations(stations), true)
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

    private fun contains(stations: ArrayList<Station>, name: String): Boolean {
        for(station in stations)
            if(stations!=null && station.name == name)
                return true
        return false
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
        if(isInit) {
            Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
            map.onResume() //needed for compass, my location overlays, v6.0.0 and up
        }
    }

    public override fun onPause() {
        super.onPause()
        if(isInit) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            Configuration.getInstance().save(this, prefs)
            map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
        }
    }


}