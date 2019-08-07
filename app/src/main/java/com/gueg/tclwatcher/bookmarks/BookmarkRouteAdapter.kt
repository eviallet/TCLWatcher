package com.gueg.tclwatcher.bookmarks

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.gueg.tclwatcher.R
import com.gueg.tclwatcher.routes.Request
import com.gueg.tclwatcher.routes.Route
import com.gueg.tclwatcher.routes.RouteParser
import com.squareup.picasso.Picasso

class BookmarkRouteAdapter internal constructor(
    activity: Activity,
    bookmark: Bookmark,
    onTimesGot: (String, String) -> Unit,
    onFinished: ((String) -> Unit) ?= null,
    nextUrl : String = "")
    : RecyclerView.Adapter<BookmarkRouteAdapter.ViewHolder>() {

    private var loadedRoute: Route ?= null
    private val subroutes = ArrayList<Route.SubRoute>()

    init {
        val request = Request(bookmark.from, bookmark.to)
        if(bookmark.hasBeenRefined()) {
            request.refineFrom(bookmark.refinedFrom)
            request.refineTo(bookmark.refinedTo)
        }
        RouteParser.parseRoute(request, object: RouteParser.RouteParserListener {
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
        }, url= nextUrl)
    }


    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var left: ImageView = v.findViewById(R.id.row_bookmark_route_picleft)
        var right: ImageView = v.findViewById(R.id.row_bookmark_route_picright)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_bookmark_route, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subroute = subroutes[position]

        if(subroute is Route.TCL) {
            Picasso.get()
                .load(subroute.picLeft)
                .into(holder.left)
            Picasso.get()
                .load(subroute.picRight)
                .into(holder.right)
        }
    }

    override fun getItemCount() = subroutes.size

}