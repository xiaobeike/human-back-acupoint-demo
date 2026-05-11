package com.humanacupoints.demo.model

import android.graphics.PointF

data class AnchorState(
    val leftShoulder: PointF,
    val rightShoulder: PointF,
    val spineTop: PointF,
    val spineBottom: PointF,
)

data class AcupointSpec(
    val id: String,
    val label: String,
    val side: Side = Side.CENTER,
    val verticalT: Float,
    val lateralT: Float,
)

data class AcupointRender(
    val label: String,
    val position: PointF,
)

enum class Side {
    LEFT,
    RIGHT,
    CENTER,
}
