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

/** Palet role: Owner merah api, Moderator hijau zamrud (penjaga), Premium putih perak. */
internal object RoleFx {
    val OwnerCore = Color(0xFFE11D2E)
    val OwnerHot = Color(0xFFFF4B3E)
    val OwnerEmber = Color(0xFFFF9A3C)
    val OwnerGold = Color(0xFFFFE08A)
    val OwnerDeep = Color(0xFF7A0A14)
    val ModCore = Color(0xFF1FB878)
    val ModLight = Color(0xFF7CF2B8)
    val ModDeep = Color(0xFF0E7A52)
    val PremCore = Color(0xFFE6E6EA)
    val PremLight = Color.White

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

private fun pulseOf(millis: Int): Float {
    val p = (RoleClock.ms.floatValue % (2f * millis)) / (2f * millis)
    return 0.5f - 0.5f * cos(6.2831855f * p)
}

private fun DrawScope.pillGlow(color: Color, spread: Float, peak: Float) {
    val steps = 14
    for (i in 0 until steps) {
        val e = spread * (1f - i / steps.toFloat())
        drawRoundRect(
            color = color.copy(alpha = peak / steps * 0.9f),
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
        Brush.horizontalGradient(if (owner) listOf(RoleFx.OwnerDeep, RoleFx.OwnerCore, RoleFx.OwnerHot) else listOf(tint.copy(alpha = 0.38f), tint.copy(alpha = 0.18f)))
    }
    val rim = remember(role, tint) { Brush.horizontalGradient(listOf(light, tint, light)) }
    val strength = if (owner) 0.6f else 0.45f
    Box(modifier) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { alpha = 0.5f + 0.5f * pulseOf(pulseMs); compositingStrategy = CompositingStrategy.ModulateAlpha }
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
                    .graphicsLayer { translationX = (phaseOf(sweepMs) * 2.2f - 0.6f) * size.width }
                    .drawBehind {
                        val w = size.width * 0.45f
                        drawRect(
                            brush = Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent), Offset.Zero, Offset(w, size.height)),
                            size = Size(w, size.height)
                        )
                    }
            )
        }
    }
}

/**
 * Foto profil dengan bingkai khusus role. Premium: cincin putih berkilau. Moderator: cincin hijau berdenyut.
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
    val spinMs = if (owner) 2400 else 7000
    val pulseMs = if (owner) 950 else 1800
    val colors = when (role) {
        "OWNER" -> listOf(RoleFx.OwnerHot, RoleFx.OwnerEmber, RoleFx.OwnerGold, RoleFx.OwnerEmber, RoleFx.OwnerCore, RoleFx.OwnerDeep, RoleFx.OwnerHot)
        "MODERATOR" -> listOf(RoleFx.ModCore, RoleFx.ModLight, RoleFx.ModCore, RoleFx.ModDeep, RoleFx.ModCore)
        else -> listOf(RoleFx.PremCore, RoleFx.PremLight, Color.White, RoleFx.PremLight, RoleFx.PremCore)
    }
    Box(modifier.size(total), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer { alpha = 0.55f + 0.45f * pulseOf(pulseMs); compositingStrategy = CompositingStrategy.ModulateAlpha }
                .drawWithCache {
                    val c = Offset(this.size.width / 2f, this.size.height / 2f)
                    val outer = this.size.minDimension / 2f - halo.toPx()
                    val glowR = outer + if (big) halo.toPx() else 4.dp.toPx()
                    val glow = Brush.radialGradient(listOf(tint.copy(alpha = if (owner) 0.65f else 0.38f), Color.Transparent), c, glowR)
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
        if (owner && big) Box(
            Modifier.matchParentSize().graphicsLayer { }.drawBehind {
                val c = center
                val outer = this.size.minDimension / 2f - halo.toPx()
                val t = phaseOf(3800)
                for (i in 0 until 16) {
                    val seed = i * 0.61803f
                    val p = (t + seed) % 1f
                    val ang = seed * 6.2831f * 3f
                    val r = (1.1f + (i % 3) * 0.7f).dp.toPx() * (1f - p * 0.5f)
                    val x = (c.x + cos(ang) * outer * 1.02f + sin(p * 12.566f + i) * 5.dp.toPx()).coerceIn(r, this.size.width - r)
                    val y = (c.y + sin(ang) * outer * 0.95f - p * halo.toPx() * 1.6f).coerceIn(r, this.size.height - r)
                    drawCircle((if (i % 2 == 0) RoleFx.OwnerEmber else RoleFx.OwnerHot).copy(alpha = (1f - p) * 0.95f), r, Offset(x, y))
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
            else -> listOf(RoleFx.PremLight, Color.White, RoleFx.PremCore, RoleFx.PremLight)
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
            .graphicsLayer { alpha = 0.6f + 0.4f * pulseOf(pulseMs); compositingStrategy = CompositingStrategy.ModulateAlpha }
            .drawWithCache {
                val c = Offset(size.width / 2f, size.height * 0.46f)
                val r = min(size.height * 0.46f, size.width * 0.55f)
                val radial = Brush.radialGradient(listOf(tint.copy(alpha = if (owner) 0.42f else 0.24f), Color.Transparent), c, r)
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
