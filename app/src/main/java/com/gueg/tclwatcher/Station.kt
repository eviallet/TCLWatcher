package com.gueg.tclwatcher

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "station_database")
class Station(@PrimaryKey val name: String, val coordinatesX: Double, val coordinatesY: Double) {
    override fun toString(): String {
        return "$name = $coordinatesX : $coordinatesY"
    }

    override fun equals(other: Any?): Boolean {
        if(other is Station)
            return name == other.name
        return super.equals(other)
    }
}