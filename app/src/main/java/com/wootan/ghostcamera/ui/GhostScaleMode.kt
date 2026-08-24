package com.wootan.ghostcamera.ui

import androidx.compose.ui.layout.ContentScale

enum class GhostScaleMode(
    val preferenceValue: String,
    val label: String,
    val description: String,
    val contentScale: ContentScale,
) {
    Fill("fill", "채움", "화면을 채우고 가장자리는 잘라냅니다", ContentScale.Crop),
    Fit("fit", "맞추기", "사진 전체가 보이도록 맞춥니다", ContentScale.Fit),
    Stretch("stretch", "늘리기", "화면 비율에 맞게 사진을 늘립니다", ContentScale.FillBounds),
    ;

    fun next(): GhostScaleMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromPreference(value: String?): GhostScaleMode =
            entries.firstOrNull { it.preferenceValue == value } ?: Fill
    }
}
