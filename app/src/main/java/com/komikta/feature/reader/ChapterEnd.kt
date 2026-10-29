package com.komikta.feature.reader

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
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

private val CardTop = Color(0xFF0E0E12)
private val CardBottom = Color(0xFF09090C)
private val PillBg = Color(0xFF1A1B21)
private val PillText = Color(0xFFB9BCC8)
private val SubText = Color(0xFFA7AAB8)
private val LineColor = Color(0xFF4B4D57)
private val Muted = Color(0xFF8A8A8A)
private val CardShape = RoundedCornerShape(28.dp)

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
    val pillText = (currentNumber?.let { "Chapter $it selesai" }
        ?: fallbackTitle?.takeIf { it.isNotBlank() }?.let { "$it selesai" }
        ?: "Chapter selesai").uppercase()
    val hasNext = !nextSlug.isNullOrBlank()

    val nextNumber = cleanNumber(next?.number)
    val nextTitle = next?.title?.trim()?.takeIf { it.isNotEmpty() && cleanNumber(it) != nextNumber }
    val nextLabel = nextNumber?.let { "Chapter $it" } ?: nextTitle

    val bob by rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bobY"
    )

    Box(
        modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .padding(top = if (compact) 20.dp else 28.dp, bottom = if (compact) 20.dp else 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(CardShape)
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                .drawBehind {
                    // lingkaran halus di pojok kiri atas
                    drawCircle(
                        color = Color.White.copy(alpha = 0.03f),
                        radius = size.width * 0.28f,
                        center = Offset(0f, 0f)
                    )
                }
                .padding(horizontal = 24.dp)
                .padding(top = 44.dp, bottom = if (hasNext) 36.dp else 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ikon centang dengan halo
            Box(
                Modifier
                    .size(78.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.checkCircle, null, tint = Color.White, modifier = Modifier.size(52.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Pill "CHAPTER X SELESAI"
            Text(
                pillText,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(PillBg)
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp, fontSize = 12.sp),
                color = PillText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Akhir Chapter",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                color = Color(0xFFF2F2F7),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Kamu telah menyelesaikan chapter ini.",
                style = MaterialTheme.typography.bodyLarge,
                color = SubText,
                textAlign = TextAlign.Center
            )

            if (!hasNext) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "Kamu sudah membaca chapter terbaru",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(36.dp))

                Column(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (onNext != null) Modifier.clickable(onClick = onNext) else Modifier)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // garis — tombol chevron — garis
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(1.dp).background(LineColor))
                        Spacer(Modifier.width(16.dp))
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(PillBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                AppIcons.chevronsDown,
                                "Chapter berikutnya",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp).offset(y = (bob - 2.5f).dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.weight(1f).height(1.dp).background(LineColor))
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "CHAPTER SELANJUTNYA",
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.5.sp, fontSize = 12.sp),
                        color = PillText
                    )

                    if (nextLabel != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            nextLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (nextNumber != null && nextTitle != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                nextTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SubText,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterNav(
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 12.dp)
            .padding(top = 32.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val prevColor = if (hasPrev) Color.White else Color(0xFF4A4A4A)
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .then(if (hasPrev) Modifier.clickable(onClick = onPrev) else Modifier)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(AppIcons.chevronLeft, null, tint = prevColor, modifier = Modifier.size(20.dp))
            Text("Previous", style = MaterialTheme.typography.titleMedium, color = prevColor)
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = if (hasNext) onNext else onDetail)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (hasNext) "Next" else "Back to detail", style = MaterialTheme.typography.titleMedium, color = Color.White)
            if (hasNext) Icon(AppIcons.chevronRight, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}
