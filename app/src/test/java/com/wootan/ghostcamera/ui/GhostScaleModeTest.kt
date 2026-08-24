package com.wootan.ghostcamera.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class GhostScaleModeTest {
    @Test
    fun `saved scale modes restore with fill as fallback`() {
        assertEquals(GhostScaleMode.Fit, GhostScaleMode.fromPreference("fit"))
        assertEquals(GhostScaleMode.Stretch, GhostScaleMode.fromPreference("stretch"))
        assertEquals(GhostScaleMode.Fill, GhostScaleMode.fromPreference("unknown"))
        assertEquals(GhostScaleMode.Fill, GhostScaleMode.fromPreference(null))
    }

    @Test
    fun `scale button cycles through every mode`() {
        assertEquals(GhostScaleMode.Fit, GhostScaleMode.Fill.next())
        assertEquals(GhostScaleMode.Stretch, GhostScaleMode.Fit.next())
        assertEquals(GhostScaleMode.Fill, GhostScaleMode.Stretch.next())
    }
}
