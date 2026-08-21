package com.wootan.ghostcamera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
internal fun BodyGuideOverlay(
    mode: BodyGuideMode,
    positions: BodyGuidePositions,
    onPositionsChange: (BodyGuidePositions) -> Unit,
    onPositionsChangeFinished: (BodyGuidePositions) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (mode == BodyGuideMode.Off) return

    BoxWithConstraints(
        modifier = modifier.semantics {
            contentDescription = if (mode == BodyGuideMode.Editing) {
                "체형 촬영 가이드, 위치 조정 중"
            } else {
                "체형 촬영 가이드"
            }
        },
    ) {
        val density = LocalDensity.current
        val stageHeightPx = with(density) { maxHeight.toPx() }
        val outlineWidth = with(density) { 3.5.dp.toPx() }
        val lineWidth = with(density) { 1.4.dp.toPx() }
        val dashLength = with(density) { 10.dp.toPx() }
        val dashGap = with(density) { 7.dp.toPx() }
        val markerRadius = with(density) { 8.dp.toPx() }
        val markerStroke = with(density) { 1.8.dp.toPx() }
        val guideAlpha = if (mode == BodyGuideMode.Editing) 1f else 0.86f

        Canvas(Modifier.fillMaxSize()) {
            val outline = Color.Black.copy(alpha = 0.76f)
            val teal = GhostTeal.copy(alpha = guideAlpha)
            val amber = CaptureAmber.copy(alpha = guideAlpha)
            val dash = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashGap))

            fun drawGuideLine(start: Offset, end: Offset, color: Color, dashed: Boolean = false) {
                val pathEffect = if (dashed) dash else null
                drawLine(
                    color = outline,
                    start = start,
                    end = end,
                    strokeWidth = outlineWidth,
                    pathEffect = pathEffect,
                )
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = lineWidth,
                    pathEffect = pathEffect,
                )
            }

            drawGuideLine(
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                color = teal,
                dashed = true,
            )

            BodyGuideLine.entries.forEach { line ->
                val y = positions.position(line) * size.height
                drawGuideLine(
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    color = if (line == BodyGuideLine.Chest) amber else teal,
                )
            }

            val chestY = positions.chest * size.height
            listOf(0.38f, 0.62f).forEach { horizontalPosition ->
                val center = Offset(size.width * horizontalPosition, chestY)
                drawCircle(
                    color = outline,
                    radius = markerRadius + markerStroke,
                    center = center,
                    style = Stroke(width = markerStroke * 2f),
                )
                drawCircle(
                    color = amber,
                    radius = markerRadius,
                    center = center,
                    style = Stroke(width = markerStroke),
                )
            }
        }

        if (mode == BodyGuideMode.Editing && stageHeightPx > 0f) {
            BodyGuideLine.entries.forEach { line ->
                GuideLineHandle(
                    line = line,
                    position = positions.position(line),
                    stageHeightPx = stageHeightPx,
                    onPositionChange = { value ->
                        onPositionsChange(positions.withPosition(line, value))
                    },
                    onPositionChangeFinished = { value ->
                        onPositionsChangeFinished(positions.withPosition(line, value))
                    },
                )
            }
        }
    }
}

@Composable
private fun GuideLineHandle(
    line: BodyGuideLine,
    position: Float,
    stageHeightPx: Float,
    onPositionChange: (Float) -> Unit,
    onPositionChangeFinished: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val handleHeight = 48.dp
    val handleHeightPx = with(density) { handleHeight.toPx() }
    val latestPosition by rememberUpdatedState(position)
    val latestOnPositionChange by rememberUpdatedState(onPositionChange)
    val latestOnPositionChangeFinished by rememberUpdatedState(onPositionChangeFinished)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(handleHeight)
            .offset {
                IntOffset(
                    x = 0,
                    y = (position * stageHeightPx - handleHeightPx / 2f).roundToInt(),
                )
            }
            .pointerInput(stageHeightPx) {
                var draggedPosition = latestPosition
                detectVerticalDragGestures(
                    onDragStart = { draggedPosition = latestPosition },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        draggedPosition += dragAmount / stageHeightPx
                        latestOnPositionChange(draggedPosition)
                    },
                    onDragEnd = {
                        latestOnPositionChangeFinished(draggedPosition)
                    },
                    onDragCancel = {
                        latestOnPositionChangeFinished(draggedPosition)
                    },
                )
            }
            .semantics {
                contentDescription = "${line.label} 가이드 위치"
                stateDescription = "${(position * 100).roundToInt()} 퍼센트"
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = position,
                    range = BodyGuidePositions.MinPosition..BodyGuidePositions.MaxPosition,
                )
                setProgress { target ->
                    latestOnPositionChange(target)
                    latestOnPositionChangeFinished(target)
                    true
                }
            },
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            shape = RoundedCornerShape(4.dp),
            color = if (line == BodyGuideLine.Chest) {
                Color(0xE6A56600)
            } else {
                Color(0xE6006F65)
            },
            contentColor = Color.White,
        ) {
            Text(
                text = line.label,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                lineHeight = 13.sp,
            )
        }
    }
}
