package com.wootan.ghostcamera.ui

import androidx.compose.ui.layout.ContentScale

enum class GhostScaleMode(
    val preferenceValue: String,
    val label: String,
    val contentScale: ContentScale,
) {
    Fill("fill", "채움", ContentScale.Crop),
    Fit("fit", "맞추기", ContentScale.Fit),
    Stretch("stretch", "늘리기", ContentScale.FillBounds),
    ;

    companion object {
        fun fromPreference(value: String?): GhostScaleMode =
            entries.firstOrNull { it.preferenceValue == value } ?: Fill
    }
}
