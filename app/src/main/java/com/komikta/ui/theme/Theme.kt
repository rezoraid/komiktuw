package com.komikta.ui.theme

import com.komikta.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF181C21)
private val Surface = Color(0xFF292C31)
private val Surface2 = Color(0xFF32373D)
private val Primary = Color(0xFF7BCFFF)
private val Text1 = Color.White
private val Text2 = Color(0xFFA7ADB4)
private val Text3 = Color(0xFF7D848C)

private val Scheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF08293B),
    secondary = Color(0xFFA8D8F5),
    background = Bg,
    onBackground = Text1,
    surface = Surface,
    onSurface = Text1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Text2,
    outline = Color(0xFF454B52)
)

private val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibold, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.Bold)
)

private val Type = Typography(
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Text1),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Text1),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = Text1),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = Text1),
    bodyLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 16.sp, color = Text1),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = Text1),
    labelLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Text2),
    labelMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Text2),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Text3)
)

object Ui {
    val Bg = Color(0xFF181C21)
    val Bar = Color(0xFF262B2F)
    val Card = Color(0xFF292C31)
    val Chip = Color(0xFF292C31)
    val Line = Color(0xFF363B41)
    val Outline = Color(0xFF454B52)
    val Badge = Color(0x99101418)
    val Text2 = Color(0xFFA7ADB4)
    val Text3 = Color(0xFF7D848C)
    val Accent = Color(0xFF7BCFFF)
    val AccentSoft = Color(0x297BCFFF)
    val OnAccent = Color(0xFF08293B)
    val Pill = Color(0xFFEFEBEF)
    val NavPill = Color(0xFF2B4255)
    val Star = Color(0xFFFFD233)
    val Rank = Color(0xFF6F7078)
    val AccentBrush = Brush.linearGradient(listOf(Color(0xFF8AD6FF), Color(0xFF6BC6F8)))
    val Scrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6181C21)))
}

@Composable
fun KomiktaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}
