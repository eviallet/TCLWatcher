package com.gueg.tclwatcher.routes

import java.net.URLEncoder
import java.util.*

class Request(
    var from:String, var to:String,
    private var day:Int=-1, private var month:Int=-1, private var year:Int=-1, private var hour:Int=-1, private var minute:Int=-1,
    private val timeMode: TimeMode = TimeMode.DEPART_AT
) {

    private var hasBeenRefined = false

    @Suppress("PrivatePropertyName")
    private val LINK = "http://m.tcl.fr/navitia/itineraire_mobile?ItinDepart=#DEP#&ItinArrivee=#ARR#&radioSens=#DEPARR#&radioOption=OptionArrivRapid&radioModes=OptionPrivilTous&selectedDate=#YEAR#%7C#MON#%7C#DAY#&DepartHeure=#HOUR#&DepartMinute=#MIN#&lancer_recherche_1=Rechercher"

    @Suppress("PrivatePropertyName")
    private val REFINED_LINK = "http://m.tcl.fr/navitia/itineraire_mobile?arretDepart=#DEP#&arretArrivee=#ARR#&radioSens=#DEPARR#&radioOption=OptionArrivRapid&radioModes=OptionPrivilTous&selectedDate=#YEAR#%7C#MON#%7C#DAY#&DepartHeure=#HOUR#&DepartMinute=#MIN#&validation_arrets=Valider"

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

    fun refineFrom(from: String) {
        this.from = from
        hasBeenRefined = true
    }
    fun refineTo(to: String) {
        this.to = to
        hasBeenRefined = true
    }

    override fun toString(): String =
        if(hasBeenRefined) refinedString()
        else regularString()

    private fun regularString() = LINK
        .replace("#DEP#", URLEncoder.encode(from, "UTF-8") + "+%28Arret%29")
        .replace("#ARR#", URLEncoder.encode(to, "UTF-8") + "+%28Arret%29")
        .replace("#DEPARR#", if(timeMode == TimeMode.DEPART_AT) "HorPartir" else "HorArriver")
        .replace("#YEAR#", year.toString())
        .replace("#MON#", month.toString())
        .replace("#DAY#", day.toString())
        .replace("#HOUR#", hour.toString())
        .replace("#MIN#", minute.toString())

    private fun refinedString() = REFINED_LINK
        .replace("#DEP#", URLEncoder.encode(from, "UTF-8"))
        .replace("#ARR#", URLEncoder.encode(to, "UTF-8"))
        .replace("#DEPARR#", if(timeMode == TimeMode.DEPART_AT) "HorPartir" else "HorArriver")
        .replace("#YEAR#", year.toString())
        .replace("#MON#", month.toString())
        .replace("#DAY#", day.toString())
        .replace("#HOUR#", hour.toString())
        .replace("#MIN#", minute.toString())



}