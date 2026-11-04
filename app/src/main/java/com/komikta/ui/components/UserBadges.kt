package com.komikta.ui.components

import com.komikta.BuildConfig

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.core.common.AvatarBust
import com.komikta.ui.theme.Mono
import com.komikta.ui.theme.Ui

/** Warna role. null = role biasa (Member), tampil netral. */
fun roleTint(role: String): Color? = when (role) {
    "OWNER" -> RoleFx.OwnerCore
    "MODERATOR" -> RoleFx.ModCore
    "PREMIUM" -> RoleFx.PremCore
    else -> null
}

fun roleName(role: String): String = when (role) {
    "OWNER" -> "Owner"
    "MODERATOR" -> "Moderator"
    "PREMIUM" -> "Premium"
    else -> "Member"
}

/** Kapsul role: Member abu netral; Premium/Moderator/Owner memakai efek khusus ([FxRoleBadge]). */
@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val tint = roleTint(role)
    if (tint != null) {
        FxRoleBadge(role, tint, modifier)
        return
    }
    Text(
        roleName(role), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, maxLines = 1,
        modifier = modifier.clip(CircleShape).background(Mono.CardHigh).border(1.dp, Mono.Line, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** Centang verified: lencana bergerigi biru dengan tanda centang putih. */
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, size: Dp = 16.dp, color: Color = Color(0xFF1D9BF0)) {
    Canvas(modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.width / 2f
        for (i in 0 until 8) {
            val a = i * Math.PI / 4
            drawCircle(color, r * 0.36f, Offset(c.x + (r * 0.64f * Math.cos(a)).toFloat(), c.y + (r * 0.64f * Math.sin(a)).toFloat()))
        }
        drawCircle(color, r * 0.78f, c)
        val check = Path().apply {
            moveTo(c.x - r * 0.38f, c.y + r * 0.02f); lineTo(c.x - r * 0.08f, c.y + r * 0.32f); lineTo(c.x + r * 0.4f, c.y - r * 0.26f)
        }
        drawPath(check, Color.White, style = Stroke(width = r * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Nama dengan efek role + centang verified (jika ada). Dipakai di komentar dan profil. */
@Composable
fun NameWithBadge(text: String, role: String, verified: Boolean, fontSize: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier, fontWeight: FontWeight = FontWeight.SemiBold, badgeSize: Dp = 16.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoleName(text, role, fontSize, Modifier.weight(1f, fill = false), fontWeight)
        if (verified) VerifiedBadge(Modifier.padding(start = 5.dp), badgeSize)
    }
}

/** Kapsul level: bintang putih + angka, netral abu gelap. */
@Composable
fun LevelChip(level: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(Mono.CardHigh).border(1.dp, Mono.Line, CircleShape).padding(start = 7.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.star, null, tint = Mono.Icon, modifier = Modifier.size(12.dp))
        Text("Lv $level", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun Avatar(url: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val frame = modifier.size(size).clip(CircleShape).background(Ui.Chip)
    if (url.isNullOrBlank()) {
        Box(frame, contentAlignment = Alignment.Center) {
            Text(name.trim().take(1).uppercase().ifEmpty { "?" }, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    } else {
        CoverImage(url = AvatarBust.apply(url).let { if (it.startsWith("/")) BuildConfig.BASE_URL.trimEnd('/') + it else it }, contentDescription = name, widthPx = 160, heightPx = 160, modifier = frame)
    }
}
