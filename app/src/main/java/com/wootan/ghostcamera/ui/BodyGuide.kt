package com.wootan.ghostcamera.ui

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
        return when (line) {
            BodyGuideLine.Shoulder -> copy(
                shoulder = finiteValue.coerceWithin(MinPosition, chest - MinimumGap),
            )
            BodyGuideLine.Chest -> copy(
                chest = finiteValue.coerceWithin(
                    shoulder + MinimumGap,
                    pelvis - MinimumGap,
                ),
            )
            BodyGuideLine.Pelvis -> copy(
                pelvis = finiteValue.coerceWithin(chest + MinimumGap, MaxPosition),
            )
        }
    }

    companion object {
        const val MinPosition = 0.12f
        const val MaxPosition = 0.88f
        const val MinimumGap = 0.10f

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
            val safeShoulder = shoulder.finiteOr(Default.shoulder)
                .coerceWithin(MinPosition, MaxPosition - MinimumGap * 2)
            val safeChest = chest.finiteOr(Default.chest)
                .coerceWithin(safeShoulder + MinimumGap, MaxPosition - MinimumGap)
            val safePelvis = pelvis.finiteOr(Default.pelvis)
                .coerceWithin(safeChest + MinimumGap, MaxPosition)
            return BodyGuidePositions(safeShoulder, safeChest, safePelvis)
        }

        private fun Float.finiteOr(fallback: Float): Float =
            takeIf { it.isFinite() } ?: fallback
    }
}

private fun Float.coerceWithin(minimum: Float, maximum: Float): Float =
    if (minimum <= maximum) coerceIn(minimum, maximum) else maximum
