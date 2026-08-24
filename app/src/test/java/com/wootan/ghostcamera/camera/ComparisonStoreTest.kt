package com.wootan.ghostcamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonStoreTest {
    @Test
    fun `decode bounds swap for odd quarter turns`() {
        assertEquals(
            PixelSize(width = 1200, height = 1600),
            preRotationDecodeBounds(1200, 1600, 0),
        )
        assertEquals(
            PixelSize(width = 1600, height = 1200),
            preRotationDecodeBounds(1200, 1600, 1),
        )
        assertEquals(
            PixelSize(width = 1200, height = 1600),
            preRotationDecodeBounds(1200, 1600, 2),
        )
        assertEquals(
            PixelSize(width = 1600, height = 1200),
            preRotationDecodeBounds(1200, 1600, 3),
        )
    }

    @Test
    fun `covering size preserves quality needed for a filled panel`() {
        assertEquals(
            PixelSize(width = 1200, height = 1600),
            coveringSize(3072, 4096, 1200, 1600),
        )
        assertEquals(
            PixelSize(width = 2133, height = 1600),
            coveringSize(4000, 3000, 1200, 1600),
        )
        assertEquals(
            PixelSize(width = 800, height = 600),
            coveringSize(800, 600, 1200, 1600),
        )
    }

    @Test
    fun `landscape image is center cropped to portrait panel ratio`() {
        assertEquals(
            PixelRect(left = 875, top = 0, right = 3125, bottom = 3000),
            centerCropRect(
                sourceWidth = 4000,
                sourceHeight = 3000,
                targetWidth = 1200,
                targetHeight = 1600,
            ),
        )
    }

    @Test
    fun `matching portrait image uses its full frame`() {
        assertEquals(
            PixelRect(left = 0, top = 0, right = 3000, bottom = 4000),
            centerCropRect(
                sourceWidth = 3000,
                sourceHeight = 4000,
                targetWidth = 1200,
                targetHeight = 1600,
            ),
        )
    }
}
