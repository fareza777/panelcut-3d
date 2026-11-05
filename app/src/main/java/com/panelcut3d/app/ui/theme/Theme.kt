package com.panelcut3d.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.panelcut3d.app.R
import com.panelcut3d.app.data.ThemeMode

private val Amber = Color(0xFFE8A33D)
private val AmberSoft = Color(0xFFF6C87E)
private val Ink = Color(0xFF0C0D0F)
private val Bone = Color(0xFFF2EDE4)

private val DarkScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF241603),
    primaryContainer = Color(0xFF4B3413),
    onPrimaryContainer = Color(0xFFFFE0AE),
    secondary = Color(0xFFA3C3B3),
    onSecondary = Color(0xFF10241B),
    secondaryContainer = Color(0xFF24382F),
    onSecondaryContainer = Color(0xFFD3EBDD),
    tertiary = Color(0xFFD5A083),
    onTertiary = Color(0xFF2C1509),
    tertiaryContainer = Color(0xFF4A2A19),
    onTertiaryContainer = Color(0xFFFFDBC8),
    background = Ink,
    onBackground = Bone,
    surface = Color(0xFF101215),
    onSurface = Bone,
    surfaceVariant = Color(0xFF202329),
    onSurfaceVariant = Color(0xFFB4AEA3),
    surfaceTint = Amber,
    inverseSurface = Bone,
    inverseOnSurface = Color(0xFF1B1B18),
    inversePrimary = Color(0xFF8A5A12),
    outline = Color(0xFF4A4D54),
    outlineVariant = Color(0xFF282C32),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF8A5A1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E0C4),
    onPrimaryContainer = Color(0xFF2E1A02),
    secondary = Color(0xFF3E6254),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8EADF),
    onSecondaryContainer = Color(0xFF0D241A),
    tertiary = Color(0xFF96593A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF6DCCB),
    onTertiaryContainer = Color(0xFF341205),
    background = Color(0xFFF5F0E7),
    onBackground = Color(0xFF1C1914),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1C1914),
    surfaceVariant = Color(0xFFEDE4D7),
    onSurfaceVariant = Color(0xFF665D50),
    surfaceTint = Color(0xFF8A5A1E),
    inverseSurface = Color(0xFF312D27),
    inverseOnSurface = Color(0xFFF7F1E8),
    inversePrimary = Color(0xFFF4C681),
    outline = Color(0xFF8B8071),
    outlineVariant = Color(0xFFE1D6C7),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF000000),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val Jakarta = FontFamily(
    Font(
        resId = R.font.pjs_variable,
        weight = FontWeight.Light,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(300)),
    ),
    Font(
        resId = R.font.pjs_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.pjs_variable,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.pjs_variable,
        weight = FontWeight.SemiBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.pjs_variable,
        weight = FontWeight.Bold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        resId = R.font.pjs_variable,
        weight = FontWeight.ExtraBold,
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
)

private val PcTypography = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-1).sp),
    displaySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.6).sp),
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp, letterSpacing = (-0.35).sp),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, lineHeight = 19.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.15.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.35.sp),
)

private val PcShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun PanelCutTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = PcTypography,
        shapes = PcShapes,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
