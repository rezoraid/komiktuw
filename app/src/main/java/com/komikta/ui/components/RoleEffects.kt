package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.theme.Mono
import com.komikta.ui.theme.Poppins
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Palet role: Owner merah api, Moderator hijau zamrud (penjaga), Premium biru berlian. */
internal object RoleFx {
    val OwnerCore = Color(0xFFE11D2E)
    val OwnerHot = Color(0xFFFF4B3E)
    val OwnerEmber = Color(0xFFFF9A3C)
    val OwnerGold = Color(0xFFFFE08A)
    val OwnerDeep = Color(0xFF7A0A14)
    val ModCore = Color(0xFF1FB878)
    val ModLight = Color(0xFF7CF2B8)
    val ModDeep = Color(0xFF0E7A52)
    val PremCore = Color(0xFF2F8CFF)
    val PremLight = Color(0xFF9AD2FF)
    val PremIce = Color(0xFFEAF8FF)
    val PremDeep = Color(0xFF1B5FCC)

    fun light(role: String): Color = when (role) {
        "OWNER" -> OwnerEmber
        "MODERATOR" -> ModLight
        else -> PremLight
    }
}

internal object RoleClock {
    val ms = mutableFloatStateOf(0f)
    val users = mutableIntStateOf(0)
}

@Composable
internal fun RoleClockDriver() {
    val active = RoleClock.users.intValue > 0
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) androidx.compose.runtime.withFrameNanos { RoleClock.ms.floatValue = ((it - start) / 1_000_000L % 3_600_000L).toFloat() }
    }
}

@Composable
private fun UseRoleClock() = DisposableEffect(Unit) {
    RoleClock.users.intValue++
    onDispose { RoleClock.users.intValue-- }
}

private fun phaseOf(millis: Int): Float = (RoleClock.ms.floatValue % millis) / millis

/**
 * Cahaya yang "bernapas" tidak beraturan: gabungan tiga gelombang dengan periode tidak kelipatan,
 * jadi naik-turunnya terasa alami (seperti nyala api / napas), bukan denyut metronom. Hasil 0..1.
 */
private fun organic(periodMs: Int, seed: Float = 0f): Float {
    val t = RoleClock.ms.floatValue / periodMs * 6.2831855f
    val n = 0.6f * sin(t + seed) + 0.3f * sin(t * 2.71f + seed * 1.7f) + 0.1f * sin(t * 6.13f + seed * 3.1f)
    return 0.5f + 0.5f * n
}

private fun smooth(x: Float): Float = x * x * (3f - 2f * x)

/** Kilatan singkat (sekitar seperlima periode) lalu padam lama, seperti pantulan cahaya di sisi berlian. Hasil 0..1. */
private fun flash(periodMs: Int, seed: Float): Float {
    val p = (RoleClock.ms.floatValue / periodMs + seed) % 1f
    return if (p < 0.22f) sin(p / 0.22f * 3.1415927f) else 0f
}

/** Bintang empat sudut kecil: dua garis bersilang + titik putih di tengah. */
private fun DrawScope.twinkle(center: Offset, r: Float, a: Float, color: Color) {
    if (a <= 0.01f) return
    val w = (r * 0.2f).coerceAtLeast(1f)
    val c = color.copy(alpha = a)
    drawLine(c, Offset(center.x - r, center.y), Offset(center.x + r, center.y), w, StrokeCap.Round)
    drawLine(c, Offset(center.x, center.y - r), Offset(center.x, center.y + r), w, StrokeCap.Round)
    drawCircle(Color.White.copy(alpha = a), r * 0.3f, center)
}

private fun DrawScope.pillGlow(color: Color, spread: Float, peak: Float) {
    // Banyak lapisan tipis dengan jatuh alpha kuadratik: tepi menyatu halus, tidak ada garis keras.
    val steps = 26
    var norm = 0f
    for (i in 0 until steps) norm += (1f - i / steps.toFloat()).let { it * it }
    for (i in 0 until steps) {
        val k = 1f - i / steps.toFloat()
        val e = spread * k
        drawRoundRect(
            color = color.copy(alpha = (peak * k * k / norm * 3.2f).coerceAtMost(1f)),
            topLeft = Offset(-e, -e),
            size = Size(size.width + e * 2, size.height + e * 2),
            cornerRadius = CornerRadius((size.height + e * 2) / 2f)
        )
    }
}

private fun Modifier.bleed(x: Dp, y: Dp): Modifier = layout { m, c ->
    val dx = x.roundToPx()
    val dy = y.roundToPx()
    val p = m.measure(c)
    layout((p.width - 2 * dx).coerceAtLeast(0), (p.height - 2 * dy).coerceAtLeast(0)) { p.place(-dx, -dy) }
}

/** Kapsul role: Premium berkilau putih, Moderator berdenyut hijau, Owner merah menyala. Cahaya lembut tanpa tepi keras. */
@Composable
internal fun FxRoleBadge(role: String, tint: Color, modifier: Modifier = Modifier, large: Boolean = false) {
    UseRoleClock()
    val owner = role == "OWNER"
    val light = RoleFx.light(role)
    val sweepMs = if (owner) 1700 else 2800
    val pulseMs = if (owner) 850 else 1500
    val icon = when (role) {
        "OWNER" -> AppIcons.crown
        "MODERATOR" -> AppIcons.shield
        else -> AppIcons.diamond
    }
    val fill = remember(role, tint) {
        Brush.horizontalGradient(
            when (role) {
                "OWNER" -> listOf(RoleFx.OwnerDeep, RoleFx.OwnerCore, RoleFx.OwnerHot)
                "MODERATOR" -> listOf(tint.copy(alpha = 0.38f), tint.copy(alpha = 0.18f))
                else -> listOf(RoleFx.PremDeep.copy(alpha = 0.6f), RoleFx.PremCore.copy(alpha = 0.38f), RoleFx.PremLight.copy(alpha = 0.3f))
            }
        )
    }
    val rim = remember(role, tint) { Brush.horizontalGradient(if (role == "PREMIUM") listOf(RoleFx.PremIce, tint, RoleFx.PremLight, tint, RoleFx.PremIce) else listOf(light, tint, light)) }
    val strength = if (owner) 0.42f else 0.30f
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { alpha = 0.55f + 0.45f * organic(pulseMs * 2, 1.3f); compositingStrategy = CompositingStrategy.ModulateAlpha }
                .drawBehind { pillGlow(tint, (if (large) 9 else 7).dp.toPx(), if (owner) 0.6f else 0.34f) }
        )
        Box(Modifier.then(if (large) Modifier.height(34.dp) else Modifier).clip(CircleShape).background(fill).border(1.dp, rim, CircleShape)) {
            Row(
                Modifier.then(if (large) Modifier.height(34.dp).padding(start = 10.dp, end = 16.dp) else Modifier.padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = if (owner) RoleFx.OwnerGold else light, modifier = Modifier.size(if (large) 16.dp else 13.dp))
                Text(roleName(role), fontSize = if (large) 14.sp else 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, modifier = Modifier.padding(start = if (large) 6.dp else 4.dp))
            }
            Box(
                Modifier.matchParentSize()
                    .graphicsLayer {
                        // Kilau lewat sekali lalu jeda sebentar, dengan gerak melambat di ujung (bukan konstan terus-menerus).
                        val p = smooth((phaseOf(sweepMs) / 0.6f).coerceAtMost(1f))
                        translationX = (p * 2.2f - 0.6f) * size.width
                    }
                    .drawBehind {
                        val w = size.width * 0.45f
                        drawRect(
                            brush = Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = strength * 0.5f), Color.White.copy(alpha = strength), Color.White.copy(alpha = strength * 0.5f), Color.Transparent), Offset.Zero, Offset(w, size.height)),
                            size = Size(w, size.height)
                        )
                    }
            )
            // Kilatan bintang: Premium satu (putih-biru, seperti berlian), Owner dua (emas) supaya tetap paling istimewa.
            if (owner || role == "PREMIUM") Box(
                Modifier.matchParentSize().drawBehind {
                    val r = (if (large) 4.2f else 3.2f).dp.toPx()
                    if (owner) {
                        twinkle(Offset(size.width * 0.2f, size.height * 0.3f), r, flash(2300, 0.1f), RoleFx.OwnerGold)
                        twinkle(Offset(size.width * 0.84f, size.height * 0.7f), r * 0.85f, flash(2300, 0.58f), RoleFx.OwnerGold)
                    } else twinkle(Offset(size.width * 0.86f, size.height * 0.3f), r, flash(3200, 0.15f), RoleFx.PremIce)
                }
            )
        }
    }
}

/**
 * Foto profil dengan bingkai khusus role. Premium: cincin biru berlian dengan kilatan bintang. Moderator: cincin hijau berdenyut.
 * Owner: cincin api merah yang berputar, cahaya berdenyut, dan percikan bara untuk ukuran besar (halaman profil).
 */
@Composable
fun RoleAvatar(url: String?, name: String, size: Dp, role: String, modifier: Modifier = Modifier) {
    val tint = roleTint(role)
    if (tint == null) {
        Avatar(url, name, size, modifier)
        return
    }
    UseRoleClock()
    val owner = role == "OWNER"
    val big = size >= 80.dp
    val ring = if (big) 4.dp else if (owner) 2.5.dp else 2.dp
    val gap = if (big) 3.dp else 2.dp
    val halo = if (big) 18.dp else 0.dp
    val total = size + (ring + gap) * 2 + halo * 2
    val spinMs = if (owner) 5200 else 16000
    val pulseMs = if (owner) 950 else 1800
    val colors = when (role) {
        "OWNER" -> listOf(RoleFx.OwnerHot, RoleFx.OwnerEmber, RoleFx.OwnerGold, RoleFx.OwnerEmber, RoleFx.OwnerCore, RoleFx.OwnerDeep, RoleFx.OwnerHot)
        "MODERATOR" -> listOf(RoleFx.ModCore, RoleFx.ModLight, RoleFx.ModCore, RoleFx.ModDeep, RoleFx.ModCore)
        else -> listOf(RoleFx.PremCore, RoleFx.PremLight, RoleFx.PremIce, RoleFx.PremLight, RoleFx.PremCore, RoleFx.PremDeep, RoleFx.PremCore)
    }
    Box(modifier.size(total), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { alpha = 0.6f + 0.4f * organic(pulseMs * 2, 0.7f); compositingStrategy = CompositingStrategy.ModulateAlpha }
                .drawWithCache {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val outer = this.size.minDimension / 2f - halo.toPx()
                    val glowR = outer + if (big) halo.toPx() else 4.dp.toPx()
                    val a = if (owner) 0.65f else 0.38f
                    val f = (outer / glowR).coerceIn(0.2f, 0.95f)
                    // Jatuh cahaya berlapis (bukan linear dua warna) supaya memudar lembut ke tepi, seperti cahaya sungguhan.
                    val glow = Brush.radialGradient(
                        0f to tint.copy(alpha = a * 0.85f),
                        f to tint.copy(alpha = a * 0.6f),
                        f + (1f - f) * 0.35f to tint.copy(alpha = a * 0.2f),
                        f + (1f - f) * 0.7f to tint.copy(alpha = a * 0.05f),
                        1f to Color.Transparent,
                        center = c, radius = glowR
                    )
                    onDrawBehind { drawCircle(glow, glowR, c) }
                }
        )
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { rotationZ = phaseOf(spinMs) * 360f }
                .drawWithCache {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val outer = this.size.minDimension / 2f - halo.toPx()
                    val sweep = Brush.sweepGradient(colors, c)
                    onDrawBehind { drawCircle(sweep, outer - ring.toPx() / 2f, c, style = Stroke(ring.toPx())) }
                }
        )
        // Owner: cincin emas tipis berputar berlawanan arah (khusus ukuran besar).
        if (owner && big) Box(
            Modifier.matchParentSize()
                .graphicsLayer { rotationZ = -phaseOf(9000) * 360f }
                .drawWithCache {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val outer = this.size.minDimension / 2f - halo.toPx()
                    val sweep = Brush.sweepGradient(listOf(Color.Transparent, RoleFx.OwnerGold, Color.Transparent, Color.Transparent), c)
                    onDrawBehind { drawCircle(sweep, outer + 6.dp.toPx(), c, style = Stroke(1.4.dp.toPx())) }
                }
        )
        // Owner: percikan bara naik (banyak di ukuran besar, sedikit di ukuran kecil).
        if (owner) Box(
            Modifier.matchParentSize().graphicsLayer { }.drawBehind {
                val c = center
                val outer = this.size.minDimension / 2f - halo.toPx()
                val t = phaseOf(3800)
                val count = if (big) 16 else 6
                val rise = if (big) halo.toPx() * 1.6f else 7.dp.toPx()
                val scale = if (big) 1f else 0.7f
                for (i in 0 until count) {
                    val seed = i * 0.61803f
                    val p = (t + seed) % 1f
                    val ang = seed * 6.2831f * 3f
                    val r = (1.1f + (i % 3) * 0.7f).dp.toPx() * scale * (1f - p * 0.5f)
                    val x = (c.x + cos(ang) * outer * 1.02f + sin(p * 12.566f + i) * 5.dp.toPx() * scale).coerceIn(r, this.size.width - r)
                    val y = (c.y + sin(ang) * outer * 0.95f - p * rise).coerceIn(r, this.size.height - r)
                    drawCircle((if (i % 2 == 0) RoleFx.OwnerEmber else RoleFx.OwnerHot).copy(alpha = sin(p * 3.1415927f) * 0.9f), r, Offset(x, y))
                }
            }
        )
        // Premium: kilatan bintang di tepi cincin, singkat dan bergantian seperti pantulan di sisi berlian.
        if (role == "PREMIUM") Box(
            Modifier.matchParentSize().drawBehind {
                val c = center
                val outer = this.size.minDimension / 2f - halo.toPx()
                val r = (if (big) 6f else 3f).dp.toPx()
                val angles = if (big) floatArrayOf(-0.9f, 0.7f, 2.3f, 3.9f) else floatArrayOf(-0.8f, 2.4f)
                angles.forEachIndexed { i, ang ->
                    val rad = outer - r * 0.7f
                    twinkle(Offset(c.x + cos(ang) * rad, c.y + sin(ang) * rad), r, flash(2900 + i * 430, i * 0.27f), RoleFx.PremIce)
                }
            }
        )
        Avatar(url, name, size)
    }
}

/** Nama dengan warna role: gradien bergerak + sedikit cahaya. Pengguna biasa tetap putih. */
@Composable
fun RoleName(
    text: String,
    role: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.SemiBold,
    maxLines: Int = 1
) {
    val tint = roleTint(role)
    if (tint == null) {
        Text(text, fontSize = fontSize, fontWeight = fontWeight, color = Mono.Text, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
        return
    }
    UseRoleClock()
    val owner = role == "OWNER"
    val periodMs = if (owner) 2200 else 3600
    val brush = remember(role) {
        val colors = when (role) {
            "OWNER" -> listOf(RoleFx.OwnerHot, RoleFx.OwnerGold, RoleFx.OwnerHot, RoleFx.OwnerCore)
            "MODERATOR" -> listOf(RoleFx.ModLight, RoleFx.ModCore, RoleFx.ModLight)
            else -> listOf(RoleFx.PremLight, RoleFx.PremIce, RoleFx.PremCore, RoleFx.PremLight)
        }
        Brush.linearGradient(colors, Offset.Zero, Offset(260f, 0f), TileMode.Mirror)
    }
    Text(
        text,
        style = TextStyle(
            fontFamily = Poppins,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = fontWeight,
            shadow = Shadow(color = tint.copy(alpha = if (owner) 0.7f else 0.45f), offset = Offset.Zero, blurRadius = if (owner) 12f else 7f)
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .bleed(6.dp, 3.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val dx = phaseOf(periodMs) * 520f
                translate(left = dx) { drawRect(brush, Offset(-dx, 0f), size, blendMode = BlendMode.SrcIn) }
            }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

/** Cahaya latar header profil: Owner paling kuat (merah menyala), role lain lebih lembut. Selalu memudar habis di tepi header. */
@Composable
fun RoleAura(role: String?, modifier: Modifier = Modifier) {
    val tint = role?.let { roleTint(it) } ?: return
    UseRoleClock()
    val owner = role == "OWNER"
    val pulseMs = if (owner) 1400 else 2200
    Box(
        modifier.clipToBounds()
            .graphicsLayer { alpha = 0.65f + 0.35f * organic(pulseMs * 2, 2.1f); compositingStrategy = CompositingStrategy.ModulateAlpha }
            .drawWithCache {
                val c = Offset(size.width / 2f, size.height * 0.46f)
                val r = min(size.height * 0.46f, size.width * 0.55f)
                val a = if (owner) 0.42f else 0.24f
                val radial = Brush.radialGradient(
                    0f to tint.copy(alpha = a),
                    0.5f to tint.copy(alpha = a * 0.4f),
                    0.8f to tint.copy(alpha = a * 0.1f),
                    1f to Color.Transparent,
                    center = c, radius = r
                )
                val vertical = if (owner) Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.3f to RoleFx.OwnerDeep.copy(alpha = 0.5f),
                    0.7f to RoleFx.OwnerDeep.copy(alpha = 0.22f),
                    1f to Color.Transparent
                ) else null
                onDrawBehind {
                    if (vertical != null) drawRect(vertical)
                    drawCircle(radial, r, c)
                }
            }
    )
}
