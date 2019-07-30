package com.gueg.tclwatcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import kotlin.random.Random




@Suppress("PrivatePropertyName")
class MapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PATH = "com.gueg.tclwatcher.mapactivity.extra_path"
        const val EXTRA_STATION = "com.gueg.tclwatcher.mapactivity.extra_station"
    }

    private lateinit var map: MapView

    private val COLORS = arrayOf(
        0xb32424,
        0xcf660a,
        0xd4e024,
        0x8ddb2e,
        0x389632,
        0x0aa65d,
        0x2a9da1,
        0xb51058
    )

    private fun loadActivity() {
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.activity_map_map)

        // Map configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
        map.setTileSource(TileSourceFactory.MAPNIK)

        var startingGeoPoint: GeoPoint ?= null

        val extras = intent.extras
        if(extras != null) {
            val doubleArray = extras.getDoubleArray(EXTRA_PATH)
            val stationName = extras.getString(EXTRA_STATION)
            if(doubleArray != null) {
                for(i in 0 until doubleArray.size - 1 step 4) {
                    val path = Polyline(map)
                    path.color = COLORS[abs(Random.nextInt()) % COLORS.size]
                    path.addPoint(GeoPoint(doubleArray[i], doubleArray[i + 1]))
                    path.addPoint(GeoPoint(doubleArray[i + 2], doubleArray[i + 3]))
                    path.width = 70f
                    map.overlays.add(path)
                    map.invalidate()
                    if(i == 0) startingGeoPoint = path.points[0]
                }
            } else if(stationName != null) {
                Thread {
                    val station = StationDatabase.getDatabase(this).stationDao().findByName(stationName)
                    val marker = MapOverlayProvider.getMarkerFromStation(map, station)
                    map.overlays.add(marker)
                    runOnUiThread {
                        map.invalidate()
                        startingGeoPoint = marker.position
                        map.controller.setCenter(startingGeoPoint)
                    }
                }.start()
            }
        }

        // Position overlay
        val location = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        location.enableMyLocation()
        val myLocation = location.myLocation
        map.overlays.add(location)

        val mapController = map.controller
        mapController.setZoom(18.0)

        when {
            startingGeoPoint != null -> mapController.setCenter(startingGeoPoint)
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