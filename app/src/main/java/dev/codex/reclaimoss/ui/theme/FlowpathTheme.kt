package dev.codex.reclaimoss.ui.theme

import android.os.Build
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.codex.reclaimoss.R
import dev.codex.reclaimoss.settings.FontSizeScale
import dev.codex.reclaimoss.settings.ThemeMode

// Drop a pixel font TTF into app/src/main/res/font/ named pixel_font.ttf,
// then uncomment the lines below and remove the FontFamily.Monospace fallback.
//
private val PixelFont = FontFamily(
    Font(R.font.press_start2p_regular, FontWeight.Normal),
    Font(R.font.press_start2p_regular, FontWeight.Bold),
)

// ── Light (cream / parchment) ────────────────────────────────────────────────

private val LightCream       = Color(0xFFF5EDD9)
private val LightCreamSurface = Color(0xFFEDE4C6)
private val LightInk         = Color(0xFF1A1714)
private val LightInkMuted    = Color(0xFF4A4540)
private val LightSage        = Color(0xFF6B7C5E)
private val LightSageLight   = Color(0xFFB5C4A8)
private val LightBorder      = Color(0xFF1A1714)
private val LightBorderSoft  = Color(0xFFCBC2A8)

private val FlowpathLightColors = lightColorScheme(
    primary              = LightSage,
    onPrimary            = Color(0xFFF5EDD9),
    primaryContainer     = Color(0xFFD4E0C8),
    onPrimaryContainer   = Color(0xFF1E2E18),
    secondary            = Color(0xFF8A7B5E),
    onSecondary          = LightCream,
    secondaryContainer   = Color(0xFFE8DFC8),
    onSecondaryContainer = Color(0xFF2E2418),
    background           = LightCream,
    onBackground         = LightInk,
    surface              = LightCreamSurface,
    onSurface            = LightInk,
    surfaceVariant       = Color(0xFFE4D9BF),
    onSurfaceVariant     = LightInkMuted,
    outline              = LightBorder,
    outlineVariant       = LightBorderSoft,
    error                = Color(0xFF8B2020),
    onError              = LightCream,
)

// ── Dark (dark parchment / aged paper) ──────────────────────────────────────

private val DarkPaper   = Color(0xFF1C1A14)
private val DarkSurface = Color(0xFF252218)
private val DarkInk     = Color(0xFFF0E8D0)
private val DarkMuted   = Color(0xFFA89C80)
private val DarkSage    = Color(0xFF8FAF7E)
private val DarkBorder  = Color(0xFFF0E8D0)
private val DarkBorderSoft = Color(0xFF4A4230)

private val FlowpathDarkColors = darkColorScheme(
    primary              = DarkSage,
    onPrimary            = Color(0xFF1A2A14),
    primaryContainer     = Color(0xFF3A5030),
    onPrimaryContainer   = Color(0xFFCCE8BB),
    secondary            = Color(0xFFC4A86A),
    onSecondary          = Color(0xFF2A1E08),
    secondaryContainer   = Color(0xFF3A2E14),
    onSecondaryContainer = Color(0xFFE8D4A8),
    background           = DarkPaper,
    onBackground         = DarkInk,
    surface              = DarkSurface,
    onSurface            = DarkInk,
    surfaceVariant       = Color(0xFF302C20),
    onSurfaceVariant     = DarkMuted,
    outline              = DarkBorder,
    outlineVariant       = DarkBorderSoft,
    error                = Color(0xFFCF6679),
    onError              = Color(0xFF1A0A0E),
)

// ── Typography ───────────────────────────────────────────────────────────────

private val FlowpathTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 1.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 1.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.8.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.8.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
    ),
)

// ── Shapes (square — pixel art has no rounding) ──────────────────────────────

private val FlowpathShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small      = RoundedCornerShape(0.dp),
    medium     = RoundedCornerShape(0.dp),
    large      = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

// ── Scale helper ─────────────────────────────────────────────────────────────

internal fun scaledTypography(base: Typography, scaleFactor: Float): Typography = base.copy(
    displayLarge  = base.displayLarge.scaledBy(scaleFactor),
    displayMedium = base.displayMedium.scaledBy(scaleFactor),
    displaySmall  = base.displaySmall.scaledBy(scaleFactor),
    headlineLarge  = base.headlineLarge.scaledBy(scaleFactor),
    headlineMedium = base.headlineMedium.scaledBy(scaleFactor),
    headlineSmall  = base.headlineSmall.scaledBy(scaleFactor),
    titleLarge  = base.titleLarge.scaledBy(scaleFactor),
    titleMedium = base.titleMedium.scaledBy(scaleFactor),
    titleSmall  = base.titleSmall.scaledBy(scaleFactor),
    bodyLarge   = base.bodyLarge.scaledBy(scaleFactor),
    bodyMedium  = base.bodyMedium.scaledBy(scaleFactor),
    bodySmall   = base.bodySmall.scaledBy(scaleFactor),
    labelLarge  = base.labelLarge.scaledBy(scaleFactor),
    labelMedium = base.labelMedium.scaledBy(scaleFactor),
    labelSmall  = base.labelSmall.scaledBy(scaleFactor),
)

private fun TextStyle.scaledBy(factor: Float): TextStyle = copy(
    fontSize      = fontSize.scaledBy(factor),
    lineHeight    = lineHeight.scaledBy(factor),
    letterSpacing = letterSpacing.scaledBy(factor),
)

private fun TextUnit.scaledBy(factor: Float): TextUnit =
    if (this == TextUnit.Unspecified) this else (value * factor).sp

// ── Theme entry point ─────────────────────────────────────────────────────────

@Composable
fun FlowpathTheme(
    themeMode: ThemeMode,
    fontSizeScale: FontSizeScale,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val colorScheme = if (useDarkTheme) FlowpathDarkColors else FlowpathLightColors
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        window.statusBarColor     = colorScheme.background.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced     = false
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars     = !useDarkTheme
            isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = scaledTypography(FlowpathTypography, fontSizeScale.scaleFactor),
        shapes      = FlowpathShapes,
        content     = content,
    )
}
