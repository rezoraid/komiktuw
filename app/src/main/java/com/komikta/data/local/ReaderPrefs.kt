package com.komikta.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("reader_prefs", Context.MODE_PRIVATE)
    private val _autoNext = MutableStateFlow(sp.getBoolean("auto_next", true))
    val autoNext: StateFlow<Boolean> = _autoNext.asStateFlow()
    private val _floatingNav = MutableStateFlow(sp.getBoolean("floating_nav", false))
    val floatingNav: StateFlow<Boolean> = _floatingNav.asStateFlow()
    fun setFloatingNav(v: Boolean) { sp.edit().putBoolean("floating_nav", v).apply(); _floatingNav.value = v }
    fun setAutoNext(v: Boolean) { sp.edit().putBoolean("auto_next", v).apply(); _autoNext.value = v }
}
