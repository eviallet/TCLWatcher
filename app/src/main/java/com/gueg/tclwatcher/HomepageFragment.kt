package com.gueg.tclwatcher

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.gueg.tclwatcher.bookmarks.Bookmark
import com.gueg.tclwatcher.bookmarks.BookmarkAdapter
import com.gueg.tclwatcher.bookmarks.bookmarks_db.BookmarkDatabase
import com.gueg.tclwatcher.map.MapActivity
import com.gueg.tclwatcher.routes.Request
import com.gueg.tclwatcher.routes.Route
import com.gueg.tclwatcher.routes.RouteParser
import com.gueg.tclwatcher.routes.RouteParserExceptionHandler
import com.gueg.tclwatcher.stations.Station
import com.gueg.tclwatcher.stations.StationPicker


class HomepageFragment : Fragment() {

    private lateinit var rootView: ViewGroup
    private lateinit var stationPicker: StationPicker

    private lateinit var bookmarkRecyclerView: RecyclerView
    private lateinit var bookmarkAdapter: BookmarkAdapter

    private lateinit var stationPickerListener: StationPicker.StationPickerListener
    private lateinit var stations: List<Station>
    var tempStationPickerData: StationPicker?= null
        set(value) {
            if(::stationPicker.isInitialized)
                stationPicker.initFrom(value!!)
            field = value
        }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_homepage, container, false) as ViewGroup

        stationPicker = rootView.findViewById(R.id.fragment_homepage_stationpicker)
        stationPicker.setStations(stations)
        stationPicker.listener = stationPickerListener
        if(tempStationPickerData != null)
            stationPicker.initFrom(tempStationPickerData!!)

        rootView.findViewById<ImageButton>(R.id.fragment_homepage_menu_map).setOnClickListener {
            activity!!.startActivity(Intent(activity, MapActivity::class.java))
        }

        rootView.findViewById<ImageButton>(R.id.fragment_homepage_menu_bookmarks).setOnClickListener {
            if(stationPicker.checkText()) {
                val from = stationPicker.from()
                val to = stationPicker.to()
                RouteParser.parseRoute(Request(from, to), object: RouteParser.RouteParserListener {
                    override fun onRouteParsed(route: Route) {
                        addBookmark(Bookmark(
                            from=stationPicker.from(),
                            to=stationPicker.to()
                        ))
                    }
                }, uncaughtExceptionHandler = RouteParserExceptionHandler(activity!! as MainActivity, Request(from, to)) { refined: Request ->
                    addBookmark(Bookmark(
                        from=stationPicker.from(),
                        to=stationPicker.to(),
                        refinedFrom=refined.from,
                        refinedTo=refined.to
                    ))
                })
            }
        }

        bookmarkRecyclerView = rootView.findViewById(R.id.fragment_homepage_recyclerview)
        bookmarkRecyclerView.setHasFixedSize(true)
        bookmarkRecyclerView.layoutManager = LinearLayoutManager(context)
        bookmarkAdapter = BookmarkAdapter(
            activity!!,
            bookmarkRecyclerView,
            object: BookmarkSelectedListener {
                override fun onBookmarkSelected(bookmark: Bookmark) {
                    stationPicker.fill(bookmark.from, bookmark.to)
                    if(bookmark.hasBeenRefined()) {
                        stationPicker.refinedFrom = bookmark.refinedFrom
                        stationPicker.refinedTo = bookmark.refinedTo
                    }
                }

                override fun onBookmarkDeleted(bookmark: Bookmark) {
                    askBookmarkDeletion(bookmark)
                }
            }
        )
        bookmarkRecyclerView.adapter = bookmarkAdapter
        bookmarkRecyclerView.addItemDecoration(OrientedItemDecoration(30))

        bookmarkRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER

        rootView.findViewById<ImageButton>(R.id.fragment_homepage_menu_refresh).setOnClickListener {
            bookmarkAdapter.refresh()
        }

        return rootView
    }

    private fun addBookmark(bookmark: Bookmark) {
        Thread {
            try {
                BookmarkDatabase.getDatabase(context!!).bookmarkDao().insert(bookmark)
                activity!!.runOnUiThread {
                    bookmarkAdapter.add(bookmark)
                    bookmarkAdapter.notifyDataSetChanged()
                }
            } catch(err: SQLiteConstraintException) {
                (activity!! as MainActivity).setError("Ce favori existe déjà.")
            }
        }.start()
    }

    fun onError() {
        stationPicker.setLoading(false)
    }

    fun setStations(stations: List<Station>) {
        this.stations = stations
    }

    fun setStationPickerListener(stationPickerListener: StationPicker.StationPickerListener) {
        this.stationPickerListener = stationPickerListener
    }

    private fun askBookmarkDeletion(bookmark: Bookmark) {
        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    dialog.dismiss()
                    Thread {
                        BookmarkDatabase.getDatabase(context!!).bookmarkDao().delete(bookmark)
                    }.start()
                    bookmarkAdapter.remove(bookmark)
                    bookmarkAdapter.notifyDataSetChanged()
                }

                DialogInterface.BUTTON_NEGATIVE -> { dialog.dismiss() }
            }
        }

        val builder = AlertDialog.Builder(context!!)
        builder.setMessage("Supprimer le favori ?")
            .setPositiveButton("Oui", dialogClickListener)
            .setNegativeButton("Non", dialogClickListener)
            .show()
    }

    interface BookmarkSelectedListener {
        fun onBookmarkSelected(bookmark: Bookmark)
        fun onBookmarkDeleted(bookmark: Bookmark)
    }

}