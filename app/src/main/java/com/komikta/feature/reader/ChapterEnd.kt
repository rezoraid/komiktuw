package com.komikta.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.komikta.domain.model.Chapter
import com.komikta.ui.components.AppIcons

private val Line = Color(0xFF262626)
private val Muted = Color(0xFF8A8A8A)
private val CardShape = RoundedCornerShape(20.dp)

private fun cleanNumber(raw: String?): String? =
    raw?.replace(Regex("(?i)^\\s*(chapter|ch\\.?)\\s*"), "")?.trim()?.takeIf { it.isNotEmpty() }

@Composable
fun ChapterEnd(
    current: Chapter?,
    fallbackTitle: String?,
    nextSlug: String?,
    next: Chapter?,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val currentNumber = cleanNumber(current?.number)
    val finished = currentNumber?.let { "Chapter $it selesai" } ?: fallbackTitle?.takeIf { it.isNotBlank() }?.let { "$it selesai" } ?: "Chapter selesai"
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF080808))))
            .padding(horizontal = 20.dp)
            .padding(top = if (compact) 48.dp else 56.dp, bottom = if (compact) 48.dp else 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(AppIcons.checkCircle, null, tint = Color.White, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(14.dp))
        Text(finished, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(0.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            listOf(0.25f, 0.45f, 0.7f).forEach { a ->
                Icon(AppIcons.chevronDown, null, tint = Color.White.copy(alpha = a), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        if (nextSlug.isNullOrBlank()) {
            Text("Kamu sudah membaca chapter terbaru", style = MaterialTheme.typography.bodyMedium, color = Muted, textAlign = TextAlign.Center)
        } else {
            val number = cleanNumber(next?.number)
            val title = next?.title?.trim()?.takeIf { it.isNotEmpty() && cleanNumber(it) != number }
            Row(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF141414), Color(0xFF0C0C0C))))
                    .border(1.dp, Line, CardShape)
                    .then(if (onNext != null) Modifier.clickable(onClick = onNext) else Modifier)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Chapter berikutnya", style = MaterialTheme.typography.labelMedium, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        number?.let { "Chapter $it" } ?: title ?: nextSlug,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (number != null && title != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal), color = Color(0xFFB5B5B5), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (onNext != null) {
                    Spacer(Modifier.size(12.dp))
                    Box(Modifier.size(40.dp).clip(CircleShape).border(1.dp, Line, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(AppIcons.arrowRight, "Next", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
