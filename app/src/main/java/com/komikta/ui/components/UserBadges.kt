package com.komikta.ui.components

import com.komikta.BuildConfig

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.core.common.AvatarBust
import com.komikta.ui.theme.Mono
import com.komikta.ui.theme.Ui

/** Warna role. null = role biasa (Member), tampil netral. */
fun roleTint(role: String): Color? = when (role) {
    "OWNER" -> Color(0xFF2398F7)
    "MODERATOR" -> Color(0xFF2FBF9B)
    "PREMIUM" -> Color(0xFFE0A526)
    else -> null
}

fun roleName(role: String): String = when (role) {
    "OWNER" -> "Owner"
    "MODERATOR" -> "Moderator"
    "PREMIUM" -> "Premium"
    else -> "Member"
}

/** Kapsul role untuk komentar: Owner biru, Moderator hijau, Premium emas, Member abu netral. */
@Composable
fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val tint = roleTint(role)
    val bg = tint?.copy(alpha = 0.22f) ?: Mono.CardHigh
    val line = tint?.copy(alpha = 0.8f) ?: Mono.Line
    Text(
        roleName(role), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Mono.Text, maxLines = 1,
        modifier = modifier.clip(CircleShape).background(bg).border(1.dp, line, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp)
    )
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
