package com.humanacupoints.demo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.humanacupoints.demo.R
import com.humanacupoints.demo.model.AcupointRender
import com.humanacupoints.demo.model.AnchorState
import com.humanacupoints.demo.model.DemoBackModel
import kotlin.math.abs
import kotlin.math.hypot

class BackOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onAcupointSelected(point: AcupointRender)
        fun onAnchorChanged(anchorLabel: String)
    }

    enum class CalibrationStep {
        SHOULDERS,
        SPINE,
        BODY_WIDTH,
        LOCKED,
    }

    private val accentColor = ContextCompat.getColor(context, R.color.accent)
    private val accentSecondary = ContextCompat.getColor(context, R.color.accent_secondary)
    private val dangerColor = ContextCompat.getColor(context, R.color.danger_zone)
    private val textPrimary = ContextCompat.getColor(context, R.color.text_primary)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentSecondary
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }

    private val silhouettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x223CC7B8
        style = Paint.Style.FILL
    }

    private val silhouetteStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentSecondary
        strokeWidth = 3f
        style = Paint.Style.STROKE
        alpha = 210
    }

    private val centerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        alpha = 180
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    private val anchorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor
        style = Paint.Style.FILL
    }

    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentSecondary
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary
        textSize = 32f
    }

    private val labelBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAA11151B.toInt()
        style = Paint.Style.FILL
    }

    private val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = dangerColor
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 160
    }

    private val helperTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary
        textSize = 28f
    }

    private val guideTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textPrimary
        textSize = 24f
        alpha = 210
    }

    private var anchors: AnchorState? = null
    private var frozen = false
    private var selectedAnchor: AnchorHandle? = null
    private var selectedAcupointId: String? = null
    private var cachedAcupoints: List<AcupointRender> = emptyList()
    private var currentTemplate = DemoBackModel.BodyTemplate.STANDARD
    private var calibrationStep = CalibrationStep.SHOULDERS
    private var showAcupoints = false
    var listener: Listener? = null

    fun setFrozen(value: Boolean) {
        frozen = value
        invalidate()
    }

    fun setShowAcupoints(value: Boolean) {
        showAcupoints = value
        invalidate()
    }

    fun setCalibrationStep(step: CalibrationStep) {
        calibrationStep = step
        if (step == CalibrationStep.LOCKED) {
            selectedAnchor = null
        }
        invalidate()
    }

    fun applyTemplate(template: DemoBackModel.BodyTemplate) {
        currentTemplate = template
        if (width > 0 && height > 0) {
            anchors = DemoBackModel.defaultAnchors(width.toFloat(), height.toFloat(), template)
            invalidate()
        }
    }

    fun resetAnchors() {
        if (width > 0 && height > 0) {
            anchors = DemoBackModel.defaultAnchors(width.toFloat(), height.toFloat(), currentTemplate)
            invalidate()
        }
    }

    fun getCurrentAnchors(): AnchorState? = anchors

    fun getCurrentAcupoints(): List<AcupointRender> = cachedAcupoints

    fun selectAcupoint(pointId: String?) {
        selectedAcupointId = pointId
        invalidate()
    }

    fun clearSelectedAcupoint() {
        selectedAcupointId = null
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (anchors == null && w > 0 && h > 0) {
            anchors = DemoBackModel.defaultAnchors(w.toFloat(), h.toFloat(), currentTemplate)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val state = anchors ?: return

        drawBodyGuide(canvas, state)
        cachedAcupoints = DemoBackModel.generateAcupoints(state)
        if (showAcupoints) {
            cachedAcupoints.forEach { drawAcupoint(canvas, it) }
        }
        if (frozen && calibrationStep != CalibrationStep.LOCKED) {
            drawAnchors(canvas, state)
        }
        if (frozen) {
            drawStepOverlay(canvas, state)
        }
    }

    private fun drawBodyGuide(canvas: Canvas, state: AnchorState) {
        drawSilhouette(canvas, state)
        canvas.drawLine(
            state.leftShoulder.x,
            state.leftShoulder.y,
            state.rightShoulder.x,
            state.rightShoulder.y,
            linePaint,
        )
        canvas.drawLine(
            state.spineTop.x,
            state.spineTop.y,
            state.spineBottom.x,
            state.spineBottom.y,
            centerLinePaint,
        )
        canvas.drawLine(
            state.leftWaist.x,
            state.leftWaist.y,
            state.rightWaist.x,
            state.rightWaist.y,
            linePaint,
        )

        val shoulderRect = RectF(
            state.leftShoulder.x,
            state.spineTop.y + 16f,
            state.rightShoulder.x,
            state.spineBottom.y,
        )
        canvas.drawRoundRect(shoulderRect, 28f, 28f, dangerPaint)
        drawLandmarkLabels(canvas, state)
    }

    private fun drawSilhouette(canvas: Canvas, state: AnchorState) {
        val leftHip = PointF(
            state.leftWaist.x - (state.leftShoulder.x - state.leftWaist.x) * 0.12f,
            state.spineBottom.y - 18f,
        )
        val rightHip = PointF(
            state.rightWaist.x + (state.rightWaist.x - state.rightShoulder.x) * 0.12f,
            state.spineBottom.y - 18f,
        )
        val neckLeft = PointF(state.spineTop.x - 34f, state.spineTop.y - 10f)
        val neckRight = PointF(state.spineTop.x + 34f, state.spineTop.y - 10f)

        val path = Path().apply {
            moveTo(neckLeft.x, neckLeft.y)
            quadTo(state.leftShoulder.x, state.leftShoulder.y - 14f, state.leftShoulder.x, state.leftShoulder.y)
            quadTo(state.leftWaist.x - 18f, (state.leftShoulder.y + state.leftWaist.y) / 2f, state.leftWaist.x, state.leftWaist.y)
            quadTo(leftHip.x, leftHip.y - 28f, leftHip.x, leftHip.y)
            lineTo(rightHip.x, rightHip.y)
            quadTo(rightHip.x, rightHip.y - 28f, state.rightWaist.x, state.rightWaist.y)
            quadTo(state.rightWaist.x + 18f, (state.rightShoulder.y + state.rightWaist.y) / 2f, state.rightShoulder.x, state.rightShoulder.y)
            quadTo(state.rightShoulder.x, state.rightShoulder.y - 14f, neckRight.x, neckRight.y)
            close()
        }
        canvas.drawPath(path, silhouettePaint)
        canvas.drawPath(path, silhouetteStrokePaint)
    }

    private fun drawLandmarkLabels(canvas: Canvas, state: AnchorState) {
        drawGuideLabel(canvas, "C7 / 大椎参考", state.spineTop.x + 22f, state.spineTop.y - 18f)
        drawGuideLabel(canvas, "肩峰连线", (state.leftShoulder.x + state.rightShoulder.x) / 2f - 40f, state.leftShoulder.y - 16f)
        val scapulaY = state.spineTop.y + (state.spineBottom.y - state.spineTop.y) * 0.32f
        drawGuideLabel(canvas, "肩胛参考区", state.rightShoulder.x + 18f, scapulaY)
        drawGuideLabel(canvas, "腰侧宽度", state.rightWaist.x + 18f, state.rightWaist.y)
        drawGuideLabel(canvas, "脊柱中线", state.spineBottom.x + 20f, (state.spineTop.y + state.spineBottom.y) / 2f)
    }

    private fun drawGuideLabel(canvas: Canvas, label: String, x: Float, y: Float) {
        val textWidth = guideTextPaint.measureText(label)
        val rect = RectF(x, y - 26f, x + textWidth + 20f, y + 12f)
        canvas.drawRoundRect(rect, 12f, 12f, labelBackgroundPaint)
        canvas.drawText(label, rect.left + 10f, rect.bottom - 10f, guideTextPaint)
    }

    private fun drawAcupoint(canvas: Canvas, point: AcupointRender) {
        val isSelected = point.id == selectedAcupointId
        canvas.drawCircle(point.position.x, point.position.y, if (isSelected) 18f else 12f, if (isSelected) selectedPointPaint else pointPaint)
        if (!isSelected) {
            return
        }
        val textWidth = textPaint.measureText(point.label)
        val left = point.position.x + 18f
        val top = point.position.y - 38f
        val rect = RectF(left, top, left + textWidth + 24f, top + 42f)
        canvas.drawRoundRect(rect, 16f, 16f, labelBackgroundPaint)
        canvas.drawText(point.label, left + 12f, top + 29f, textPaint)
    }

    private fun drawAnchors(canvas: Canvas, state: AnchorState) {
        anchorHandlesForCurrentStep(state).forEach { point ->
            canvas.drawCircle(point.x, point.y, 18f, anchorPaint)
            canvas.drawCircle(point.x, point.y, 24f, centerLinePaint)
        }
        selectedAnchor?.let { anchor ->
            val point = anchor.resolve(state)
            val label = anchor.label
            val textWidth = helperTextPaint.measureText(label)
            val rect = RectF(point.x + 20f, point.y - 54f, point.x + textWidth + 44f, point.y - 8f)
            canvas.drawRoundRect(rect, 14f, 14f, labelBackgroundPaint)
            canvas.drawText(label, rect.left + 12f, rect.bottom - 14f, helperTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!frozen) {
            if (!showAcupoints) return false
            return handlePointTap(event)
        }
        if (calibrationStep == CalibrationStep.LOCKED) {
            if (!showAcupoints) return false
            return handlePointTap(event)
        }
        val state = anchors ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selectedAnchor = pickHandle(state, event.x, event.y)
                if (selectedAnchor != null) {
                    listener?.onAnchorChanged(selectedAnchor!!.label)
                    invalidate()
                    return true
                }
                return handlePointTap(event)
            }

            MotionEvent.ACTION_MOVE -> {
                selectedAnchor?.let { handle ->
                    moveHandle(handle, event.x, event.y, state)
                    listener?.onAnchorChanged(handle.label)
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                selectedAnchor = null
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handlePointTap(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return false
        }
        val tapped = cachedAcupoints.minByOrNull { point ->
            hypot((point.position.x - event.x).toDouble(), (point.position.y - event.y).toDouble())
        } ?: return false
        val distance = hypot((tapped.position.x - event.x).toDouble(), (tapped.position.y - event.y).toDouble())
        if (distance <= 72.0) {
            selectedAcupointId = tapped.id
            listener?.onAcupointSelected(tapped)
            invalidate()
            return true
        }
        return false
    }

    private fun pickHandle(state: AnchorState, x: Float, y: Float): AnchorHandle? {
        val handles = handlesForCurrentStep(state)
        return handles.firstOrNull { (_, point) ->
            abs(point.x - x) < 48f && abs(point.y - y) < 48f
        }?.first
    }

    private fun handlesForCurrentStep(state: AnchorState): List<Pair<AnchorHandle, PointF>> = when (calibrationStep) {
        CalibrationStep.SHOULDERS -> listOf(
            AnchorHandle.LeftShoulder to state.leftShoulder,
            AnchorHandle.RightShoulder to state.rightShoulder,
        )
        CalibrationStep.SPINE -> listOf(
            AnchorHandle.SpineTop to state.spineTop,
            AnchorHandle.SpineBottom to state.spineBottom,
        )
        CalibrationStep.BODY_WIDTH -> listOf(
            AnchorHandle.LeftWaist to state.leftWaist,
            AnchorHandle.RightWaist to state.rightWaist,
        )
        CalibrationStep.LOCKED -> emptyList()
    }

    private fun anchorHandlesForCurrentStep(state: AnchorState): List<PointF> =
        handlesForCurrentStep(state).map { it.second }

    private fun drawStepOverlay(canvas: Canvas, state: AnchorState) {
        val message = when (calibrationStep) {
            CalibrationStep.SHOULDERS -> "步骤 1/3：对齐左右肩线"
            CalibrationStep.SPINE -> "步骤 2/3：对齐脊柱中线"
            CalibrationStep.BODY_WIDTH -> "步骤 3/3：调整腰侧胖瘦"
            CalibrationStep.LOCKED -> "定位已锁定：当前为演示态"
        }
        val textWidth = helperTextPaint.measureText(message)
        val rect = RectF(
            state.spineTop.x - textWidth / 2f - 20f,
            state.spineTop.y - 92f,
            state.spineTop.x + textWidth / 2f + 20f,
            state.spineTop.y - 44f,
        )
        canvas.drawRoundRect(rect, 16f, 16f, labelBackgroundPaint)
        canvas.drawText(message, rect.left + 14f, rect.bottom - 14f, helperTextPaint)
    }

    private fun moveHandle(handle: AnchorHandle, x: Float, y: Float, state: AnchorState) {
        val clampedX = x.coerceIn(40f, width - 40f)
        val clampedY = y.coerceIn(40f, height - 40f)
        when (handle) {
            AnchorHandle.LeftShoulder -> state.leftShoulder.set(clampedX, clampedY)
            AnchorHandle.RightShoulder -> state.rightShoulder.set(clampedX, clampedY)
            AnchorHandle.SpineTop -> state.spineTop.set(clampedX, clampedY)
            AnchorHandle.SpineBottom -> state.spineBottom.set(clampedX, clampedY)
            AnchorHandle.LeftWaist -> state.leftWaist.set(clampedX, clampedY)
            AnchorHandle.RightWaist -> state.rightWaist.set(clampedX, clampedY)
        }
    }

    private enum class AnchorHandle {
        LeftShoulder,
        RightShoulder,
        SpineTop,
        SpineBottom,
        LeftWaist,
        RightWaist;

        val label: String
            get() = when (this) {
                LeftShoulder -> "左肩锚点"
                RightShoulder -> "右肩锚点"
                SpineTop -> "脊柱上端"
                SpineBottom -> "脊柱下端"
                LeftWaist -> "左腰侧轮廓"
                RightWaist -> "右腰侧轮廓"
            }

        fun resolve(state: AnchorState): PointF = when (this) {
            LeftShoulder -> state.leftShoulder
            RightShoulder -> state.rightShoulder
            SpineTop -> state.spineTop
            SpineBottom -> state.spineBottom
            LeftWaist -> state.leftWaist
            RightWaist -> state.rightWaist
        }
    }
}
