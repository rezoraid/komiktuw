package com.komikta.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

internal class PreparedAvatar(
    /** Data URI JPEG yang dikirim ke server. */
    val dataUri: String,
    /** File lokal untuk tampilan instan sebelum upload selesai. */
    val previewUrl: String
)

/** Decode + kecilkan + kompres foto profil. Berat, jadi hanya dipanggil dari thread latar belakang. */
internal object AvatarProcessor {
    private const val MAX_SIDE = 640
    private const val PREVIEW_PREFIX = "avatar_preview_"

    fun prepare(context: Context, uri: Uri): PreparedAvatar {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("Foto tidak bisa dibuka")
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Foto tidak bisa dibuka" }

        // Decode langsung dalam ukuran kecil: jauh lebih cepat dan hemat memori daripada decode penuh.
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val source = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Foto tidak bisa dibuka")

        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(source.width, source.height))
        val resized = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        } else source

        val output = ByteArrayOutputStream()
        var quality = 82
        do {
            output.reset()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            quality -= 8
        } while (output.size() > 260_000 && quality >= 42)
        if (resized !== source) resized.recycle()
        source.recycle()
        require(output.size() <= 280_000) { "Foto terlalu besar. Pilih foto lain." }

        val bytes = output.toByteArray()
        context.cacheDir.listFiles { f -> f.name.startsWith(PREVIEW_PREFIX) }?.forEach { it.delete() }
        val file = File(context.cacheDir, "$PREVIEW_PREFIX${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        return PreparedAvatar(
            dataUri = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP),
            previewUrl = Uri.fromFile(file).toString()
        )
    }
}
