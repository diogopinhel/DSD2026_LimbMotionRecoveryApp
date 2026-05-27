package com.example.limbmotionrecoveryapp.screens.progress.charts

import android.content.Context
import android.util.AttributeSet
import android.view.View

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var percentage: Int = 0

    fun setPercentage(percent: Int) {
        this.percentage = percent
        invalidate()
    }
}
