package com.gueg.tclwatcher

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gueg.tclwatcher.stations_db.StationDao

@Database(entities = [Station::class], version = 1, exportSchema = false)
abstract class StationDatabase : RoomDatabase() {

    abstract fun stationDao(): StationDao

    companion object {

        private var INSTANCE: StationDatabase? = null

        fun getDatabase(context: Context): StationDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            StationDatabase::class.java, "station_database"
                        ).build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}