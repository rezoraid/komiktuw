package com.komikta.feature.profile

import androidx.lifecycle.ViewModel
import com.komikta.data.local.ReaderPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: ReaderPrefs) : ViewModel() {
    val floatingNav: StateFlow<Boolean> = prefs.floatingNav
    fun setFloatingNav(value: Boolean) = prefs.setFloatingNav(value)
}
