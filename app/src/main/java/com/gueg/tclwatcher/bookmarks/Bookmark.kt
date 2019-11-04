package com.gueg.tclwatcher.bookmarks

import androidx.room.Entity

@Entity(tableName = "bookmark_database", primaryKeys = ["from", "to"])
data class Bookmark(
    var from: String,
    var to: String,
    var rank: Int = -1,
    var fromName: String,
    var toName: String
) {
    override fun toString() = "$from -> $to\n\trank = $rank\n\tfromName = $fromName\n\ttoName = $toName"
}
