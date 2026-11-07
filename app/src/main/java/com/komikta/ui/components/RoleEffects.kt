package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.theme.Mono
import com.komikta.ui.theme.Poppins
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Efek role bertema Jepang klasik (samurai bayangan dan berlian), selaras dengan aplikasi komik:
 *  - Owner "Shogun": merah dengan aksen emas. Cincin merah dengan satu wajik emas, latar matahari merah datar tipis.
 *  - Moderator "Penjaga bayangan": hijau teal gelap dan perak. Cincin teal gelap bertepi perak dengan satu shuriken, latar dua goresan katana.
 *  - Premium "Berlian": biru es berlian. Cincin biru berlian dengan tiga berlian kecil, di profil berlian dan kilau melayang naik pelan.
 * Lencana berbentuk papan sumi gelap dengan segel hanko merah (kanji 主 / 守 / 宝). Nama bergaris sapuan kuas.
 * Tanpa cahaya kabur. Elemen yang tampil banyak (lencana, avatar kecil, nama, komentar) statis; satu-satunya gerak: berlian dan kilau Premium di profil.
 */
internal object RoleFx {
    val OwnerCore = Color(0xFFE0323C)
    val OwnerHot = Color(0xFFFF6B6B)
    val OwnerEmber = Color(0xFFB81D32)
    val OwnerGold = Color(0xFFFFE7A3)
    val OwnerDeep = Color(0xFF7A0F1A)
    val ModCore = Color(0xFF2EC4A5)
    val ModLight = Color(0xFF7EE7CF)
    val ModDeep = Color(0xFF0F4F43)
    val PremCore = Color(0xFF4FB3F6)
    val PremLight = Color(0xFFB5E3FF)
    val PremIce = Color(0xFFEAF7FF)
    val PremDeep = Color(0xFF1E5F9E)
    /** Merah segel hanko: sama untuk semua role, jadi lencana tampak satu keluarga. */
    val Vermilion = Color(0xFFC8312F)
    val Ink = Color(0xFF0E0F12)
    val Sumi = Color(0xFF14151A)
    val Silver = Color(0xFFD5DBEE)

    fun light(role: String): Color = when (role) {
        "OWNER" -> OwnerHot
        "MODERATOR" -> ModLight
        else -> PremLight
    }

    fun core(role: String): Color = when (role) {
        "OWNER" -> OwnerCore
        "MODERATOR" -> ModCore
        else -> PremCore
    }

    fun deep(role: String): Color = when (role) {
        "OWNER" -> OwnerDeep
        "MODERATOR" -> ModDeep
        else -> PremDeep
    }

    fun kanji(role: String): String = when (role) {
        "OWNER" -> "主"
        "MODERATOR" -> "守"
        else -> "宝"
    }
}

internal object RoleClock {
    val ms = mutableFloatStateOf(0f)
    val users = mutableIntStateOf(0)
}

/** Jam animasi hanya berjalan selama ada elemen role yang bergerak di layar. */
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
private fun UseRoleClock(enabled: Boolean = true) {
    if (!enabled) return
    DisposableEffect(Unit) {
        RoleClock.users.intValue++
        onDispose { RoleClock.users.intValue-- }
    }
}

private fun phaseOf(millis: Int): Float = (RoleClock.ms.floatValue % millis) / millis

/** Kilatan singkat lalu jeda panjang. Hasil 0..1. */
private fun flash(periodMs: Int, seed: Float): Float {
    val p = (RoleClock.ms.floatValue / periodMs + seed) % 1f
    return if (p < 0.2f) sin(p / 0.2f * 3.1415927f) else 0f
}

/** Acak deterministik 0..1 dari bilangan (tanpa Random, supaya gambarnya stabil antar frame). */
private fun rnd(x: Float): Float {
    val v = sin(x * 12.9898f) * 43758.547f
    return abs(v - floor(v))
}

/** Berlian kecil berpusat di (x,y): segitiga atas lebih terang, bawah lebih gelap, garis tinta tipis. Tinggi 2s. */
private fun DrawScope.gem(x: Float, y: Float, s: Float, deg: Float, alpha: Float, ink: Boolean = false) {
    withTransform({ translate(x, y); rotate(deg, Offset.Zero) }) {
        val top = Path().apply { moveTo(0f, -s); lineTo(0.75f * s, 0f); lineTo(-0.75f * s, 0f); close() }
        val bottom = Path().apply { moveTo(-0.75f * s, 0f); lineTo(0.75f * s, 0f); lineTo(0f, s); close() }
        drawPath(top, RoleFx.PremIce.copy(alpha = alpha))
        drawPath(bottom, RoleFx.PremCore.copy(alpha = alpha))
        if (ink) {
            val whole = Path().apply { moveTo(0f, -s); lineTo(0.75f * s, 0f); lineTo(0f, s); lineTo(-0.75f * s, 0f); close() }
            drawPath(whole, RoleFx.Ink.copy(alpha = alpha), style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}

/** Kilau empat sudut kecil. */
private fun DrawScope.sparkle(center: Offset, r: Float, alpha: Float, color: Color) {
    if (alpha <= 0.01f) return
    val w = (r * 0.2f).coerceAtLeast(1f)
    val c = color.copy(alpha = alpha)
    drawLine(c, Offset(center.x - r, center.y), Offset(center.x + r, center.y), w, StrokeCap.Round)
    drawLine(c, Offset(center.x, center.y - r), Offset(center.x, center.y + r), w, StrokeCap.Round)
}

/** Shuriken empat bilah berpusat di (x,y). */
private fun DrawScope.shuriken(x: Float, y: Float, r: Float, deg: Float, fill: Color, alpha: Float = 1f) {
    withTransform({ translate(x, y); rotate(deg, Offset.Zero) }) {
        val p = Path()
        for (i in 0 until 8) {
            val a = i * 0.7853982f
            val rr = if (i % 2 == 0) r else r * 0.3f
            val px = cos(a) * rr
            val py = sin(a) * rr
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
        }
        p.close()
        drawPath(p, fill.copy(alpha = alpha))
        drawPath(p, RoleFx.Ink.copy(alpha = alpha), style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
        drawCircle(RoleFx.Ink.copy(alpha = alpha), r * 0.14f, Offset.Zero)
    }
}

/** Wajik kecil (hishi), hiasan kamon. */
private fun DrawScope.hishi(x: Float, y: Float, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(x, y - r); lineTo(x + r * 0.7f, y); lineTo(x, y + r); lineTo(x - r * 0.7f, y); close()
    }
    drawPath(p, color)
    drawPath(p, RoleFx.Ink, style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
}

/** Lingkaran enso: satu sapuan kuas terbuka, tebal di awal dan menipis di ujung. */
private fun DrawScope.enso(c: Offset, r: Float, w: Float, color: Color, startDeg: Float) {
    val tl = Offset(c.x - r, c.y - r)
    val sz = androidx.compose.ui.geometry.Size(r * 2, r * 2)
    drawArc(color, startDeg, 200f, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
    drawArc(color, startDeg + 190f, 90f, false, tl, sz, style = Stroke(w * 0.6f, cap = StrokeCap.Round))
    drawArc(color, startDeg + 275f, 55f, false, tl, sz, style = Stroke(w * 0.3f, cap = StrokeCap.Round))
}

/** Goresan katana: garis tipis menyilang, tebal di tengah dan menipis di kedua ujung. */
private fun DrawScope.slash(from: Offset, to: Offset, w: Float, color: Color) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
    val nx = -dy / len * w / 2f
    val ny = dx / len * w / 2f
    val mid = Offset((from.x + to.x) / 2f, (from.y + to.y) / 2f)
    val p = Path().apply {
        moveTo(from.x, from.y)
        lineTo(mid.x + nx, mid.y + ny)
        lineTo(to.x, to.y)
        lineTo(mid.x - nx, mid.y - ny)
        close()
    }
    drawPath(p, color)
}

/** Latar per role. [compact] = baris komentar; false = header profil. [now] = waktu animasi (ms) atau null untuk statis. */
private fun DrawScope.roleBackdrop(role: String, c: Offset, compact: Boolean, now: Float?) {
    val w = size.width
    val h = size.height
    when (role) {
        "OWNER" -> {
            val r = if (compact) 64.dp.toPx() else min(h * 0.36f, w * 0.3f)
            drawCircle(RoleFx.Vermilion.copy(alpha = if (compact) 0.14f else 0.18f), r, c)
            if (!compact) enso(c, r * 1.14f, 3.dp.toPx(), RoleFx.OwnerGold.copy(alpha = 0.34f), 130f)
        }
        "MODERATOR" -> {
            val n = if (compact) 1 else 2
            for (i in 0 until n) {
                val y0 = (if (compact) c.y - 36.dp.toPx() else h * (0.12f + 0.32f * i))
                val from = Offset((if (compact) c.x + 30.dp.toPx() else -10f) + i * 60.dp.toPx() * (if (compact) 1f else 0.7f), y0)
                val to = Offset(from.x + (if (compact) 150.dp.toPx() else w * 0.9f), from.y + (if (compact) 90.dp.toPx() else h * 0.55f))
                slash(from, to, (if (i == 0) 2.6f else 1.2f).dp.toPx(), RoleFx.ModLight.copy(alpha = if (i == 0) 0.14f else 0.2f))
            }
        }
        else -> {
            if (now == null) {
                // Statis (komentar): beberapa berlian kecil di sekitar avatar.
                for (i in 0 until 4) {
                    val x = c.x + (20f + 200f * rnd(i * 2.3f)).dp.toPx()
                    val y = c.y + (-40f + 90f * rnd(i * 4.1f)).dp.toPx()
                    gem(x, y, (3.5f + 2f * rnd(i * 1.7f)).dp.toPx(), 0f, 0.5f * (1f - (x - c.x) / 260.dp.toPx()).coerceIn(0.2f, 1f))
                }
            } else {
                // Profil: berlian kecil dan kilau melayang naik pelan, memudar di ujung.
                for (i in 0 until 8) {
                    val t = (now / (9000f + i * 400f) + rnd(i * 1.3f)) % 1f
                    val x = w * rnd(i * 2.3f) + sin(t * 6.2831855f + i) * 8.dp.toPx()
                    val y = h + 10.dp.toPx() - t * (h + 20.dp.toPx())
                    val a = sin(t * 3.1415927f).coerceIn(0f, 1f) * 0.8f
                    if (i % 2 == 0) gem(x, y, (4f + 2.5f * rnd(i * 1.7f)).dp.toPx(), 0f, a)
                    else sparkle(Offset(x, y), (4f + 3f * rnd(i * 1.7f)).dp.toPx(), a, RoleFx.PremLight)
                }
            }
        }
    }
}

/**
 * Lencana role: papan sumi gelap bergaris tipis warna role, dengan segel hanko merah berisi kanji (主 / 守 / 桜) yang miring sedikit.
 */
@Composable
internal fun FxRoleBadge(role: String, tint: Color, modifier: Modifier = Modifier, large: Boolean = false) {
    val shape = RoundedCornerShape(if (large) 6.dp else 4.dp)
    val seal = RoleFx.Vermilion
    val sealSize = if (large) 22.dp else 15.dp
    Box(modifier.clip(shape).background(RoleFx.Sumi).border(1.dp, tint.copy(alpha = 0.75f), shape)) {
        Row(
            Modifier.then(if (large) Modifier.height(34.dp).padding(start = 6.dp, end = 14.dp) else Modifier.padding(start = 3.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(sealSize).clip(RoundedCornerShape(2.dp)).background(seal),
                contentAlignment = Alignment.Center
            ) {
                Text(RoleFx.kanji(role), fontSize = (sealSize.value * 0.62f).sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
            Text(
                roleName(role), fontSize = if (large) 14.sp else 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1,
                modifier = Modifier.padding(start = if (large) 8.dp else 5.dp)
            )
        }
    }
}

private fun DrawScope.roleFrame(role: String, c: Offset, avatarR: Float, sticker: Float, ext: Float, big: Boolean) {
    val base = avatarR + sticker
    val line = (if (big) 1.4f else 1f).dp.toPx()
    val mid = base + ext / 2f
    when (role) {
        "OWNER" -> {
            drawCircle(RoleFx.OwnerCore, mid, c, style = Stroke(ext))
            drawCircle(RoleFx.Ink, base + ext - line / 2f, c, style = Stroke(line))
            drawCircle(RoleFx.Ink, base + line / 2f, c, style = Stroke(line))
            drawCircle(RoleFx.OwnerGold, mid, c, style = Stroke(if (big) 1.2.dp.toPx() else 0.8.dp.toPx()))
            // Satu wajik emas (kamon) di atas, hanya di ukuran besar.
            if (big) hishi(c.x, c.y - mid, ext * 0.42f, RoleFx.OwnerGold)
        }
        "MODERATOR" -> {
            drawCircle(RoleFx.ModDeep, mid, c, style = Stroke(ext))
            drawCircle(RoleFx.Ink, base + ext - line / 2f, c, style = Stroke(line))
            drawCircle(RoleFx.Ink, base + line / 2f, c, style = Stroke(line))
            // Tepi bilah perak tipis di sisi luar.
            drawCircle(RoleFx.Silver, base + ext - line - 0.8.dp.toPx(), c, style = Stroke(0.9.dp.toPx()))
            // Satu shuriken perak di pojok kanan atas.
            shuriken(c.x + cos(-0.78f) * mid, c.y + sin(-0.78f) * mid, if (big) ext * 0.62f else ext * 0.95f, 15f, RoleFx.Silver)
        }
        else -> {
            drawCircle(RoleFx.PremCore, mid, c, style = Stroke(ext))
            drawCircle(RoleFx.Ink, base + ext - line / 2f, c, style = Stroke(line))
            drawCircle(RoleFx.Ink, base + line / 2f, c, style = Stroke(line))
            drawCircle(RoleFx.PremIce.copy(alpha = 0.55f), mid, c, style = Stroke(0.9.dp.toPx()))
            // Tiga berlian kecil di sisi tetap.
            for (k in 0 until 3) {
                val a = -0.9f + k * 2.1f
                gem(c.x + cos(a) * mid, c.y + sin(a) * mid, if (big) ext * 0.62f else ext * 0.95f, 0f, 1f, true)
            }
        }
    }
}

/**
 * Foto profil dengan bingkai role + pinggir putih tipis. Ukuran kecil dan besar sama bentuknya. Semua bingkai statis dan sederhana: satu hiasan
 * khas tiap role (wajik emas, shuriken, berlian).
 */
@Composable
fun RoleAvatar(url: String?, name: String, size: Dp, role: String, modifier: Modifier = Modifier) {
    val tint = roleTint(role)
    if (tint == null) {
        Avatar(url, name, size, modifier)
        return
    }
    val big = size >= 80.dp
    val ext = if (big) 14.dp else 5.dp
    val sticker = if (big) 3.dp else 2.dp
    val total = size + (ext + sticker) * 2
    Box(modifier.size(total), contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize().drawBehind {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                roleFrame(role, c, this.size.minDimension / 2f - (ext + sticker).toPx(), sticker.toPx(), ext.toPx(), big)
            }
        )
        Box(
            Modifier.matchParentSize().drawBehind {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val avatarR = this.size.minDimension / 2f - (ext + sticker).toPx()
                drawCircle(Color.White, avatarR + sticker.toPx() / 2f, c, style = Stroke(sticker.toPx()))
            }
        )
        Avatar(url, name, size)
    }
}

/** Nama dengan warna role terang dan sapuan kuas di bawahnya (tebal di kiri, menipis ke kanan). Pengguna biasa tetap putih. */
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
    val brush = RoleFx.core(role)
    Text(
        text,
        style = TextStyle(fontFamily = Poppins, color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Bold),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .drawBehind {
                val t = 1.6.dp.toPx()
                val y = size.height - 1.5.dp.toPx()
                val p = Path().apply {
                    moveTo(0f, y - t)
                    quadraticBezierTo(size.width * 0.5f, y - t * 0.5f, size.width, y)
                    quadraticBezierTo(size.width * 0.5f, y + t * 0.9f, 0f, y + t)
                    close()
                }
                drawPath(p, brush.copy(alpha = 0.9f))
            }
            .padding(bottom = 3.dp)
    )
}

/** Latar header profil: Owner matahari datar + enso, Moderator goresan katana, Premium berlian dan kilau melayang. */
@Composable
fun RoleAura(role: String?, modifier: Modifier = Modifier) {
    if (roleTint(role.orEmpty()) == null) return
    val falling = role == "PREMIUM"
    UseRoleClock(falling)
    Box(
        modifier.clipToBounds().drawBehind {
            roleBackdrop(role.orEmpty(), Offset(size.width / 2f, size.height * 0.46f), false, if (falling) RoleClock.ms.floatValue else null)
        }
    )
}

/** Pintu masuk latar komentar (statis, versi ringkas). */
internal fun DrawScope.drawRoleBackdrop(role: String, center: Offset) = roleBackdrop(role, center, true, null)
