package dev.codex.reclaimoss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
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
    primary = Color(0xFFAAC0FF),
    onPrimary = Color(0xFF102457),
    primaryContainer = Color(0xFF27418A),
    onPrimaryContainer = Color(0xFFE2E9FF),
    secondary = Color(0xFFA5C8BC),
    secondaryContainer = Color(0xFF29453D),
    onSecondaryContainer = Color(0xFFE4F6EF),
    background = Color(0xFF0F1722),
    surface = Color(0xFF151E2A),
    surfaceVariant = Color(0xFF1D2836),
    outline = Color(0xFF445365),
    outlineVariant = Color(0xFF2D3847),
    onSurface = Color(0xFFF2F5FA),
    onSurfaceVariant = Color(0xFFAEBBCA),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OpenReclaimApplication
        setContent {
            val colorScheme = if (isSystemInDarkTheme()) OpenReclaimDarkColors else OpenReclaimLightColors
            MaterialTheme(colorScheme = colorScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OpenReclaimApp(app.appGraph)
                }
            }
        }
    }
}
