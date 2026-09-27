package com.reelockapp.blocker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * A dashed background ring with a solid progress arc drawn on top,
 * showing streak progress toward the next growth stage.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0f // 0f..1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var progressColor: Int = Color.parseColor("#639922")
    var trackColor: Int = Color.parseColor("#66888880")

    private val strokeWidthPx = 22f
    private val rect = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = strokeWidthPx
        rect.set(inset, inset, width - inset, height - inset)

        trackPaint.color = trackColor
        canvas.drawArc(rect, 0f, 360f, false, trackPaint)

        progressPaint.color = progressColor
        canvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)
    }
}
