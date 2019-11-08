package com.gueg.tclwatcher.routes

import org.osmdroid.util.GeoPoint

class Route (var from:String, var to:String, var departureTime:String, var arrivalTime:String, var totalDuration:String, var date:String,
             var prev:String, var next:String,
             var warning: String = "", var request: String = ""
) {

    private val subroutes = ArrayList<SubRoute>()

    fun add(subroute: SubRoute) {
        subroutes.add(subroute)
    }

    fun get(): ArrayList<SubRoute> {
        return subroutes
    }

    override fun toString(): String {
        var string = "\n"
        for(subroute in subroutes)
            string += subroute.toString() + '\n'
        return string
    }

    override fun equals(other: Any?) = toString() == other.toString()

    abstract class SubRoute(
        val from:String="", val fromDir:String="", val to:String="",
        val departAt:String="", val arriveAt:String="", val duration:String="",
        val pic:String= "", val coords:ArrayList<GeoPoint>? = null, val color: Int = 0, val debugInfo:String = "") {
        override fun toString(): String {
            return "$debugInfo $from $fromDir $to $departAt $arriveAt $duration"
        }

        fun lineName() : String = pic.substring(pic.lastIndexOf('/')+1).replace(".svg","")
    }


    class TCL(from:String, fromDir:String, to:String, departAt:String, arriveAt:String, duration:String, pic:String, coords: ArrayList<GeoPoint>, color: Int)
        : SubRoute(from, fromDir, to, departAt, arriveAt, duration, pic, coords, color)

    class Walk(duration:String)
        : SubRoute(duration=duration, debugInfo = "Walk : ")

    class Wait(duration:String)
        : SubRoute(duration=duration, debugInfo = "Wait : ")
}
