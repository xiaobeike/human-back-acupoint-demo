package com.humanacupoints.demo.model

import android.graphics.PointF

data class AnchorState(
    val leftShoulder: PointF,
    val rightShoulder: PointF,
    val spineTop: PointF,
    val spineBottom: PointF,
    val leftWaist: PointF,
    val rightWaist: PointF,
)

data class AcupointSpec(
    val id: String,
    val label: String,
    val meridian: String,
    val summary: String,
    val side: Side = Side.CENTER,
    val verticalT: Float,
    val lateralT: Float,
)

data class AcupointRender(
    val id: String,
    val label: String,
    val meridian: String,
    val summary: String,
    val position: PointF,
    val verticalT: Float,
    val lateralSignedT: Float,
)

enum class Side {
    LEFT,
    RIGHT,
    CENTER,
}
