package com.gueg.tclwatcher.bookmarks

import androidx.room.Entity

@Entity(tableName = "bookmark_database", primaryKeys = ["from", "to"])
data class Bookmark(
    var from: String,
    var to: String,
    var rank: Int = -1,
    var refinedFrom: String = "",
    var refinedTo: String = ""
) {
    fun hasBeenRefined() = refinedFrom != "" || refinedTo != ""
}