package com.humanacupoints.demo.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

class BackOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val accentColor = ContextCompat.getColor(context, R.color.accent)
    private val accentSecondary = ContextCompat.getColor(context, R.color.accent_secondary)
    private val dangerColor = ContextCompat.getColor(context, R.color.danger_zone)
    private val textPrimary = ContextCompat.getColor(context, R.color.text_primary)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentSecondary
        strokeWidth = 6f
        style = Paint.Style.STROKE
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

    private var anchors: AnchorState? = null
    private var frozen = false
    private var selectedAnchor: AnchorHandle? = null

    fun setFrozen(value: Boolean) {
        frozen = value
        invalidate()
    }

    fun resetAnchors() {
        if (width > 0 && height > 0) {
            anchors = DemoBackModel.defaultAnchors(width.toFloat(), height.toFloat())
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (anchors == null && w > 0 && h > 0) {
            anchors = DemoBackModel.defaultAnchors(w.toFloat(), h.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val state = anchors ?: return

        drawBodyGuide(canvas, state)
        val acupoints = DemoBackModel.generateAcupoints(state)
        acupoints.forEach { drawAcupoint(canvas, it) }
        if (frozen) {
            drawAnchors(canvas, state)
        }
    }

    private fun drawBodyGuide(canvas: Canvas, state: AnchorState) {
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

        val shoulderRect = RectF(
            state.leftShoulder.x,
            state.spineTop.y + 16f,
            state.rightShoulder.x,
            state.spineBottom.y,
        )
        canvas.drawRoundRect(shoulderRect, 28f, 28f, dangerPaint)
    }

    private fun drawAcupoint(canvas: Canvas, point: AcupointRender) {
        canvas.drawCircle(point.position.x, point.position.y, 12f, pointPaint)
        val textWidth = textPaint.measureText(point.label)
        val left = point.position.x + 18f
        val top = point.position.y - 38f
        val rect = RectF(left, top, left + textWidth + 24f, top + 42f)
        canvas.drawRoundRect(rect, 16f, 16f, labelBackgroundPaint)
        canvas.drawText(point.label, left + 12f, top + 29f, textPaint)
    }

    private fun drawAnchors(canvas: Canvas, state: AnchorState) {
        listOf(
            state.leftShoulder,
            state.rightShoulder,
            state.spineTop,
            state.spineBottom,
        ).forEach { point ->
            canvas.drawCircle(point.x, point.y, 18f, anchorPaint)
            canvas.drawCircle(point.x, point.y, 24f, centerLinePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!frozen) {
            return false
        }
        val state = anchors ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selectedAnchor = pickHandle(state, event.x, event.y)
                return selectedAnchor != null
            }

            MotionEvent.ACTION_MOVE -> {
                selectedAnchor?.let { handle ->
                    moveHandle(handle, event.x, event.y, state)
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                selectedAnchor = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun pickHandle(state: AnchorState, x: Float, y: Float): AnchorHandle? {
        val handles = listOf(
            AnchorHandle.LeftShoulder to state.leftShoulder,
            AnchorHandle.RightShoulder to state.rightShoulder,
            AnchorHandle.SpineTop to state.spineTop,
            AnchorHandle.SpineBottom to state.spineBottom,
        )
        return handles.firstOrNull { (_, point) ->
            abs(point.x - x) < 48f && abs(point.y - y) < 48f
        }?.first
    }

    private fun moveHandle(handle: AnchorHandle, x: Float, y: Float, state: AnchorState) {
        val clampedX = x.coerceIn(40f, width - 40f)
        val clampedY = y.coerceIn(40f, height - 40f)
        when (handle) {
            AnchorHandle.LeftShoulder -> state.leftShoulder.set(clampedX, clampedY)
            AnchorHandle.RightShoulder -> state.rightShoulder.set(clampedX, clampedY)
            AnchorHandle.SpineTop -> state.spineTop.set(clampedX, clampedY)
            AnchorHandle.SpineBottom -> state.spineBottom.set(clampedX, clampedY)
        }
    }

    private enum class AnchorHandle {
        LeftShoulder,
        RightShoulder,
        SpineTop,
        SpineBottom,
    }
}
