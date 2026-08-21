package com.wootan.ghostcamera.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyGuideTest {
    @Test
    fun `default guide follows shoulder chest pelvis order`() {
        val positions = BodyGuidePositions.Default

        assertTrue(positions.shoulder < positions.chest)
        assertTrue(positions.chest < positions.pelvis)
        assertTrue(positions.shoulder >= BodyGuidePositions.MinPosition)
        assertTrue(positions.pelvis <= BodyGuidePositions.MaxPosition)
    }

    @Test
    fun `moving a line preserves bounds and minimum gaps`() {
        val positions = BodyGuidePositions.Default

        assertFloatEquals(
            positions.chest - BodyGuidePositions.MinimumGap,
            positions.withPosition(BodyGuideLine.Shoulder, 1f).shoulder,
        )
        assertFloatEquals(
            positions.shoulder + BodyGuidePositions.MinimumGap,
            positions.withPosition(BodyGuideLine.Chest, 0f).chest,
        )
        assertFloatEquals(
            positions.pelvis - BodyGuidePositions.MinimumGap,
            positions.withPosition(BodyGuideLine.Chest, 1f).chest,
        )
        assertFloatEquals(
            positions.chest + BodyGuidePositions.MinimumGap,
            positions.withPosition(BodyGuideLine.Pelvis, 0f).pelvis,
        )
    }

    @Test
    fun `invalid stored values restore to a usable ordered guide`() {
        val nonFinite = BodyGuidePositions.fromStored(
            shoulder = Float.NaN,
            chest = Float.POSITIVE_INFINITY,
            pelvis = Float.NEGATIVE_INFINITY,
        )
        assertEquals(BodyGuidePositions.Default, nonFinite)

        val reversed = BodyGuidePositions.fromStored(
            shoulder = 0.9f,
            chest = 0.2f,
            pelvis = 0.3f,
        )
        assertTrue(reversed.shoulder >= BodyGuidePositions.MinPosition)
        assertTrue(
            reversed.chest - reversed.shoulder + 0.0001f >= BodyGuidePositions.MinimumGap,
        )
        assertTrue(
            reversed.pelvis - reversed.chest + 0.0001f >= BodyGuidePositions.MinimumGap,
        )
        assertTrue(reversed.pelvis <= BodyGuidePositions.MaxPosition)
    }

    @Test
    fun `non finite drag input leaves the selected line unchanged`() {
        val positions = BodyGuidePositions.Default

        assertEquals(
            positions,
            positions.withPosition(BodyGuideLine.Chest, Float.NaN),
        )
    }

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assertEquals(expected.toDouble(), actual.toDouble(), 0.0001)
    }
}
