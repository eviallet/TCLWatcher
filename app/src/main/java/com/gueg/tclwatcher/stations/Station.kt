package com.gueg.tclwatcher.stations

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "station_database")
data class Station(@PrimaryKey val name: String, val lon: Double, val lat: Double, val data: Int) {
    override fun toString(): String {
        return "$name = $lon : $lat ; data = $data"
    }

    override fun equals(other: Any?): Boolean {
        if(other is Station)
            return name == other.name
        return super.equals(other)
    }
}