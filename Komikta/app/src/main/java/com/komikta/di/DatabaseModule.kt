package com.komikta.di

import android.content.Context
import androidx.room.Room
import com.komikta.core.database.KomiktaDatabase
import com.komikta.data.local.dao.BookmarkDao
import com.komikta.data.local.dao.ReadingHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KomiktaDatabase {
        return Room.databaseBuilder(
            context,
            KomiktaDatabase::class.java,
            "komikta.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReadingHistoryDao(db: KomiktaDatabase): ReadingHistoryDao = db.readingHistoryDao()

    @Provides
    fun provideBookmarkDao(db: KomiktaDatabase): BookmarkDao = db.bookmarkDao()
}
