package com.komikta.ui.theme

import com.komikta.R
import com.komikta.ui.components.RoleClockDriver
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
private val Primary = Color.White
private val Text1 = Color.White
private val Text2 = Color(0xFFA7ADB4)
private val Text3 = Color(0xFF7D848C)

private val Scheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFFD9D9DD),
    background = Bg,
    onBackground = Text1,
    surface = Surface,
    onSurface = Text1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Text2,
    outline = Color(0xFF454B52)
)

/** Satu-satunya font aplikasi. Dipakai juga oleh teks yang membuat TextStyle sendiri (mis. nama role). */
val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_semibold, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.Bold)
)

private val Base = Typography()

// Semua style (termasuk yang tidak diatur manual, mis. titleSmall/bodySmall) dipaksa Poppins.
private val Type = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = Poppins),
    displayMedium = Base.displayMedium.copy(fontFamily = Poppins),
    displaySmall = Base.displaySmall.copy(fontFamily = Poppins),
    headlineLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 27.sp, color = Text1),
    headlineMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Text1),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Poppins),
    titleLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Text1),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Text1),
    titleSmall = Base.titleSmall.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = Text1),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Text1),
    bodySmall = Base.bodySmall.copy(fontFamily = Poppins),
    labelLarge = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Text2),
    labelMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Text2),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 11.sp, color = Text3)
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
    val Accent = Color.White
    val AccentSoft = Color(0x29FFFFFF)
    val OnAccent = Color(0xFF111111)
    val Pill = Color(0xFFEFEBEF)
    val NavPill = Color(0xFF3A3A3F)
    val Star = Color(0xFFFFD233)
    val Rank = Color(0xFF6F7078)
    val AccentBrush = Brush.linearGradient(listOf(Color.White, Color(0xFFE4E4E8)))
    val Scrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6181C21)))
}

@Composable
fun KomiktaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type) {
        // Teks yang tidak memakai style tema (hanya fontSize/fontWeight) tetap Poppins, bukan font sistem.
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = Poppins))) {
            RoleClockDriver()
            content()
        }
    }
}
