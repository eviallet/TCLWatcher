package com.gueg.tclwatcher.bookmarks.bookmarks_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gueg.tclwatcher.bookmarks.Bookmark

@Database(entities = [Bookmark::class], version = 1, exportSchema = false)
abstract class BookmarkDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao

    companion object {

        private var INSTANCE: BookmarkDatabase? = null

        fun getDatabase(context: Context): BookmarkDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            BookmarkDatabase::class.java, "bookmark_database"
                        ).build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}