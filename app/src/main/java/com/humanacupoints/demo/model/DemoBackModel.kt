package com.humanacupoints.demo.model

import android.graphics.PointF
import kotlin.math.max

object DemoBackModel {
    enum class BodyTemplate(
        val label: String,
        val shoulderScale: Float,
        val waistScale: Float,
    ) {
        SLIM("偏瘦", 0.16f, 0.10f),
        STANDARD("标准", 0.18f, 0.12f),
        BROAD("偏宽", 0.21f, 0.145f),
    }

    val specs = listOf(
        AcupointSpec("du13", "陶道", "督脉", "位于上背部中线，可帮助演示锁定后中线点如何联动确定。", Side.CENTER, 0.10f, 0f),
        AcupointSpec("du14", "大椎", "督脉", "颈胸交界标志点，适合作为上端校准参考。", Side.CENTER, 0.02f, 0f),
        AcupointSpec("bl11_l", "左大杼", "足太阳膀胱经", "上背肩胛内侧参考点，适合验证肩线校准。", Side.LEFT, 0.12f, 0.40f),
        AcupointSpec("bl11_r", "右大杼", "足太阳膀胱经", "上背肩胛内侧参考点，适合验证肩线校准。", Side.RIGHT, 0.12f, 0.40f),
        AcupointSpec("bl12_l", "左风门", "足太阳膀胱经", "靠近上背肩胛区域，适合作为上背部扩展示例。", Side.LEFT, 0.14f, 0.38f),
        AcupointSpec("bl12_r", "右风门", "足太阳膀胱经", "靠近上背肩胛区域，适合作为上背部扩展示例。", Side.RIGHT, 0.14f, 0.38f),
        AcupointSpec("bl13_l", "左肺俞", "足太阳膀胱经", "常用于上背部穴位展示和对照说明。", Side.LEFT, 0.16f, 0.36f),
        AcupointSpec("bl13_r", "右肺俞", "足太阳膀胱经", "常用于上背部穴位展示和对照说明。", Side.RIGHT, 0.16f, 0.36f),
        AcupointSpec("bl14_l", "左厥阴俞", "足太阳膀胱经", "位于肺俞与心俞之间，便于补齐中上背部序列。", Side.LEFT, 0.22f, 0.35f),
        AcupointSpec("bl14_r", "右厥阴俞", "足太阳膀胱经", "位于肺俞与心俞之间，便于补齐中上背部序列。", Side.RIGHT, 0.22f, 0.35f),
        AcupointSpec("bl15_l", "左心俞", "足太阳膀胱经", "用于胸背区域重点点位讲解。", Side.LEFT, 0.28f, 0.34f),
        AcupointSpec("bl15_r", "右心俞", "足太阳膀胱经", "用于胸背区域重点点位讲解。", Side.RIGHT, 0.28f, 0.34f),
        AcupointSpec("du11", "神道", "督脉", "胸段中线上点位，可用于说明督脉与膀胱经的相对关系。", Side.CENTER, 0.26f, 0f),
        AcupointSpec("bl17_l", "左膈俞", "足太阳膀胱经", "背部中段横向校准后更容易观察对称性。", Side.LEFT, 0.40f, 0.33f),
        AcupointSpec("bl17_r", "右膈俞", "足太阳膀胱经", "背部中段横向校准后更容易观察对称性。", Side.RIGHT, 0.40f, 0.33f),
        AcupointSpec("bl18_l", "左肝俞", "足太阳膀胱经", "适合做左右对照演示。", Side.LEFT, 0.50f, 0.31f),
        AcupointSpec("bl18_r", "右肝俞", "足太阳膀胱经", "适合做左右对照演示。", Side.RIGHT, 0.50f, 0.31f),
        AcupointSpec("bl19_l", "左胆俞", "足太阳膀胱经", "中下背部过渡点，可用于扩展俞穴序列。", Side.LEFT, 0.56f, 0.30f),
        AcupointSpec("bl19_r", "右胆俞", "足太阳膀胱经", "中下背部过渡点，可用于扩展俞穴序列。", Side.RIGHT, 0.56f, 0.30f),
        AcupointSpec("bl20_l", "左脾俞", "足太阳膀胱经", "用于下胸段到上腰段过渡展示。", Side.LEFT, 0.60f, 0.29f),
        AcupointSpec("bl20_r", "右脾俞", "足太阳膀胱经", "用于下胸段到上腰段过渡展示。", Side.RIGHT, 0.60f, 0.29f),
        AcupointSpec("bl21_l", "左胃俞", "足太阳膀胱经", "可配合腰背保健场景说明。", Side.LEFT, 0.70f, 0.28f),
        AcupointSpec("bl21_r", "右胃俞", "足太阳膀胱经", "可配合腰背保健场景说明。", Side.RIGHT, 0.70f, 0.28f),
        AcupointSpec("bl22_l", "左三焦俞", "足太阳膀胱经", "进入腰段后仍沿相同纵轴和横向比例生成。", Side.LEFT, 0.76f, 0.27f),
        AcupointSpec("bl22_r", "右三焦俞", "足太阳膀胱经", "进入腰段后仍沿相同纵轴和横向比例生成。", Side.RIGHT, 0.76f, 0.27f),
        AcupointSpec("bl23_l", "左肾俞", "足太阳膀胱经", "靠近腰段，适合用于下背部点位展示。", Side.LEFT, 0.84f, 0.26f),
        AcupointSpec("bl23_r", "右肾俞", "足太阳膀胱经", "靠近腰段，适合用于下背部点位展示。", Side.RIGHT, 0.84f, 0.26f),
        AcupointSpec("du4", "命门", "督脉", "腰段中线参考点，可用于中线点位展示。", Side.CENTER, 0.86f, 0f),
        AcupointSpec("bl25_l", "左大肠俞", "足太阳膀胱经", "下腰段扩展示例，有助于验证锁定后整条序列连续性。", Side.LEFT, 0.92f, 0.25f),
        AcupointSpec("bl25_r", "右大肠俞", "足太阳膀胱经", "下腰段扩展示例，有助于验证锁定后整条序列连续性。", Side.RIGHT, 0.92f, 0.25f),
    )

    fun defaultAnchors(
        width: Float,
        height: Float,
        template: BodyTemplate = BodyTemplate.STANDARD,
    ): AnchorState {
        val topY = height * 0.24f
        val bottomY = height * 0.84f
        val centerX = width / 2f
        val halfShoulder = width * template.shoulderScale
        return AnchorState(
            leftShoulder = PointF(centerX - halfShoulder, topY + height * 0.03f),
            rightShoulder = PointF(centerX + halfShoulder, topY + height * 0.03f),
            spineTop = PointF(centerX, topY),
            spineBottom = PointF(centerX, bottomY),
            leftWaist = PointF(centerX - width * template.waistScale, height * 0.60f),
            rightWaist = PointF(centerX + width * template.waistScale, height * 0.60f),
        )
    }

    fun generateAcupoints(anchorState: AnchorState): List<AcupointRender> {
        val centerDx = anchorState.spineBottom.x - anchorState.spineTop.x
        val centerDy = anchorState.spineBottom.y - anchorState.spineTop.y
        val shoulderWidth = max(anchorState.rightShoulder.x - anchorState.leftShoulder.x, 1f)
        val waistWidth = max(anchorState.rightWaist.x - anchorState.leftWaist.x, 1f)

        return specs.map { spec ->
            val center = PointF(
                anchorState.spineTop.x + centerDx * spec.verticalT,
                anchorState.spineTop.y + centerDy * spec.verticalT,
            )
            val blendWidth = shoulderWidth * (1f - spec.verticalT) + waistWidth * spec.verticalT
            val offset = blendWidth * spec.lateralT * 0.5f
            val x = when (spec.side) {
                Side.LEFT -> center.x - offset
                Side.RIGHT -> center.x + offset
                Side.CENTER -> center.x
            }
            val lateralSignedT = when (spec.side) {
                Side.LEFT -> -spec.lateralT
                Side.RIGHT -> spec.lateralT
                Side.CENTER -> 0f
            }
            AcupointRender(
                spec.id,
                spec.label,
                spec.meridian,
                spec.summary,
                PointF(x, center.y),
                spec.verticalT,
                lateralSignedT,
            )
        }
    }
}
