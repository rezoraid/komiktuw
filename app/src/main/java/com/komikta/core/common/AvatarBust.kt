package com.komikta.core.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Foto profil sendiri di server beralamat tetap, jadi Coil terus memakai gambar lama dari cache.
 * Objek ini menyimpan alamat foto milik akun yang sedang masuk + nomor revisinya; [apply] menempelkan
 * revisi itu ke alamat yang sama di mana pun muncul (komentar, profil publik, aktivitas), sehingga
 * foto baru langsung dipakai di semua tempat. State Compose: tampilan ikut ter-update saat revisi berubah.
 */
object AvatarBust {
    private var base by mutableStateOf<String?>(null)
    private var rev by mutableLongStateOf(0L)

    fun set(picture: String?, revision: Long) {
        base = picture?.takeIf { it.isNotBlank() && !it.startsWith("data:") && !it.startsWith("file:") }?.substringBefore('?')
        rev = revision
    }

    fun apply(url: String): String {
        val b = base ?: return url
        if (rev <= 0L || url.startsWith("data:") || url.startsWith("file:")) return url
        return if (url.substringBefore('?') == b && "rev=" !in url) "$b?rev=$rev" else url
    }
}
