package com.komikta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0A0A0C)
private val Surface = Color(0xFF121216)
private val Surface2 = Color(0xFF1A1A20)
private val Primary = Color(0xFFA78BFA)
private val Text1 = Color(0xFFFAFAFA)
private val Text2 = Color(0xFFA1A1AA)
private val Text3 = Color(0xFF71717A)

private val Scheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.Black,
    secondary = Color(0xFF22D3EE),
    background = Bg,
    onBackground = Text1,
    surface = Surface,
    onSurface = Text1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Text2,
    outline = Color(0xFF27272A)
)

private val Type = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Text1),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Text1),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Text1),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Text1),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Text1),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Text1),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Text2),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Text2),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, color = Text3)
)

@Composable
fun KomiktaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}
