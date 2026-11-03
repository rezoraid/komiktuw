package com.komikta.feature.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayInputStream
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
    private const val BANNER_DECODE_SIDE = 1600
    private const val BANNER_WIDTH = 1080
    private const val BANNER_MAX_BYTES = 420_000

    /**
     * Banner profil: potong tengah ke rasio 2:1, kecilkan maksimal 1080 px lebar, lalu kompres ke JPEG
     * di bawah batas server. Mengembalikan data URI. Berat; panggil dari thread latar belakang.
     */
    fun prepareBanner(context: Context, uri: Uri): String {
        val source = decode(context, uri, BANNER_DECODE_SIDE) ?: error("Foto tidak bisa dibuka")
        var cropW = source.width
        var cropH = cropW / 2
        if (cropH > source.height) { cropH = source.height; cropW = cropH * 2 }
        require(cropW >= 2 && cropH >= 1) { "Foto terlalu kecil" }
        val cropped = Bitmap.createBitmap(source, (source.width - cropW) / 2, (source.height - cropH) / 2, cropW, cropH)
        val outW = minOf(cropW, BANNER_WIDTH)
        val outH = (outW / 2).coerceAtLeast(1)
        val canvasBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(canvasBitmap).apply {
            drawColor(Color.WHITE)
            drawBitmap(Bitmap.createScaledBitmap(cropped, outW, outH, true), 0f, 0f, null)
        }
        if (cropped !== source) cropped.recycle()
        source.recycle()

        val output = ByteArrayOutputStream()
        var quality = 86
        do {
            output.reset()
            canvasBitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            quality -= 8
        } while (output.size() > BANNER_MAX_BYTES && quality >= 38)
        canvasBitmap.recycle()
        require(output.size() <= BANNER_MAX_BYTES + 20_000) { "Gambar terlalu besar. Pilih gambar lain." }
        return "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    fun prepare(context: Context, uri: Uri): PreparedAvatar {
        val source = decode(context, uri) ?: error("Foto tidak bisa dibuka")
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(source.width, source.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        } else source
        // JPEG tidak punya transparansi (PNG/WebP/GIF): gambar di atas latar putih supaya tidak jadi hitam.
        val resized = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply { drawColor(Color.WHITE); drawBitmap(scaled, 0f, 0f, null) }
        }
        if (scaled !== source) scaled.recycle()

        val output = ByteArrayOutputStream()
        var quality = 82
        do {
            output.reset()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            quality -= 8
        } while (output.size() > 260_000 && quality >= 42)
        resized.recycle()
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

    /**
     * Decode ke bitmap software berukuran kecil. ImageDecoder (API 28+) mendukung HEIC/AVIF/WebP dan
     * otomatis memutar sesuai EXIF; BitmapFactory jadi cadangan untuk Android 8 atau jika gagal.
     */
    private fun decode(context: Context, uri: Uri, maxSide: Int = MAX_SIDE): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val bmp = runCatching {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = maxOf(info.size.width, info.size.height)
                    if (longest > maxSide) {
                        val f = maxSide.toFloat() / longest
                        decoder.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
                    }
                }
            }.getOrNull()
            if (bmp != null) return bmp
        }
        return decodeLegacy(context, uri, maxSide)
    }

    private fun decodeLegacy(context: Context, uri: Uri, maxSide: Int): Bitmap? {
        val resolver = context.contentResolver
        // Baca sekali ke memori: sebagian provider (Google Foto, cloud) tidak bisa dibuka dua kali.
        val data = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            ?: return null
        if (data.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        val degrees = runCatching {
            when (ExifInterface(ByteArrayInputStream(data)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        if (degrees == 0f) return bmp
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== bmp) bmp.recycle()
        return rotated
    }
}
