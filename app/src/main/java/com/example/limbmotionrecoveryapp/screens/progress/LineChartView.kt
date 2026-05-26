package com.example.limbmotionrecoveryapp.screens.progress

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class LineChartView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    private var points: List<RomPoint> = emptyList()
    private var targetDegrees: Int = 0
    private var noDataMessage: String = ""

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D9E75")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D9E75")
        style = Paint.Style.FILL
    }
    private val dotLastPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D9E75")
        style = Paint.Style.FILL
        setShadowLayer(6f, 0f, 0f, Color.parseColor("#401D9E75"))
    }
    private val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0F4F3")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1D9E75")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 100
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9EB5AF")
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }
    private val noDataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4D4D0")
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    fun setData(points: List<RomPoint>, targetDegrees: Int) {
        this.points = points
        this.targetDegrees = targetDegrees
        this.noDataMessage = ""
        invalidate()
    }

    fun setNoData(message: String = "Awaiting data from server") {
        this.points = emptyList()
        this.noDataMessage = message
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (points.isEmpty()) {
            canvas.drawText(noDataMessage, width / 2f, height / 2f, noDataPaint)
            return
        }

        val padLeft = 80f
        val padRight = 20f
        val padTop = 20f
        val padBottom = 30f
        val chartW = width - padLeft - padRight
        val chartH = height - padTop - padBottom

        val minVal = points.minOf { it.degrees }.toFloat() * 0.85f
        val maxVal = maxOf(points.maxOf { it.degrees }.toFloat(), targetDegrees.toFloat()) * 1.05f

        fun xOf(i: Int) = padLeft + (i.toFloat() / (points.size - 1)) * chartW
        fun yOf(v: Int) = padTop + chartH - ((v - minVal) / (maxVal - minVal)) * chartH

        // Grid lines at 25% intervals
        for (i in 0..3) {
            val y = padTop + (i.toFloat() / 3f) * chartH
            canvas.drawLine(padLeft, y, padLeft + chartW, y, gridPaint)
        }

        // Target line
        if (targetDegrees > 0) {
            val ty = yOf(targetDegrees)
            canvas.drawLine(padLeft, ty, padLeft + chartW, ty, targetPaint)
        }

        // Gradient fill
        val path = Path()
        path.moveTo(xOf(0), yOf(points[0].degrees))
        for (i in 1 until points.size) path.lineTo(xOf(i), yOf(points[i].degrees))
        path.lineTo(xOf(points.size - 1), padTop + chartH)
        path.lineTo(xOf(0), padTop + chartH)
        path.close()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, padTop, 0f, padTop + chartH,
                Color.parseColor("#2E1D9E75"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(path, fillPaint)

        // Line
        val linePath = Path()
        linePath.moveTo(xOf(0), yOf(points[0].degrees))
        for (i in 1 until points.size) linePath.lineTo(xOf(i), yOf(points[i].degrees))
        canvas.drawPath(linePath, linePaint)

        // Dots
        for (i in 0 until points.size - 1) {
            canvas.drawCircle(xOf(i), yOf(points[i].degrees), 8f, dotPaint)
        }
        // Last dot with border (larger)
        val lastX = xOf(points.size - 1)
        val lastY = yOf(points.last().degrees)
        canvas.drawCircle(lastX, lastY, 12f, dotPaint)
        canvas.drawCircle(lastX, lastY, 7f, dotBorderPaint)

        // X labels (dates)
        for (i in points.indices) {
            canvas.drawText(points[i].date.takeLast(5), xOf(i), height - 2f, labelPaint)
        }
    }
}
