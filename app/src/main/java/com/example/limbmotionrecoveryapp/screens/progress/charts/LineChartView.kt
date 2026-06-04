package com.example.limbmotionrecoveryapp.screens.progress.charts

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.example.limbmotionrecoveryapp.screens.progress.RomPoint
import com.example.limbmotionrecoveryapp.screens.progress.PainPoint

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    fun setData(romHistory: List<RomPoint>, painDaily: List<PainPoint>) {
        // Stub: store data for future rendering
        invalidate()
    }
}
