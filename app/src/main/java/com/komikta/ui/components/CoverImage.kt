package com.komikta.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

private val Placeholder = ColorPainter(Color(0xFF171717))

/**
 * Cover image yang dioptimasi untuk daftar / grid.
 * - Ukuran decode disesuaikan dengan kebutuhan tampilan (bukan full-res).
 * - Tanpa crossfade di list agar scroll lebih mulus.
 * - Cache key stabil agar hit memory/disk tinggi.
 */
@Composable
fun CoverImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    /** Target lebar decode dalam px. Default ~2x grid card biasa. */
    widthPx: Int = 0,
    heightPx: Int = 0,
    /** Atau pakai Dp agar otomatis dihitung dari density. */
    targetWidth: Dp = 0.dp,
    targetHeight: Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop,
    /** Crossfade hanya untuk hero/detail besar; list sebaiknya false. */
    crossfade: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val (w, h) = remember(widthPx, heightPx, targetWidth, targetHeight, density) {
        val dw = when {
            widthPx > 0 -> widthPx
            targetWidth > 0.dp -> with(density) { targetWidth.roundToPx() }
            else -> 280
        }
        val dh = when {
            heightPx > 0 -> heightPx
            targetHeight > 0.dp -> with(density) { targetHeight.roundToPx() }
            else -> (dw * 1.47f).toInt() // ~0.68 aspect
        }
        dw to dh
    }

    val request = remember(url, w, h, crossfade) {
        ImageRequest.Builder(context)
            .data(url)
            .size(Size(w, h))
            .memoryCacheKey(url?.let { "cover:${it.takeLast(48)}:${w}x${h}" })
            .diskCacheKey(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(if (crossfade) 180 else 0)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        placeholder = Placeholder,
        error = Placeholder,
        modifier = modifier
    )
}
