package com.wootan.ghostcamera.ui

import kotlin.math.roundToInt

enum class BodyGuideMode {
    Off,
    Locked,
    Editing,
}

enum class BodyGuideLine(val label: String) {
    Shoulder("어깨"),
    Chest("가슴"),
    Pelvis("골반"),
}

data class BodyGuidePositions(
    val shoulder: Float,
    val chest: Float,
    val pelvis: Float,
) {
    fun position(line: BodyGuideLine): Float = when (line) {
        BodyGuideLine.Shoulder -> shoulder
        BodyGuideLine.Chest -> chest
        BodyGuideLine.Pelvis -> pelvis
    }

    fun withPosition(line: BodyGuideLine, value: Float): BodyGuidePositions {
        val finiteValue = value.takeIf { it.isFinite() } ?: position(line)
        val snappedValue = snapPosition(finiteValue)
        return when (line) {
            BodyGuideLine.Shoulder -> copy(
                shoulder = snappedValue.coerceWithin(MinPosition, chest - MinimumGap),
            )
            BodyGuideLine.Chest -> copy(
                chest = snappedValue.coerceWithin(
                    shoulder + MinimumGap,
                    pelvis - MinimumGap,
                ),
            )
            BodyGuideLine.Pelvis -> copy(
                pelvis = snappedValue.coerceWithin(chest + MinimumGap, MaxPosition),
            )
        }
    }

    companion object {
        const val MinPosition = 0.12f
        const val MaxPosition = 0.88f
        const val MinimumGap = 0.10f
        const val PositionStep = 0.005f

        val Default = BodyGuidePositions(
            shoulder = 0.28f,
            chest = 0.44f,
            pelvis = 0.70f,
        )

        fun fromStored(
            shoulder: Float,
            chest: Float,
            pelvis: Float,
        ): BodyGuidePositions {
            val safeShoulder = snapPosition(shoulder.finiteOr(Default.shoulder))
                .coerceWithin(MinPosition, MaxPosition - MinimumGap * 2)
            val safeChest = snapPosition(chest.finiteOr(Default.chest))
                .coerceWithin(safeShoulder + MinimumGap, MaxPosition - MinimumGap)
            val safePelvis = snapPosition(pelvis.finiteOr(Default.pelvis))
                .coerceWithin(safeChest + MinimumGap, MaxPosition)
            return BodyGuidePositions(safeShoulder, safeChest, safePelvis)
        }

        internal fun snapPosition(value: Float): Float =
            (value / PositionStep).roundToInt() * PositionStep

        private fun Float.finiteOr(fallback: Float): Float =
            takeIf { it.isFinite() } ?: fallback
    }
}

private fun Float.coerceWithin(minimum: Float, maximum: Float): Float =
    if (minimum <= maximum) coerceIn(minimum, maximum) else maximum
