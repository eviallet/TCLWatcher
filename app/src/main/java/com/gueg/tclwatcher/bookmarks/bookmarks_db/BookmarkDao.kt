package com.gueg.tclwatcher.bookmarks.bookmarks_db

import androidx.room.*
import com.gueg.tclwatcher.bookmarks.Bookmark

@Dao
interface BookmarkDao {
    @get:Query("SELECT * FROM bookmark_database ORDER BY rank")
    val all: List<Bookmark>

    @Insert
    fun insert(bookmark: Bookmark)

    @Insert
    fun insertAll(bookmarks: List<Bookmark>)

    @Delete
    fun delete(bookmark: Bookmark)
}
