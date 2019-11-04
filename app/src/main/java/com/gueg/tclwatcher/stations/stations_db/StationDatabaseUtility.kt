package com.gueg.tclwatcher.stations.stations_db

import android.content.Context
import com.gueg.tclwatcher.StationDatabase

class StationDatabaseUtility {

    companion object {

        fun findDataByName(context: Context, name: String, func : (res: String) -> Unit) {
            Thread {
                try {
                    func("stop_point:tcl:SP:${StationDatabase.getDatabase(context).stationDao().findByName(name).data}")
                } catch (e: Exception) {
                    func("")
                }
            }.start()
        }
    }
}