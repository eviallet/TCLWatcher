package com.gueg.tclwatcher.stations_db

import androidx.room.*
import com.gueg.tclwatcher.Station

@Dao
interface StationDao {
    @get:Query("SELECT * FROM station_database ORDER BY name")
    val all: List<Station>

    @Query("SELECT * FROM station_database WHERE name LIKE :name LIMIT 1")
    fun findByName(name: String): Station

    @Insert
    fun insertAll(stations: List<Station>)

    @Delete
    fun delete(station: Station)
}
