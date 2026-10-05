package com.iattend.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test fun everyCustomAccentHasReadableSemanticPairs() {
        AvailableAccents.forEach { accent ->
            listOf(false, true).forEach { dark ->
                val scheme = customColorScheme(accent, dark)
                checkPairs("$accent dark=$dark", scheme)
                if (!dark) checkPairs("$accent light surfaces", scheme.lightSurfaces())
                if (dark) {
                    val amoled = scheme.amoled()
                    checkPairs("$accent AMOLED", amoled)
                    assertEquals(Color.Black, amoled.background)
                    val surfaces = listOf(amoled.surfaceContainerLowest, amoled.surfaceContainerLow,
                        amoled.surfaceContainer, amoled.surfaceContainerHigh, amoled.surfaceContainerHighest)
                    assertEquals(5, surfaces.distinct().size)
                    assertTrue(surfaces.none { it == Color.Black })
                }
            }
        }
    }

    private fun checkPairs(label: String, scheme: ColorScheme) {
        with(scheme) {
            listOf(primary to onPrimary, primaryContainer to onPrimaryContainer,
                secondary to onSecondary, secondaryContainer to onSecondaryContainer,
                tertiary to onTertiary, tertiaryContainer to onTertiaryContainer,
                surfaceContainerHighest to onSurface, surfaceContainerHighest to onSurfaceVariant,
                surfaceContainerHighest to primary, background to onBackground).forEach { (fill, text) ->
                assertTrue("$label: $fill / $text contrast=${colorContrast(fill, text)}", colorContrast(fill, text) >= 4.49f)
            }
            assertTrue("$label control outline", colorContrast(outline, surfaceContainerHighest) >= 2.99f)
        }
    }
}
