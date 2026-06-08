package com.example.limbmotionrecoveryapp.screens.progress

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class PainLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Point(val label: String, val value: Int)

    private var data: List<Point> = emptyList()

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0392B")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0392B")
        style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B8A82")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9EB5AF")
        textAlign = Paint.Align.CENTER
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C4D4D0")
        textAlign = Paint.Align.CENTER
    }

    fun setData(points: List<Point>) {
        data = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        linePaint.strokeWidth = dp(2.5f)
        dotBorderPaint.strokeWidth = dp(2f)
        valuePaint.textSize = sp(10f)
        labelPaint.textSize = sp(10f)
        emptyPaint.textSize = sp(13f)

        if (data.size < 2) {
            canvas.drawText(
                if (data.isEmpty()) "No pain check-ins yet" else "Need more data",
                width / 2f,
                height / 2f - dp(4f),
                emptyPaint
            )
            return
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val padTop = dp(22f)
        val padBottom = dp(22f)
        val padH = dp(14f)
        val chartH = h - padTop - padBottom
        val chartW = w - padH * 2
        val n = data.size

        val xs = FloatArray(n) { i -> padH + i.toFloat() / (n - 1) * chartW }
        val ys = FloatArray(n) { i ->
            padTop + chartH - ((data[i].value.coerceIn(1, 10) - 1).toFloat() / 9f) * chartH
        }

        // Gradient fill
        val fillPath = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until n) lineTo(xs[i], ys[i])
            lineTo(xs[n - 1], h - padBottom)
            lineTo(xs[0], h - padBottom)
            close()
        }
        fillPaint.shader = LinearGradient(
            0f, padTop, 0f, h - padBottom,
            intArrayOf(Color.parseColor("#50C0392B"), Color.parseColor("#05C0392B")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // Line
        val linePath = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until n) lineTo(xs[i], ys[i])
        }
        canvas.drawPath(linePath, linePaint)

        // Dots + labels
        val dotR = dp(4.5f)
        for (i in data.indices) {
            canvas.drawText(data[i].value.toString(), xs[i], ys[i] - dp(9f), valuePaint)
            canvas.drawCircle(xs[i], ys[i], dotR, dotFillPaint)
            canvas.drawCircle(xs[i], ys[i], dotR, dotBorderPaint)
            canvas.drawText(data[i].label, xs[i], h - dp(4f), labelPaint)
        }
    }
}
