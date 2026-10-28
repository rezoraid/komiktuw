package com.komikta.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.komikta.data.local.dao.BookmarkDao
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.local.entity.BookmarkEntity
import com.komikta.data.local.entity.ReadingHistoryEntity

@Database(
    entities = [
        ReadingHistoryEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KomiktaDatabase : RoomDatabase() {
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
