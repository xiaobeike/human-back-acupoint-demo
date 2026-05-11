package com.humanacupoints.demo.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PayloadFormatter {
    fun buildPayload(
        template: DemoBackModel.BodyTemplate,
        overlayWidth: Int,
        overlayHeight: Int,
        anchors: AnchorState,
        acupoints: List<AcupointRender>,
    ): String {
        val root = buildBasePayload(template, overlayWidth, overlayHeight, anchors).apply {
            getJSONObject("body_frame").put("acupoint_count", acupoints.size)
        }
        root.put("anchors", JSONObject().apply {
            put("left_shoulder", pointJson(anchors.leftShoulder, overlayWidth, overlayHeight))
            put("right_shoulder", pointJson(anchors.rightShoulder, overlayWidth, overlayHeight))
            put("spine_top", pointJson(anchors.spineTop, overlayWidth, overlayHeight))
            put("spine_bottom", pointJson(anchors.spineBottom, overlayWidth, overlayHeight))
            put("left_waist", pointJson(anchors.leftWaist, overlayWidth, overlayHeight))
            put("right_waist", pointJson(anchors.rightWaist, overlayWidth, overlayHeight))
        })
        root.put("acupoints", JSONArray().apply {
            acupoints.forEach { point ->
                put(acupointJson(point, overlayWidth, overlayHeight))
            }
        })
        return root.toString(2)
    }

    fun buildSingleAcupointPayload(
        template: DemoBackModel.BodyTemplate,
        overlayWidth: Int,
        overlayHeight: Int,
        anchors: AnchorState,
        point: AcupointRender,
    ): String {
        val root = buildBasePayload(template, overlayWidth, overlayHeight, anchors).apply {
            put("dispatch_mode", "single_acupoint")
            getJSONObject("body_frame").put("acupoint_count", 1)
        }
        root.put("anchors", JSONObject().apply {
            put("left_shoulder", pointJson(anchors.leftShoulder, overlayWidth, overlayHeight))
            put("right_shoulder", pointJson(anchors.rightShoulder, overlayWidth, overlayHeight))
            put("spine_top", pointJson(anchors.spineTop, overlayWidth, overlayHeight))
            put("spine_bottom", pointJson(anchors.spineBottom, overlayWidth, overlayHeight))
            put("left_waist", pointJson(anchors.leftWaist, overlayWidth, overlayHeight))
            put("right_waist", pointJson(anchors.rightWaist, overlayWidth, overlayHeight))
        })
        root.put("selected_acupoint", acupointJson(point, overlayWidth, overlayHeight))
        return root.toString(2)
    }

    private fun buildBasePayload(
        template: DemoBackModel.BodyTemplate,
        overlayWidth: Int,
        overlayHeight: Int,
        anchors: AnchorState,
    ): JSONObject {
        val shoulderWidth = anchors.rightShoulder.x - anchors.leftShoulder.x
        val waistWidth = anchors.rightWaist.x - anchors.leftWaist.x
        val spineAxisLength = anchors.spineBottom.y - anchors.spineTop.y
        return JSONObject().apply {
            put("version", 1)
            put("template", template.name.lowercase(Locale.US))
            put("captured_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
            put("frame", JSONObject().apply {
                put("width", overlayWidth)
                put("height", overlayHeight)
                put("space", "overlay_canvas")
            })
            put("body_frame", JSONObject().apply {
                put("anchor_count", 6)
                put("spine_axis_length", spineAxisLength.round3())
                put("shoulder_width", shoulderWidth.round3())
                put("waist_width", waistWidth.round3())
            })
        }
    }

    private fun acupointJson(
        point: AcupointRender,
        overlayWidth: Int,
        overlayHeight: Int,
    ): JSONObject = JSONObject().apply {
        put("id", point.id)
        put("label", point.label)
        put("meridian", point.meridian)
        put("pixel", JSONObject().apply {
            put("x", point.position.x.round3())
            put("y", point.position.y.round3())
        })
        put("normalized", JSONObject().apply {
            put("x", (point.position.x / overlayWidth).round3())
            put("y", (point.position.y / overlayHeight).round3())
        })
        put("body_relative", JSONObject().apply {
            put("vertical_t", point.verticalT.round3())
            put("lateral_t", point.lateralSignedT.round3())
        })
    }

    private fun pointJson(point: android.graphics.PointF, width: Int, height: Int): JSONObject =
        JSONObject().apply {
            put("pixel_x", point.x.round3())
            put("pixel_y", point.y.round3())
            put("normalized_x", (point.x / width).round3())
            put("normalized_y", (point.y / height).round3())
        }

    private fun Float.round3(): Float = ((this * 1000f).toInt()) / 1000f
}
