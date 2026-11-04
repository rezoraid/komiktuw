package com.komikta.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.theme.Mono
import kotlin.math.cos
import kotlin.math.sin

/** Palet role: Owner merah api, Moderator hijau zamrud (penjaga), Premium biru. */
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

    fun light(role: String): Color = when (role) {
        "OWNER" -> OwnerEmber
        "MODERATOR" -> ModLight
        else -> PremLight
    }
}

/** 0 -> 1 berulang (untuk sapuan / putaran). Dibaca di lambda gambar supaya tidak merekomposisi tiap frame. */
@Composable
internal fun rememberPhase(millis: Int, label: String): State<Float> {
    val t = rememberInfiniteTransition(label = label)
    return t.animateFloat(0f, 1f, infiniteRepeatable(tween(millis, easing = LinearEasing), RepeatMode.Restart), label = label)
}

/** 0 -> 1 -> 0 halus (untuk denyut cahaya). */
@Composable
internal fun rememberPulse(millis: Int, label: String): State<Float> {
    val t = rememberInfiniteTransition(label = label)
    return t.animateFloat(0f, 1f, infiniteRepeatable(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = label)
}

/** Garis cahaya putih yang menyapu dari kiri ke kanan. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.sweepShine(phase: Float, strength: Float) {
    val x = (phase * 2.2f - 0.6f) * size.width
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
            start = Offset(x, 0f),
            end = Offset(x + size.width * 0.45f, size.height)
        )
    )
}

/** Kapsul role dengan efek: Premium berkilau biru, Moderator berdenyut hijau, Owner merah menyala. */
@Composable
internal fun FxRoleBadge(role: String, tint: Color, modifier: Modifier = Modifier) {
    val owner = role == "OWNER"
    val light = RoleFx.light(role)
    val sweep = rememberPhase(if (owner) 1700 else 2800, "badgeSweep")
    val pulse = rememberPulse(if (owner) 850 else 1500, "badgePulse")
    val icon = when (role) {
        "OWNER" -> AppIcons.crown
        "MODERATOR" -> AppIcons.shield
        else -> AppIcons.diamond
    }
    val fill = if (owner) listOf(RoleFx.OwnerDeep, RoleFx.OwnerCore, RoleFx.OwnerHot)
    else listOf(tint.copy(alpha = 0.38f), tint.copy(alpha = 0.18f))
    Row(
        modifier
            .drawBehind {
                val g = 0.5f + 0.5f * pulse.value
                val padX = 7.dp.toPx()
                val padY = 5.dp.toPx()
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = (if (owner) 0.6f else 0.34f) * g), Color.Transparent),
                        center = center,
                        radius = size.width * 0.75f
                    ),
                    topLeft = Offset(-padX, -padY),
                    size = Size(size.width + padX * 2, size.height + padY * 2),
                    cornerRadius = CornerRadius((size.height + padY * 2) / 2f)
                )
            }
            .clip(CircleShape)
            .background(Brush.horizontalGradient(fill))
            .border(1.dp, Brush.horizontalGradient(listOf(light, tint, light)), CircleShape)
            .drawWithContent {
                drawContent()
                sweepShine(sweep.value, if (owner) 0.6f else 0.45f)
            }
            .padding(start = 6.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (owner) RoleFx.OwnerGold else light, modifier = Modifier.size(13.dp))
        Text(roleName(role), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
    }
}

/**
 * Foto profil dengan bingkai khusus role. Premium: cincin biru berkilau. Moderator: cincin hijau berdenyut.
 * Owner: cincin api merah yang berputar, cahaya berdenyut, dan percikan bara untuk ukuran besar (halaman profil).
 */
@Composable
fun RoleAvatar(url: String?, name: String, size: Dp, role: String, modifier: Modifier = Modifier) {
    val tint = roleTint(role)
    if (tint == null) {
        Avatar(url, name, size, modifier)
        return
    }
    val owner = role == "OWNER"
    val big = size >= 80.dp
    val ring = if (big) 4.dp else if (owner) 2.5.dp else 2.dp
    val gap = if (big) 3.dp else 2.dp
    val halo = if (big) 18.dp else 0.dp
    val total = size + (ring + gap) * 2 + halo * 2
    val spin = rememberPhase(if (owner) 2400 else 7000, "ringSpin")
    val pulse = rememberPulse(if (owner) 950 else 1800, "ringPulse")
    val embers = rememberPhase(3800, "embers")
    val colors = when (role) {
        "OWNER" -> listOf(RoleFx.OwnerHot, RoleFx.OwnerEmber, RoleFx.OwnerGold, RoleFx.OwnerEmber, RoleFx.OwnerCore, RoleFx.OwnerDeep, RoleFx.OwnerHot)
        "MODERATOR" -> listOf(RoleFx.ModCore, RoleFx.ModLight, RoleFx.ModCore, RoleFx.ModDeep, RoleFx.ModCore)
        else -> listOf(RoleFx.PremCore, RoleFx.PremLight, Color.White, RoleFx.PremLight, RoleFx.PremCore)
    }
    Box(
        modifier
            .size(total)
            .drawBehind {
                val c = center
                val outer = this.size.minDimension / 2f - halo.toPx()
                val g = 0.55f + 0.45f * pulse.value
                val glowR = outer + if (big) halo.toPx() else 4.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = (if (owner) 0.65f else 0.38f) * g), Color.Transparent),
                        center = c,
                        radius = glowR
                    ),
                    radius = glowR,
                    center = c
                )
                rotate(spin.value * 360f, pivot = c) {
                    drawCircle(
                        brush = Brush.sweepGradient(colors, center = c),
                        radius = outer - ring.toPx() / 2f,
                        center = c,
                        style = Stroke(ring.toPx())
                    )
                }
                if (owner && big) {
                    val t = embers.value
                    for (i in 0 until 16) {
                        val seed = i * 0.61803f
                        val p = (t + seed) % 1f
                        val ang = seed * 6.2831f * 3f
                        val x = c.x + cos(ang) * outer * 1.02f + sin(p * 12.566f + i) * 5.dp.toPx()
                        val y = c.y + sin(ang) * outer * 0.95f - p * halo.toPx() * 2.4f
                        val a = (1f - p) * 0.95f
                        val r = (1.1f + (i % 3) * 0.7f).dp.toPx() * (1f - p * 0.5f)
                        drawCircle(
                            color = (if (i % 2 == 0) RoleFx.OwnerEmber else RoleFx.OwnerHot).copy(alpha = a),
                            radius = r,
                            center = Offset(x, y)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) { Avatar(url, name, size) }
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
    val owner = role == "OWNER"
    val phase by rememberPhase(if (owner) 2200 else 3600, "nameShift")
    val shift = phase * 520f
    val colors = when (role) {
        "OWNER" -> listOf(RoleFx.OwnerHot, RoleFx.OwnerGold, RoleFx.OwnerHot, RoleFx.OwnerCore)
        "MODERATOR" -> listOf(RoleFx.ModLight, RoleFx.ModCore, RoleFx.ModLight)
        else -> listOf(RoleFx.PremLight, Color.White, RoleFx.PremCore, RoleFx.PremLight)
    }
    val brush = Brush.linearGradient(colors, start = Offset(shift, 0f), end = Offset(shift + 260f, 0f), tileMode = TileMode.Mirror)
    Text(
        text,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontWeight = fontWeight,
            shadow = Shadow(color = tint.copy(alpha = if (owner) 0.7f else 0.45f), offset = Offset.Zero, blurRadius = if (owner) 12f else 7f)
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Cahaya latar header profil: Owner paling kuat (merah menyala), role lain lebih lembut. */
@Composable
fun Modifier.roleAura(role: String?): Modifier {
    val tint = role?.let { roleTint(it) } ?: return this
    val owner = role == "OWNER"
    val pulse = rememberPulse(if (owner) 1400 else 2200, "aura")
    return this.drawBehind {
        val g = 0.6f + 0.4f * pulse.value
        if (owner) {
            drawRect(Brush.verticalGradient(listOf(RoleFx.OwnerDeep.copy(alpha = 0.55f * g), Color.Transparent), startY = 0f, endY = size.height))
        }
        val c = Offset(size.width / 2f, 150.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tint.copy(alpha = (if (owner) 0.42f else 0.24f) * g), Color.Transparent),
                center = c,
                radius = size.width * 0.8f
            ),
            radius = size.width * 0.8f,
            center = c
        )
    }
}
