package com.example.limbmotionrecoveryapp.screens.session

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R
import com.google.android.material.button.MaterialButton

class PrepareExerciseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_exercise)

        val planName = intent.getStringExtra("planName")

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvPlanName).text = planName ?: "Today's Exercise"

        findViewById<MaterialButton>(R.id.btnStartSession).setOnClickListener {
            // TODO: 启动传感器连接 + 创建 V2 Session + 跳转训练页面
        }
    }
}