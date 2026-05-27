package com.example.limbmotionrecoveryapp.screens.progress

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    var percent: Int = 0
        set(value) { field = value; invalidate() }

    private fun dp(v: Float) = v * resources.displayMetrics.density

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#E8F5EF")
        strokeCap = Paint.Cap.ROUND
    }
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1D9E75")
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        val stroke = dp(8f)
        bgPaint.strokeWidth = stroke
        fgPaint.strokeWidth = stroke

        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) / 2f) - stroke
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        canvas.drawArc(rect, 0f, 360f, false, bgPaint)
        if (percent > 0) {
            canvas.drawArc(rect, -90f, 360f * percent / 100f, false, fgPaint)
        }
    }
}
