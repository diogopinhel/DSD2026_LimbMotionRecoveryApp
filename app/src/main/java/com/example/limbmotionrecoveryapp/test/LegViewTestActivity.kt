package com.example.limbmotionrecoveryapp.test

import android.graphics.Color
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.limbmotionrecoveryapp.R
import com.example.limbmotionrecoveryapp.view.LegView

/**
 * Test page for verifying LegView functionality.
 * Includes model display, mode/color settings, and independent angle control for both legs.
 */
class LegViewTestActivity : AppCompatActivity() {

    private lateinit var legView: LegView

    // Setting controls
    private lateinit var rgDisplayMode: RadioGroup
    private lateinit var rgAngleMode: RadioGroup
    private lateinit var cbShowLeft: CheckBox
    private lateinit var cbShowRight: CheckBox
    private lateinit var btnColorDefault: Button
    private lateinit var btnColorSwap: Button
    private lateinit var btnColorHighContrast: Button

    // Left leg control
    private lateinit var seekLeft: SeekBar
    private lateinit var etLeft: EditText

    // Right leg control
    private lateinit var seekRight: SeekBar
    private lateinit var etRight: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leg_view_test)

        initViews()
        initListeners()

        // Initial state: horizontal mode, degree input, 90 degrees
        legView.setDisplayMode(LegView.DisplayMode.HORIZONTAL)
        legView.setAngleModeDegrees()
        legView.setLeftAngle(90f)
        legView.setRightAngle(90f)
    }

    private fun initViews() {
        legView = findViewById(R.id.legView)

        rgDisplayMode = findViewById(R.id.rgDisplayMode)
        rgAngleMode = findViewById(R.id.rgAngleMode)
        cbShowLeft = findViewById(R.id.cbShowLeft)
        cbShowRight = findViewById(R.id.cbShowRight)
        btnColorDefault = findViewById(R.id.btnColorDefault)
        btnColorSwap = findViewById(R.id.btnColorSwap)
        btnColorHighContrast = findViewById(R.id.btnColorHighContrast)

        seekLeft = findViewById(R.id.seekLeft)
        etLeft = findViewById(R.id.etLeft)
        seekRight = findViewById(R.id.seekRight)
        etRight = findViewById(R.id.etRight)
    }

    private fun initListeners() {
        // -------------------------
        // Display mode switch
        // -------------------------
        rgDisplayMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbSquat -> LegView.DisplayMode.SQUAT
                R.id.rbStepping -> LegView.DisplayMode.STEPPING
                else -> LegView.DisplayMode.HORIZONTAL
            }
            legView.setDisplayMode(mode)
        }

        // -------------------------
        // Angle unit switch
        // -------------------------
        rgAngleMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbDegrees -> legView.setAngleModeDegrees()
                R.id.rbRadians -> legView.setAngleModeRadians()
            }
        }

        // -------------------------
        // Visibility toggle
        // -------------------------
        cbShowLeft.setOnCheckedChangeListener { _, isChecked ->
            legView.setShowLeft(isChecked)
        }
        cbShowRight.setOnCheckedChangeListener { _, isChecked ->
            legView.setShowRight(isChecked)
        }

        // -------------------------
        // Color schemes
        // -------------------------
        btnColorDefault.setOnClickListener {
            // Left: blue series, Right: red series (original default)
            legView.setLeftLimbColors(intArrayOf(
                Color.parseColor("#87CEEB"),
                Color.parseColor("#4A90D9"),
                Color.parseColor("#1A3A5C")
            ))
            legView.setRightLimbColors(intArrayOf(
                Color.parseColor("#FFB6C1"),
                Color.parseColor("#D94A4A"),
                Color.parseColor("#5C1A1A")
            ))
            legView.setLeftCapColors(intArrayOf(
                Color.parseColor("#A8D8FF"),
                Color.parseColor("#2C5F8A")
            ))
            legView.setRightCapColors(intArrayOf(
                Color.parseColor("#FFD8D8"),
                Color.parseColor("#8A2C2C")
            ))
        }

        btnColorSwap.setOnClickListener {
            // Swap left and right leg colors
            legView.setLeftLimbColors(intArrayOf(
                Color.parseColor("#FFB6C1"),
                Color.parseColor("#D94A4A"),
                Color.parseColor("#5C1A1A")
            ))
            legView.setRightLimbColors(intArrayOf(
                Color.parseColor("#87CEEB"),
                Color.parseColor("#4A90D9"),
                Color.parseColor("#1A3A5C")
            ))
            legView.setLeftCapColors(intArrayOf(
                Color.parseColor("#FFD8D8"),
                Color.parseColor("#8A2C2C")
            ))
            legView.setRightCapColors(intArrayOf(
                Color.parseColor("#A8D8FF"),
                Color.parseColor("#2C5F8A")
            ))
        }

        btnColorHighContrast.setOnClickListener {
            // High contrast: Left green, Right purple
            legView.setLeftLimbColors(intArrayOf(
                Color.parseColor("#90EE90"),
                Color.parseColor("#32CD32"),
                Color.parseColor("#006400")
            ))
            legView.setRightLimbColors(intArrayOf(
                Color.parseColor("#DDA0DD"),
                Color.parseColor("#9370DB"),
                Color.parseColor("#4B0082")
            ))
            legView.setLeftCapColors(intArrayOf(
                Color.parseColor("#98FB98"),
                Color.parseColor("#228B22")
            ))
            legView.setRightCapColors(intArrayOf(
                Color.parseColor("#E6E6FA"),
                Color.parseColor("#6A5ACD")
            ))
        }

        // -------------------------
        // Left leg: SeekBar and EditText linkage
        // -------------------------
        seekLeft.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etLeft.setText(progress.toString())
                    legView.setLeftAngle(progress.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etLeft.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyLeftAngleFromEditText()
                true
            } else false
        }
        etLeft.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) applyLeftAngleFromEditText()
        }

        // -------------------------
        // Right leg: SeekBar and EditText linkage
        // -------------------------
        seekRight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etRight.setText(progress.toString())
                    legView.setRightAngle(progress.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etRight.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyRightAngleFromEditText()
                true
            } else false
        }
        etRight.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) applyRightAngleFromEditText()
        }
    }

    /** Read left leg angle from EditText, clamp to [0,180], sync SeekBar and LegView */
    private fun applyLeftAngleFromEditText() {
        val value = etLeft.text.toString().toFloatOrNull() ?: 0f
        val clamped = value.coerceIn(0f, 180f)
        seekLeft.progress = clamped.toInt()
        etLeft.setText(clamped.toInt().toString())
        legView.setLeftAngle(clamped)
    }

    /** Read right leg angle from EditText, clamp to [0,180], sync SeekBar and LegView */
    private fun applyRightAngleFromEditText() {
        val value = etRight.text.toString().toFloatOrNull() ?: 0f
        val clamped = value.coerceIn(0f, 180f)
        seekRight.progress = clamped.toInt()
        etRight.setText(clamped.toInt().toString())
        legView.setRightAngle(clamped)
    }
}