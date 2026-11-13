package com.komikta.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.komikta.core.notify.NotificationPrefs
import com.komikta.data.local.dao.BookmarkDao
import com.komikta.data.local.dao.ReadChapterDao
import com.komikta.data.local.dao.ReadingHistoryDao
import com.komikta.data.local.entity.BookmarkEntity
import com.komikta.data.local.entity.ReadingHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    bookmarkDao: BookmarkDao,
    private val historyDao: ReadingHistoryDao,
    private val readChapterDao: ReadChapterDao,
    private val notifications: NotificationPrefs
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = notifications.enabledFlow

    fun setNotifications(enabled: Boolean) = notifications.setEnabled(enabled)

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<ReadingHistoryEntity>> = historyDao.getRecentHistory(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeHistory(h: ReadingHistoryEntity) {
        viewModelScope.launch {
            historyDao.delete(h.id)
            readChapterDao.clear(h.id)
        }
    }
}
