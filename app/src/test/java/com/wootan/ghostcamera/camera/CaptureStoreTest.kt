package com.wootan.ghostcamera.camera

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureStoreTest {
    @Test
    fun `additional rotation composes with normal exif orientations`() {
        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_90,
            rotateExifOrientation(ExifInterface.ORIENTATION_NORMAL, 1),
        )
        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_180,
            rotateExifOrientation(ExifInterface.ORIENTATION_ROTATE_90, 1),
        )
        assertEquals(
            ExifInterface.ORIENTATION_ROTATE_270,
            rotateExifOrientation(ExifInterface.ORIENTATION_NORMAL, 3),
        )
    }

    @Test
    fun `additional rotation preserves mirrored exif transformations`() {
        assertEquals(
            ExifInterface.ORIENTATION_TRANSPOSE,
            rotateExifOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 1),
        )
        assertEquals(
            ExifInterface.ORIENTATION_FLIP_VERTICAL,
            rotateExifOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 2),
        )
    }
}
