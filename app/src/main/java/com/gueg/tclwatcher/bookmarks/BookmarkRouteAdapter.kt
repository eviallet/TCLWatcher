package com.gueg.tclwatcher.bookmarks

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gueg.tclwatcher.ImageLoader
import com.gueg.tclwatcher.ImageViewWithCache
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.routes.Route
import com.gueg.tclwatcher.routes.RouteParser
import com.gueg.tclwatcher.routes.RouteRequest

class BookmarkRouteAdapter internal constructor(
    private val activity: Activity,
    bookmark: Bookmark,
    onTimesGot: (String, String) -> Unit,
    onFinished: ((String) -> Unit) ?= null,
    next : String = ""
) : RecyclerView.Adapter<BookmarkRouteAdapter.ViewHolder>() {

    private var loadedRoute: Route ?= null
    private val subroutes = ArrayList<Route.SubRoute>()

    init {
        val request =
            if(next.isNotEmpty())
                RouteRequest(bookmark.from, bookmark.to, datetime= next)
            else
                RouteRequest(bookmark.from, bookmark.to)

        RouteParser.parseRoute(activity, request, object: RouteParser.RouteParserListener {
            override fun onRouteParsed(route: Route) {
                loadedRoute = route
                for(subroute in route.get())
                    if(subroute is Route.TCL)
                        subroutes.add(subroute)
                activity.runOnUiThread { notifyDataSetChanged() }

                if(onFinished != null)
                    onFinished(route.next)
                onTimesGot(route.departureTime, route.arrivalTime)
            }
        })
    }


    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var pic: ImageViewWithCache = v.findViewById(R.id.row_bookmark_route_pic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_bookmark_route, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subroute = subroutes[position]

        if(subroute is Route.TCL)
            ImageLoader.load(activity, subroute.pic, holder.pic)
    }

    override fun getItemCount() = subroutes.size

}