package com.gueg.tclwatcher.stations

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "station_database")
class Station(@PrimaryKey val name: String, val lon: Double, val lat: Double) {
    override fun toString(): String {
        return "$name = $lon : $lat"
    }

    override fun equals(other: Any?): Boolean {
        if(other is Station)
            return name == other.name
        return super.equals(other)
    }
}