package com.gueg.tclwatcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.transition.AutoTransition
import android.transition.Explode
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.aditya.filebrowser.Constants
import com.aditya.filebrowser.FileChooser
import com.aditya.filebrowser.FolderChooser
import com.gueg.tclwatcher.LoadingFragment.LoadingText.INSERT_DB
import com.gueg.tclwatcher.LoadingFragment.LoadingText.LOAD_ONLINE
import com.gueg.tclwatcher.bookmarks.Bookmark
import com.gueg.tclwatcher.bookmarks.bookmarks_db.BookmarkDatabase
import com.gueg.tclwatcher.map.MapActivity
import com.gueg.tclwatcher.routes.*
import com.gueg.tclwatcher.stations.StationParser
import com.gueg.tclwatcher.stations.StationPicker
import java.io.*
import java.net.URI
import java.util.concurrent.TimeUnit


@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity(), StationPicker.StationPickerListener {

    private lateinit var container: FrameLayout

    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private var errorShown: Boolean = false

    private var loadingFragment = LoadingFragment()
    private var homepageFragment = HomepageFragment()
    private var currentFragment: Fragment ?= null

    private var pendingRequest: String ?= null

    private val MENU_ID_IMPORT = 0
    private val MENU_ID_EXPORT = 1
    private val MENU_ID_UPDATE = 2
    private val MENU_ID_SHARE  = 3

    private val IMPORT_EXPORT_BOOKMARK_LINE_SEP = "¤"
    private val IMPORT_EXPORT_BOOKMARK_SEP = "²"
    private val IMPORT_EXPORT_REFINED_NONE = "_"


    // ======= LISTENERS =======


    private val routeFragmentListener = object: RouteFragment.RouteFragmentListener {
        override fun onStationMap(nameFrom: String, nameTo: String) {
            val intent = Intent(applicationContext, MapActivity::class.java)
            intent.putExtra(MapActivity.EXTRA_STATION_FROM, nameFrom)
            intent.putExtra(MapActivity.EXTRA_STATION_TO, nameTo)
            startActivity(intent)
        }
        override fun onRouteMap(route: Route) {
            val intent = Intent(applicationContext, MapActivity::class.java)
            val stringArrayList = ArrayList<String>()
            for (subroute in route.get()) {
                if (subroute is Route.TCL) {
                    stringArrayList.add(subroute.from)
                    stringArrayList.add(subroute.to)
                }
            }
            startActivity(intent.putExtra(MapActivity.EXTRA_PATH, stringArrayList))
        }
        override fun onShare(request: String) {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, "Partager")
            i.putExtra(Intent.EXTRA_TEXT, request)
            startActivity(Intent.createChooser(i, "Partager avec"))
        }
    }

    private val routeParserListener = object: RouteParser.RouteParserListener {
        override fun onRouteParsed(route: Route) {
            val routesFragment = RoutesFragment()
            routesFragment.route = route
            routesFragment.routeFragmentListener = routeFragmentListener
            setFragment(routesFragment)
        }
    }


    // ======= ACTIVITY =======


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.activity_main_container)
        errorLayout = findViewById(R.id.activity_main_error_layout)
        errorText = findViewById(R.id.activity_main_error_text)

        homepageFragment.setStationPickerListener(this)

        setFragment(loadingFragment)

        loadStations()

        if (intent.data != null) {
            val intentUrl = intent.data!!.toString()
            if (intentUrl.contains("/navitia/itineraire_mobile?") || intentUrl.contains("/navitia/itineraire_mobile?"))
                pendingRequest = intentUrl
        } else {
            updateIfWeekPassed()
        }
    }

    // Trick to make a menu with android:showAsAction="never" and icons
    // https://stackoverflow.com/a/41569543
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_ID_IMPORT, 1, menuIconWithText(resources.getDrawable(R.drawable.ic_load), resources.getString(R.string.activity_main_import)))
        menu.add(Menu.NONE, MENU_ID_EXPORT, 2, menuIconWithText(resources.getDrawable(R.drawable.ic_save), resources.getString(R.string.activity_main_export)))
        menu.add(Menu.NONE, MENU_ID_UPDATE, 3, menuIconWithText(resources.getDrawable(R.drawable.ic_update), resources.getString(R.string.activity_main_update)))
        menu.add(Menu.NONE, MENU_ID_SHARE,  4, menuIconWithText(resources.getDrawable(R.drawable.ic_share), resources.getString(R.string.activity_main_share)))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_ID_IMPORT -> {
                checkReadPermissionAndImport()
                return true
            }
            MENU_ID_EXPORT -> {
                checkWritePermissionAndExport()
                return true
            }
            MENU_ID_UPDATE -> {
                checkWritePermissionAndUpdate(true)
                return true
            }
            MENU_ID_SHARE -> {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plain"
                i.putExtra(Intent.EXTRA_SUBJECT, "Partager")
                i.putExtra(Intent.EXTRA_TEXT, UpdateTask.GITHUB_RELEASES_LINK)
                startActivity(Intent.createChooser(i, "Partager avec"))
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun menuIconWithText(r: Drawable, title: String): CharSequence {
        r.setBounds(0, 0, r.intrinsicWidth, r.intrinsicHeight)
        val sb = SpannableString("    $title")
        val imageSpan = ImageSpan(r, ImageSpan.ALIGN_BOTTOM)
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return sb
    }


    // ======= FRAGMENTS =======


    private fun loadStations() {
        Thread(Runnable {
            var stations = StationDatabase.getDatabase(applicationContext).stationDao().all
            if(stations.isEmpty()) {
                runOnUiThread { loadingFragment.updateText(LOAD_ONLINE) }
                stations = StationParser.parseStations()
                runOnUiThread { loadingFragment.updateText(INSERT_DB) }
                StationDatabase.getDatabase(applicationContext).stationDao().insertAll(stations)
            }
            runOnUiThread {
                homepageFragment.setStations(stations)
                setFragment(homepageFragment)

                //if(pendingRequest != null)
                //    onExternalUrlOpened(pendingRequest!!)
            }
        }).start()
    }

    private fun setFragment(fragment: Fragment) {
        if((currentFragment is RoutesFragment || currentFragment is HomepageFragment) && (fragment is RoutesFragment || fragment is HomepageFragment)) {
            if(!(fragment is RoutesFragment && currentFragment is RoutesFragment)) {
                fragment.enterTransition = Explode(this, null)
                fragment.sharedElementEnterTransition = AutoTransition()
                currentFragment!!.exitTransition = Explode(this, null)
            }

            if(fragment is HomepageFragment)
                fragment.tempStationPickerData = currentFragment!!.view!!.findViewWithTag("transition_picker")
            else if(fragment is RoutesFragment)
                fragment.tempStationPickerData = currentFragment!!.view!!.findViewWithTag("transition_picker")

            supportFragmentManager.beginTransaction()
                .addSharedElement(currentFragment!!.view!!.findViewWithTag("transition_picker"), "transition_picker")
                .replace(container.id, fragment).commit()
        } else
            supportFragmentManager.beginTransaction().replace(container.id, fragment).commit()

        currentFragment = fragment
    }

    override fun onBackPressed() {
        if(currentFragment is RoutesFragment)
            setFragment(homepageFragment)
        else
            super.onBackPressed()
    }


    fun setError(text: String) {
        runOnUiThread {
            errorText.text = text
            if (!errorShown) {
                errorShown = true
                errorLayout.animate().translationYBy(-errorLayout.height.toFloat() + 2)
                Handler().postDelayed({
                    errorLayout.animate().translationY(0f).withEndAction { errorShown = false }
                }, 3000)
            }

            if(currentFragment is HomepageFragment)
                homepageFragment.onError()
            else if(currentFragment is RoutesFragment)
                (currentFragment as RoutesFragment).onError()
        }
    }


    // ======= REQUESTS =======


    override fun onRequestEmitted(request: RouteRequest) {
        RouteParser.cancel()
        RouteParser.parseRoute(this, request, routeParserListener/*, uncaughtExceptionHandler = RouteParserExceptionHandler(this, request)*/)
    }
/*
    private fun onExternalUrlOpened(url: String) {
        val request = Request("","")
        RouteParser.cancel()
        RouteParser.parseRoute(request, object: RouteParser.RouteParserListener {
            override fun onRouteParsed(route: Route) {
                try {
                    runOnUiThread {
                        if(currentFragment is HomepageFragment)
                            (currentFragment as HomepageFragment).stationPicker.fillNow(route.get()[0].from, route.get()[route.get().lastIndex].to)
                        else {
                            homepageFragment.stationPicker.fillNow(route.get()[0].from, route.get()[route.get().lastIndex].to)
                            (currentFragment as RoutesFragment).tempStationPickerData = homepageFragment.stationPicker
                        }
                    }
                } catch(err: UninitializedPropertyAccessException) {
                    (currentFragment as HomepageFragment).tempFrom = route.get()[0].from
                    (currentFragment as HomepageFragment).tempTo = route.get()[route.get().lastIndex].to
                }
                routeParserListener.onRouteParsed(route)
            }
        }, uncaughtExceptionHandler = RouteParserExceptionHandler(this, request), url = url)
    }
*/

    // ======= PERMISSIONS STUFF =======


    private val PERMISSION_READ_IMPORT = 0
    private val PERMISSION_WRITE_EXPORT = 1
    private val PERMISSION_WRITE_UPDATE = 2
    private val PERMISSION_WRITE_UPDATE_UA = 3

    private fun checkReadPermissionAndImport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_READ_IMPORT)
        } else
            beginImport()
    }

    private fun checkWritePermissionAndExport() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_EXPORT)
        } else
            beginExport()
    }

    private fun checkWritePermissionAndUpdate(userAsked: Boolean = false) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if(userAsked)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_UPDATE_UA)
            else
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_UPDATE)
        } else
            beginUpdate(userAsked)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_READ_IMPORT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    beginImport()
                else
                    Toast.makeText(this, "L'importation nécessite l'accès en lecture.", Toast.LENGTH_SHORT).show()
            }
            PERMISSION_WRITE_EXPORT -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    beginExport()
                else
                    Toast.makeText(this, "L'exportation nécessite l'accès en écriture.", Toast.LENGTH_SHORT).show()
            }
            PERMISSION_WRITE_UPDATE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    beginUpdate(userAsked=false)
                else
                    Toast.makeText(this, "La mise à jour nécessite l'accès en écriture.", Toast.LENGTH_SHORT).show()
            }
            PERMISSION_WRITE_UPDATE_UA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    beginUpdate(userAsked=true)
                else
                    Toast.makeText(this, "La mise à jour nécessite l'accès en écriture.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // ======= IMPORT/EXPORT =======


    private fun beginImport() {
        val intent = Intent(this, FileChooser::class.java)
        intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal)
        intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "csv")
        startActivityForResult(intent, MENU_ID_IMPORT)
    }

    private fun import(file: File) {
        val inputStream = FileInputStream(file)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder =  StringBuilder()

        while(true) {
            val str = bufferedReader.readLine()
            if(str == null)
                break
            else
                stringBuilder.append(str)
        }

        inputStream.close()

        val bookmarks = stringBuilder.toString().split(IMPORT_EXPORT_BOOKMARK_LINE_SEP)
        val imported = ArrayList<Bookmark>()
        for(i in 0 until bookmarks.lastIndex) {
            val bookmark = bookmarks[i]
            val infos = bookmark.split(IMPORT_EXPORT_BOOKMARK_SEP)
            imported.add(Bookmark(
                from=infos[0],
                to=infos[1],
                rank=infos[2].toInt(),
                fromName= if(infos[3]==IMPORT_EXPORT_REFINED_NONE) "" else infos[3],
                toName=if(infos[4]==IMPORT_EXPORT_REFINED_NONE) "" else infos[4]
            ))
        }

        for(b in imported)
            homepageFragment.addBookmark(b)

        Toast.makeText(this,
            "${imported.size} favori${if(imported.size >1) "s" else ""} importé${if(imported.size >1) "s" else ""}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun beginExport() {
        val intent = Intent(this, FolderChooser::class.java)
        intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal)
        startActivityForResult(intent, MENU_ID_EXPORT)
    }

    private fun export(file: File) {
        Thread {
            val toExport = BookmarkDatabase.getDatabase(this).bookmarkDao().all
            val csv = StringBuilder()
            for(item in toExport)
                csv
                    .append(item.from)
                    .append(IMPORT_EXPORT_BOOKMARK_SEP)
                    .append(item.to)
                    .append(IMPORT_EXPORT_BOOKMARK_SEP)
                    .append(item.rank)
                    .append(IMPORT_EXPORT_BOOKMARK_SEP)
                    .append(if(item.fromName.isEmpty()) IMPORT_EXPORT_REFINED_NONE else item.fromName)
                    .append(IMPORT_EXPORT_BOOKMARK_SEP)
                    .append(if(item.toName.isEmpty()) IMPORT_EXPORT_REFINED_NONE else item.toName)
                    .append(IMPORT_EXPORT_BOOKMARK_LINE_SEP)

            val finalFile = File(file, "TCLWatcher_bkp.csv")
            if(finalFile.exists()) {
                finalFile.delete()
            }
            finalFile.createNewFile()
            val writer = OutputStreamWriter(FileOutputStream(finalFile))
            writer.write(csv.toString())
            writer.close()

            runOnUiThread {
                Toast.makeText(this,
                    "${toExport.size} favori${if(toExport.size >1) "s" else ""} exporté${if(toExport.size >1) "s" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == MENU_ID_IMPORT && data != null) {
            if (resultCode == RESULT_OK)
                import(File(URI(data.data!!.toString())))
        } else if(requestCode == MENU_ID_EXPORT && data != null) {
            if (resultCode == RESULT_OK)
                export(File(URI(data.data!!.toString())))
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    // ======= UPDATE =======


    private fun updateIfWeekPassed() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val lastUpdate = prefs.getLong("SEEK_FOR_UPDATES", 7)
        if (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastUpdate) >= 7) {
            prefs.edit().putLong("SEEK_FOR_UPDATES", System.currentTimeMillis()).apply()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Pensez à vérifier les mises à jour de l'application.", Toast.LENGTH_SHORT).show()
                return
            }
            beginUpdate(userAsked=false)
        }
    }

    private fun beginUpdate(userAsked: Boolean) {
        UpdateTask(this, userAsked).execute()
    }

}
