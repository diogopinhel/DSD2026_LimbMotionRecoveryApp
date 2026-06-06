package com.example.limbmotionrecoveryapp.screens.session

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.session.SessionController
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrepareExerciseActivity : AppCompatActivity() {

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private data class ExerciseOption(
        val id: String,
        val name: String,
        val description: String
    )

    private val exercises = listOf(
        ExerciseOption(
            "lying_flat",
            "Lying Flat",
            "Lie on your back and perform leg raises or stretches to improve hip flexibility and range of motion."
        ),
        ExerciseOption(
            "squat",
            "Squat",
            "Stand up and sit down repeatedly to strengthen your quadriceps, hamstrings, and knee joints."
        ),
        ExerciseOption(
            "march_in_place",
            "March in Place",
            "Lift your knees alternately while standing still to improve leg coordination and cardiovascular endurance."
        )
    )

    private var selectedIndex: Int = -1

    private lateinit var cards: List<MaterialCardView>
    private lateinit var indicators: List<View>
    private lateinit var radioButtons: List<RadioButton>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_exercise)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val planName = intent.getStringExtra("planName")
        findViewById<TextView>(R.id.tvPlanName).text = planName ?: "Today's Exercise"

        initExerciseCards()

        findViewById<MaterialButton>(R.id.btnStartSession).setOnClickListener {
            startSession()
        }
    }

    private fun initExerciseCards() {
        cards = listOf(
            findViewById(R.id.cardExercise1),
            findViewById(R.id.cardExercise2),
            findViewById(R.id.cardExercise3)
        )
        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3)
        )
        radioButtons = listOf(
            findViewById(R.id.rbExercise1),
            findViewById(R.id.rbExercise2),
            findViewById(R.id.rbExercise3)
        )

        cards.forEachIndexed { index, card ->
            card.setOnClickListener { selectExercise(index) }
        }
    }

    private fun selectExercise(index: Int) {
        selectedIndex = index
        val green = ContextCompat.getColor(this, R.color.colorPrimaryGreen)
        val borderNormal = ContextCompat.getColor(this, R.color.colorBorder)
        val dp2px = { dp: Float -> (dp * resources.displayMetrics.density + 0.5f).toInt() }

        cards.forEachIndexed { i, card ->
            val isSelected = i == index
            card.strokeColor = if (isSelected) green else borderNormal
            card.strokeWidth = if (isSelected) dp2px(2f) else dp2px(1f)
            indicators[i].setBackgroundColor(if (isSelected) green else Color.TRANSPARENT)
            radioButtons[i].isChecked = isSelected
        }
    }

    private fun startSession() {
        if (selectedIndex == -1) {
            Snackbar.make(
                findViewById(R.id.btnStartSession),
                "Please select an exercise type",
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val selectedId = exercises[selectedIndex].id
        val controller = SessionController.getInstance(applicationContext)

        if (controller.getState() == SessionController.State.ENDED) {
            controller.reset()
        }
        controller.setExerciseType(selectedId)
        controller.setSensorMode(true)

        activityScope.launch(Dispatchers.IO) {
            val result = controller.start()
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    startActivity(
                        Intent(this@PrepareExerciseActivity, SessionPlayerActivity::class.java)
                    )
                }.onFailure { e ->
                    Snackbar.make(
                        findViewById(R.id.btnStartSession),
                        "Failed to start: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}