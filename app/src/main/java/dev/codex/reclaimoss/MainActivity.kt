package dev.codex.reclaimoss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.codex.reclaimoss.ui.theme.FlowpathTheme
import dev.codex.reclaimoss.ui.OpenReclaimApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as OpenReclaimApplication
        setContent {
            val settings by app.appGraph.appSettingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = dev.codex.reclaimoss.settings.AppSettings(),
            )
            FlowpathTheme(
                themeMode = settings.themeMode,
                fontSizeScale = settings.fontSizeScale,
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OpenReclaimApp(app.appGraph)
                }
            }
        }
    }
}
