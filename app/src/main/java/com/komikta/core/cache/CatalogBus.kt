package com.komikta.core.cache

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sinyal "katalog berubah" (mis. OWNER menyimpan perubahan komik atau gambar chapter).
 * Home, Explore, dan Detail yang sedang hidup mendengarkannya lalu memuat ulang dari server,
 * jadi perubahan langsung terlihat tanpa menutup aplikasi.
 */
object CatalogBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()
    fun changed() { _events.tryEmit(Unit) }
}
