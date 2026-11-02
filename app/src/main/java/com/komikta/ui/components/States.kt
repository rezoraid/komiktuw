package com.komikta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komikta.ui.theme.Ui

/** Tampilan "kosong" yang konsisten: ikon dalam lingkaran lembut, judul, penjelasan, dan tombol aksi opsional. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = if (compact) 20.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        Box(Modifier.size(if (compact) 60.dp else 88.dp).clip(CircleShape).background(Ui.AccentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Ui.Accent, modifier = Modifier.size(if (compact) 28.dp else 40.dp))
        }
        Text(title, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, fontSize = 15.sp, lineHeight = 22.sp, color = Ui.Text2, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ui.OnAccent,
                modifier = Modifier.padding(top = 6.dp).clip(CircleShape).background(Ui.Accent).clickable(onClick = onAction).padding(horizontal = 26.dp, vertical = 11.dp)
            )
        }
    }
}

/** Gagal memuat: pesan error + tombol coba lagi. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(AppIcons.close, "Gagal memuat", message, modifier, actionLabel = "Coba lagi", onAction = onRetry)
}
