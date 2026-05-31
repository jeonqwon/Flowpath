package dev.codex.reclaimoss.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowpathThemeTest {
    @Test
    fun `scaled typography multiplies configured text sizes`() {
        val scaled = scaledTypography(
            Typography(
                bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                labelLarge = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            ),
            1.3f,
        )

        assertEquals(20.8f, scaled.bodyLarge.fontSize.value, 0.01f)
        assertEquals(15.6f, scaled.labelLarge.fontSize.value, 0.01f)
    }
}
