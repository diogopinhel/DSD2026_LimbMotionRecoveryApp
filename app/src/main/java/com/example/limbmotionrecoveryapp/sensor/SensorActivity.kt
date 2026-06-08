package com.example.limbmotionrecoveryapp.sensor

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dsd.s1.ble.BluetoothUtil
import com.example.limbmotionrecoveryapp.R

class SensorActivity : AppCompatActivity() {

    private val REQUEST_BLE_PERMISSIONS = 1001

    private lateinit var stateScanningLayout: ScrollView
    private lateinit var stateFoundLayout: ScrollView
    private lateinit var stateConnectedLayout: ScrollView
    private lateinit var stateErrorLayout: LinearLayout
    private lateinit var stateConnectingLayout: LinearLayout

    private lateinit var tvFoundDeviceName: TextView
    private lateinit var tvFoundDeviceAddress: TextView
    private lateinit var tvConnectedDeviceName: TextView
    private lateinit var tvErrorMessage: TextView
    private lateinit var tvErrorDetail: TextView

    private lateinit var btnConnectDevice: LinearLayout
    private lateinit var btnDone: LinearLayout
    private lateinit var btnRetry: LinearLayout

    private lateinit var scanRing1: View
    private lateinit var scanRing2: View
    private lateinit var scanRing3: View

    private val ringAnimators = mutableListOf<ObjectAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        stateScanningLayout = findViewById(R.id.stateScanningLayout)
        stateFoundLayout = findViewById(R.id.stateFoundLayout)
        stateConnectedLayout = findViewById(R.id.stateConnectedLayout)
        stateErrorLayout = findViewById(R.id.stateErrorLayout)
        stateConnectingLayout = findViewById(R.id.stateConnectingLayout)

        tvFoundDeviceName = findViewById(R.id.tvFoundDeviceName)
        tvFoundDeviceAddress = findViewById(R.id.tvFoundDeviceAddress)
        tvConnectedDeviceName = findViewById(R.id.tvConnectedDeviceName)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        tvErrorDetail = findViewById(R.id.tvErrorDetail)

        btnConnectDevice = findViewById(R.id.btnConnectDevice)
        btnDone = findViewById(R.id.btnDone)
        btnRetry = findViewById(R.id.btnRetry)

        scanRing1 = findViewById(R.id.scanRing1)
        scanRing2 = findViewById(R.id.scanRing2)
        scanRing3 = findViewById(R.id.scanRing3)

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            SensorRepository.stopScan()
            finish()
        }

        findViewById<TextView>(R.id.btnSkipFromScan).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSkipFromFound).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSkipFromError).setOnClickListener { finish() }

        btnConnectDevice.setOnClickListener {
            val device = SensorRepository.foundDevice.value ?: return@setOnClickListener
            SensorRepository.connect(this, device.address)
        }

        btnDone.setOnClickListener { finish() }

        btnRetry.setOnClickListener {
            SensorRepository.retryFromScan()
            startScanOrRequestPermissions()
        }

        observeRepository()
        startScanOrRequestPermissions()
    }

    private fun observeRepository() {
        SensorRepository.state.observe(this) { state ->
            when (state) {
                SensorRepository.State.SCANNING -> {
                    showState(ScanState.SCANNING)
                    startRingAnimation()
                }
                SensorRepository.State.FOUND -> {
                    stopRingAnimation()
                    val device = SensorRepository.foundDevice.value
                    tvFoundDeviceName.text = device?.name ?: "WitMotion Sensor"
                    tvFoundDeviceAddress.text = "Ready to pair · ${device?.address?.takeLast(5) ?: "BLE"}"
                    showState(ScanState.FOUND)
                }
                SensorRepository.State.CONNECTING -> {
                    showState(ScanState.CONNECTING)
                }
                SensorRepository.State.CONNECTED -> {
                    stopRingAnimation()
                    val device = SensorRepository.foundDevice.value
                    tvConnectedDeviceName.text = "${device?.name ?: "LIMBS Sensor"} is ready.\nMotion data will be recorded during your session."
                    showState(ScanState.CONNECTED)
                }
                SensorRepository.State.ERROR -> {
                    stopRingAnimation()
                    tvErrorMessage.text = "No sensor found"
                    tvErrorDetail.text = SensorRepository.errorMessage.value
                        ?: "Make sure your sensor is on and nearby."
                    showState(ScanState.ERROR)
                }
                SensorRepository.State.IDLE -> {
                    showState(ScanState.SCANNING)
                }
            }
        }
    }

    private fun startScanOrRequestPermissions() {
        if (!hasBluetoothPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                REQUEST_BLE_PERMISSIONS
            )
        } else {
            SensorRepository.startScan(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                SensorRepository.startScan(this)
            } else {
                SensorRepository.errorMessage.postValue("Bluetooth permissions are required to connect the sensor.")
                SensorRepository.state.postValue(SensorRepository.State.ERROR)
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean =
        BluetoothUtil.hasBluetoothPermissions(this)

    private enum class ScanState { SCANNING, FOUND, CONNECTING, CONNECTED, ERROR }

    private fun showState(state: ScanState) {
        stateScanningLayout.visibility = if (state == ScanState.SCANNING) View.VISIBLE else View.GONE
        stateFoundLayout.visibility = if (state == ScanState.FOUND) View.VISIBLE else View.GONE
        stateConnectingLayout.visibility = if (state == ScanState.CONNECTING) View.VISIBLE else View.GONE
        stateConnectedLayout.visibility = if (state == ScanState.CONNECTED) View.VISIBLE else View.GONE
        stateErrorLayout.visibility = if (state == ScanState.ERROR) View.VISIBLE else View.GONE
    }

    private fun startRingAnimation() {
        stopRingAnimation()
        listOf(scanRing1 to 0L, scanRing2 to 400L, scanRing3 to 800L).forEach { (ring, delay) ->
            val anim = ObjectAnimator.ofPropertyValuesHolder(
                ring,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, ring.alpha, ring.alpha * 0.4f, ring.alpha)
            ).apply {
                duration = 2000
                startDelay = delay
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
            }
            ringAnimators.add(anim)
            anim.start()
        }
    }

    private fun stopRingAnimation() {
        ringAnimators.forEach { it.cancel() }
        ringAnimators.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingAnimation()
    }
}
