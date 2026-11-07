package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

private val Placeholder = ColorPainter(Color(0xFF263441))

/**
 * Cover komik. Kalau URL kosong atau gambar gagal dimuat, yang tampil adalah sampul pengganti
 * (warna konsisten per judul + judulnya), bukan kotak kosong.
 */
@Composable
fun CoverImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    widthPx: Int = 300,
    heightPx: Int = 420,
    alignment: Alignment = Alignment.Center
) {
    val context = LocalContext.current
    val request = remember(url, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(url)
            .size(Size(widthPx, heightPx))
            .crossfade(120)
            .build()
    }
    var failed by remember(url) { mutableStateOf(url.isNullOrBlank()) }
    Box(modifier) {
        if (!failed) {
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                alignment = alignment,
                placeholder = Placeholder,
                error = Placeholder,
                onError = { failed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CoverFallback(contentDescription.orEmpty())
        }
    }
}

/** Warna sampul pengganti ditentukan judulnya, jadi komik yang sama selalu berwarna sama. */
internal fun fallbackHue(title: String): Float = (title.fold(7) { acc, c -> acc * 31 + c.code }.mod(360)).toFloat()

@Composable
private fun CoverFallback(title: String) {
    val hue = remember(title) { fallbackHue(title) }
    val top = remember(hue) { Color.hsv(hue, 0.35f, 0.40f) }
    val bottom = remember(hue) { Color.hsv(hue, 0.45f, 0.22f) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom))), contentAlignment = Alignment.Center) {
        Text(
            title, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(6.dp)
        )
    }
}
