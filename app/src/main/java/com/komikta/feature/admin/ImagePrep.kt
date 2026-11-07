package com.komikta.feature.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Menyiapkan gambar pilihan OWNER untuk diunggah. Gambar yang sudah kecil dikirim apa adanya (tanpa
 * encode ulang, jadi cepat dan tanpa risiko kehabisan memori); gambar besar dikecilkan dulu.
 */
object ImagePrep {
    class Prepared(val bytes: ByteArray, val mime: String, val name: String)

    private const val MAX_RAW_BYTES = 4 * 1024 * 1024
    private const val MAX_WIDTH = 1600
    private const val MAX_PIXELS = 12_000_000L

    suspend fun read(context: Context, uri: Uri): Prepared? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
        val raw = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            ?: return@withContext null
        if (raw.isEmpty()) return@withContext null
        if (raw.size <= MAX_RAW_BYTES) return@withContext Prepared(raw, mime, fileName(mime))

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        while (bounds.outWidth / sample > MAX_WIDTH ||
            (bounds.outWidth / sample).toLong() * (bounds.outHeight / sample) > MAX_PIXELS
        ) sample *= 2
        val bitmap = runCatching {
            BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull() ?: return@withContext null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        bitmap.recycle()
        Prepared(out.toByteArray(), "image/jpeg", fileName("image/jpeg"))
    }

    private fun fileName(mime: String): String {
        val ext = when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        return "upload-${System.currentTimeMillis()}.$ext"
    }
}
