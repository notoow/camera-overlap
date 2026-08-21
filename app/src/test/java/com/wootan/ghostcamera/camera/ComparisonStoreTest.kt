package com.wootan.ghostcamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonStoreTest {
    @Test
    fun `bounded size preserves aspect ratio without upscaling`() {
        assertEquals(
            PixelSize(width = 1200, height = 1600),
            boundedSize(3072, 4096, 1200, 1600),
        )
        assertEquals(
            PixelSize(width = 800, height = 600),
            boundedSize(800, 600, 1200, 1600),
        )
    }

    @Test
    fun `landscape image is centered inside portrait panel`() {
        assertEquals(
            PixelRect(left = 0, top = 350, right = 1200, bottom = 1250),
            fitRect(
                sourceWidth = 4,
                sourceHeight = 3,
                targetLeft = 0,
                targetTop = 0,
                targetWidth = 1200,
                targetHeight = 1600,
            ),
        )
    }

    @Test
    fun `right panel offset is included in fitted rectangle`() {
        assertEquals(
            PixelRect(left = 1200, top = 0, right = 2400, bottom = 1600),
            fitRect(
                sourceWidth = 3,
                sourceHeight = 4,
                targetLeft = 1200,
                targetTop = 0,
                targetWidth = 1200,
                targetHeight = 1600,
            ),
        )
    }
}
