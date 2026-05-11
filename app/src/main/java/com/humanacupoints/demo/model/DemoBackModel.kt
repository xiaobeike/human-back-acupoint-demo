package com.humanacupoints.demo.model

import android.graphics.PointF
import kotlin.math.max

object DemoBackModel {
    val specs = listOf(
        AcupointSpec("du14", "大椎", Side.CENTER, 0.02f, 0f),
        AcupointSpec("bl13_l", "左肺俞", Side.LEFT, 0.16f, 0.36f),
        AcupointSpec("bl13_r", "右肺俞", Side.RIGHT, 0.16f, 0.36f),
        AcupointSpec("bl15_l", "左心俞", Side.LEFT, 0.28f, 0.34f),
        AcupointSpec("bl15_r", "右心俞", Side.RIGHT, 0.28f, 0.34f),
        AcupointSpec("bl17_l", "左膈俞", Side.LEFT, 0.40f, 0.33f),
        AcupointSpec("bl17_r", "右膈俞", Side.RIGHT, 0.40f, 0.33f),
        AcupointSpec("bl18_l", "左肝俞", Side.LEFT, 0.50f, 0.31f),
        AcupointSpec("bl18_r", "右肝俞", Side.RIGHT, 0.50f, 0.31f),
        AcupointSpec("bl20_l", "左脾俞", Side.LEFT, 0.60f, 0.29f),
        AcupointSpec("bl20_r", "右脾俞", Side.RIGHT, 0.60f, 0.29f),
        AcupointSpec("bl21_l", "左胃俞", Side.LEFT, 0.70f, 0.28f),
        AcupointSpec("bl21_r", "右胃俞", Side.RIGHT, 0.70f, 0.28f),
        AcupointSpec("bl23_l", "左肾俞", Side.LEFT, 0.84f, 0.26f),
        AcupointSpec("bl23_r", "右肾俞", Side.RIGHT, 0.84f, 0.26f),
    )

    fun defaultAnchors(width: Float, height: Float): AnchorState {
        val topY = height * 0.24f
        val bottomY = height * 0.84f
        val centerX = width / 2f
        val halfShoulder = width * 0.18f
        return AnchorState(
            leftShoulder = PointF(centerX - halfShoulder, topY + height * 0.03f),
            rightShoulder = PointF(centerX + halfShoulder, topY + height * 0.03f),
            spineTop = PointF(centerX, topY),
            spineBottom = PointF(centerX, bottomY),
        )
    }

    fun generateAcupoints(anchorState: AnchorState): List<AcupointRender> {
        val centerDx = anchorState.spineBottom.x - anchorState.spineTop.x
        val centerDy = anchorState.spineBottom.y - anchorState.spineTop.y
        val shoulderWidth = max(anchorState.rightShoulder.x - anchorState.leftShoulder.x, 1f)

        return specs.map { spec ->
            val center = PointF(
                anchorState.spineTop.x + centerDx * spec.verticalT,
                anchorState.spineTop.y + centerDy * spec.verticalT,
            )
            val offset = shoulderWidth * spec.lateralT * 0.5f
            val x = when (spec.side) {
                Side.LEFT -> center.x - offset
                Side.RIGHT -> center.x + offset
                Side.CENTER -> center.x
            }
            AcupointRender(spec.label, PointF(x, center.y))
        }
    }
}
