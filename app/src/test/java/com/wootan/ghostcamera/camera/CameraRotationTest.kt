package com.wootan.ghostcamera.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraRotationTest {
    @Test
    fun `rotation cycles through four quarter turns`() {
        assertEquals(1, CameraRotation.next(0))
        assertEquals(2, CameraRotation.next(1))
        assertEquals(3, CameraRotation.next(2))
        assertEquals(0, CameraRotation.next(3))
    }

    @Test
    fun `rotation normalizes persisted values`() {
        assertEquals(3, CameraRotation.normalize(-1))
        assertEquals(1, CameraRotation.normalize(5))
        assertEquals(270, CameraRotation.degrees(-1))
    }
}
