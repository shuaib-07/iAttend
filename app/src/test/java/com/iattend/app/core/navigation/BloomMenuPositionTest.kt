package com.iattend.app.core.navigation

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class BloomMenuPositionTest {
    @Test fun panelTracksTriggerAndStaysWithinWindow() {
        val window = IntSize(400, 800)
        val panel = IntSize(360, 256)
        assertEquals(IntOffset(24, 440), bloomMenuPosition(IntRect(312, 704, 376, 768), window, panel, 16))
        assertEquals(IntOffset(16, 440), bloomMenuPosition(IntRect(16, 704, 80, 768), window, panel, 16))
        assertEquals(IntOffset(20, 440), bloomMenuPosition(IntRect(168, 704, 232,768), window, panel, 16))
        assertEquals(IntOffset(20, 16), bloomMenuPosition(IntRect(168, 16, 232, 80), window, panel, 16))
        assertEquals(IntOffset(424, 32), bloomMenuPosition(IntRect(704, 296, 768, 360), IntSize(800, 400), panel, 16))
    }
}
