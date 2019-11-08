package com.gueg.tclwatcher.stations.stations_db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.gueg.tclwatcher.stations.Station

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
