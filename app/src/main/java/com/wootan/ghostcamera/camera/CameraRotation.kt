package com.wootan.ghostcamera.camera

object CameraRotation {
    private const val QUARTER_TURN_COUNT = 4

    fun normalize(quarterTurns: Int): Int = Math.floorMod(quarterTurns, QUARTER_TURN_COUNT)

    fun next(quarterTurns: Int): Int = normalize(quarterTurns + 1)

    fun degrees(quarterTurns: Int): Int = normalize(quarterTurns) * 90
}
