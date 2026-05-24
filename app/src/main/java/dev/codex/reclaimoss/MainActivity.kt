package dev.codex.reclaimoss

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.codex.reclaimoss.settings.ThemeMode
import dev.codex.reclaimoss.ui.OpenReclaimApp

private val OpenReclaimLightColors = lightColorScheme(
    primary = Color(0xFF3F5FBF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE6FF),
    onPrimaryContainer = Color(0xFF13265B),
    secondary = Color(0xFF6A8F82),
    secondaryContainer = Color(0xFFE4EFEA),
    onSecondaryContainer = Color(0xFF1B362E),
    background = Color(0xFFF3F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EEF6),
    outline = Color(0xFFC7D2E0),
    outlineVariant = Color(0xFFDCE4EF),
    onSurface = Color(0xFF16202D),
    onSurfaceVariant = Color(0xFF607082),
)

private val OpenReclaimDarkColors = darkColorScheme(
    primary = Color(0xFFB8C7FF),
    onPrimary = Color(0xFF132762),
    primaryContainer = Color(0xFF2A448F),
    onPrimaryContainer = Color(0xFFE3E9FF),
    secondary = Color(0xFFA6CCBE),
    secondaryContainer = Color(0xFF334C44),
    onSecondaryContainer = Color(0xFFE5F4EE),
    background = Color(0xFF1A1C1E),
    surface = Color(0xFF1E2024),
    surfaceVariant = Color(0xFF292D33),
    outline = Color(0xFF8E9199),
    outlineVariant = Color(0xFF43474E),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC3C6CF),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OpenReclaimApplication
        setContent {
            val settings by app.appGraph.appSettingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = dev.codex.reclaimoss.settings.AppSettings(),
            )
            val useDarkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val colorScheme = if (useDarkTheme) OpenReclaimDarkColors else OpenReclaimLightColors
            val view = LocalView.current
            SideEffect {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, view).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
            MaterialTheme(colorScheme = colorScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OpenReclaimApp(app.appGraph)
                }
            }
        }
    }
}
