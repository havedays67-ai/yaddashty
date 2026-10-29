package com.yaddashty.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val PaperLight = Color(0xFFF7F2E8)
val CardLight = Color(0xFFFFFDF8)
val Ink = Color(0xFF1F2933)
val InkSoft = Color(0xFF5B6773)
val Teal = Color(0xFF0F766E)
val TealDeep = Color(0xFF0B4F4A)
val Clay = Color(0xFFC2703D)
val Gold = Color(0xFFD9A441)

val NightBackground = Color(0xFF121614)
val NightCard = Color(0xFF1B211E)
val NightInk = Color(0xFFF1EDE3)
val NightInkSoft = Color(0xFFA7B2AC)
val NightTeal = Color(0xFF5FC9BC)
val NightTealDeep = Color(0xFF9BE3D8)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EDE8),
    onPrimaryContainer = TealDeep,
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E1D3),
    onSecondaryContainer = Color(0xFF6B3A18),
    tertiary = Gold,
    onTertiary = Color(0xFF3A2A05),
    background = PaperLight,
    onBackground = Ink,
    surface = CardLight,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFE8DB),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD6CDBC),
    outlineVariant = Color(0xFFE7DFD0),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = NightTeal,
    onPrimary = Color(0xFF04201D),
    primaryContainer = Color(0xFF20544E),
    onPrimaryContainer = NightTealDeep,
    secondary = Color(0xFFE0A47C),
    onSecondary = Color(0xFF3A1F0A),
    secondaryContainer = Color(0xFF57351B),
    onSecondaryContainer = Color(0xFFF6DECB),
    tertiary = Color(0xFFEBC46A),
    onTertiary = Color(0xFF3A2B06),
    background = NightBackground,
    onBackground = NightInk,
    surface = NightCard,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF283029),
    onSurfaceVariant = NightInkSoft,
    outline = Color(0xFF3C463F),
    outlineVariant = Color(0xFF2C352F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val YaddashtyTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 24.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/** Accent colours offered for note cards, mirrored by the web companion app. */
val NoteAccents: List<Color> = listOf(
    Color(0x00000000),
    Color(0xFFBFE3DD),
    Color(0xFFF6DCC0),
    Color(0xFFF3C9C9),
    Color(0xFFD9E2F2),
    Color(0xFFE8DFF2),
    Color(0xFFF7E7A6),
)

@Composable
fun YaddashtyTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = YaddashtyTypography,
        content = content,
    )
}
