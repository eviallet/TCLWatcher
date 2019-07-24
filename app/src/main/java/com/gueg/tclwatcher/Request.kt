package com.gueg.tclwatcher

import java.net.URLEncoder
import java.util.*

class Request(val from:String, val to:String,
              private var day:Int=-1, private var month:Int=-1, private var year:Int=-1, private var hour:Int=-1, private var minute:Int=-1,
              private val timeMode: TimeMode=TimeMode.DEPART_AT) {

    @Suppress("PrivatePropertyName")
    private val LINK = "http://m.tcl.fr/navitia/itineraire_mobile?ItinDepart=#DEP#&ItinArrivee=#ARR#&radioOption=OptionArrivRapid&radioModes=OptionPrivilTous&lancer_recherche_1=Rechercher&selectedDate=#YEAR#%7C#MON#%7C#DAY#"

    enum class TimeMode {
        DEPART_AT,
        ARRIVE_AT
    }

    init {
        if(day==-1) day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        if(month==-1) month = Calendar.getInstance().get(Calendar.MONTH)+1
        if(year==-1) year = Calendar.getInstance().get(Calendar.YEAR)
        if(hour==-1) hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if(minute==-1) minute = Calendar.getInstance().get(Calendar.MINUTE)
    }

    override fun toString(): String {
        var link = LINK
            .replace("#DEP#", URLEncoder.encode(from, "UTF-8") + "%28Arret%29")
            .replace("#ARR#", URLEncoder.encode(to, "UTF-8") + "%28Arret%29")

        if(timeMode == TimeMode.DEPART_AT)
            link += "&startDate=#MON#%2F#DAY%2F#YEAR#&DepartHeure=#HOUR#&DepartMinute=#MIN#"
        else if(timeMode == TimeMode.ARRIVE_AT)
            link += "&endDate=#MON#%2F#DAY%2F#YEAR#&ArriveHeure=#HOUR#&ArriveMinute=#MIN#"

        link = link.replace("#YEAR#",year.toString())
            .replace("#MON#",month.toString())
            .replace("#DAY#",day.toString())
            .replace("#HOUR#",hour.toString())
            .replace("#MIN#",minute.toString())

        return link
    }

}