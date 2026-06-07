package com.example.limbmotionrecoveryapp.sensor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.dsd.s1.ble.SensorService
import com.dsd.s1.model.SensorConfig
import com.dsd.s1.model.ServiceConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SensorRepository {

    private const val NOTIFY_CHAR_UUID = "0000ffe4-0000-1000-8000-00805f9a34fb"
    private const val SCAN_TIMEOUT_MS = 10_000L
    private const val CONNECT_TIMEOUT_MS = 12_000L

    private val MAC_WHITELIST = setOf(
        "D5:17:71:B2:B2:67",
        "C1:18:C7:C3:AA:49",
        "E1:B8:34:05:DE:E9",
        "D9:BC:B5:1E:39:35",
        "D7:27:2D:8F:6A:4C",
        "D2:26:08:77:94:1B"
    )

    enum class State { IDLE, SCANNING, FOUND, CONNECTING, CONNECTED, ERROR }

    data class FoundDevice(
        val name: String,
        val address: String,
        val rssi: Int
    )

    val state = MutableLiveData(State.IDLE)
    val foundDevice = MutableLiveData<FoundDevice?>()
    val errorMessage = MutableLiveData<String?>()

    // [Multi] 新增：所有发现的设备列表
    val foundDevices = MutableLiveData<List<FoundDevice>>(emptyList())
    // [Multi] 新增：已连接传感器数量（用于 UI 显示）
    val connectedCount = MutableLiveData(0)

    private var sensorService: SensorService? = null
    private var bleScanner: android.bluetooth.le.BluetoothLeScanner? = null
    private var activeScanCallback: ScanCallback? = null
    private var scope: CoroutineScope? = null
    private var monitorJob: Job? = null

    // [Multi] 新增：扫描期间临时收集列表
    private val _foundDevices = mutableListOf<FoundDevice>()
    // [Multi] 新增：已连接地址列表
    private val _connectedAddresses = mutableListOf<String>()

    var connectedAddress: String? = null
        private set

    fun startScan(context: Context) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            state.postValue(State.ERROR)
            errorMessage.postValue("Bluetooth is disabled. Enable it and try again.")
            return
        }

        cancelScope()
        foundDevice.postValue(null)
        errorMessage.postValue(null)
        state.postValue(State.SCANNING)

        // [Multi] 清空列表
        synchronized(_foundDevices) { _foundDevices.clear() }
        foundDevices.postValue(emptyList())

        bleScanner = adapter.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (state.value != State.SCANNING) return
                val device = result.device
                val address = device.address?.uppercase()
                if (address == null || address !in MAC_WHITELIST) return
                val name = try { device.name } catch (_: SecurityException) { null }
                    ?: "WitMotion Sensor"
                val found = FoundDevice(
                    name = name,
                    address = address,
                    rssi = result.rssi
                )

                // [Multi] 收集到列表，不立即 stopScan
                synchronized(_foundDevices) {
                    if (_foundDevices.none { it.address == address }) {
                        _foundDevices.add(found)
                        foundDevices.postValue(_foundDevices.toList())
                        // 兼容旧代码：foundDevice 始终指向第一个发现的设备
                        if (foundDevice.value == null) {
                            foundDevice.postValue(found)
                            state.postValue(State.FOUND)
                        }
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                stopScan()
                state.postValue(State.ERROR)
                errorMessage.postValue("Scan failed (code $errorCode). Check Bluetooth permissions.")
            }
        }
        activeScanCallback = cb

        try {
            bleScanner?.startScan(null, settings, cb)
        } catch (e: SecurityException) {
            state.postValue(State.ERROR)
            errorMessage.postValue("Bluetooth permission denied.")
            return
        }

        scope = CoroutineScope(Dispatchers.IO)
        scope!!.launch {
            delay(SCAN_TIMEOUT_MS)
            if (state.value == State.SCANNING) {
                stopScan()
                val hasAny = synchronized(_foundDevices) { _foundDevices.isNotEmpty() }
                if (!hasAny) {
                    withContext(Dispatchers.Main) {
                        state.value = State.ERROR
                        errorMessage.value = "No WitMotion sensor found nearby. Make sure the sensor is on and nearby."
                    }
                }
                // 如果已有设备，保持 FOUND 状态
            }
        }
    }

    fun stopScan() {
        try {
            activeScanCallback?.let { bleScanner?.stopScan(it) }
        } catch (_: SecurityException) {}
        activeScanCallback = null
    }

    // [Multi] 保留原有方法，内部转发到批量连接
    fun connect(context: Context, address: String) {
        connectMultiple(context, listOf(address))
    }

    // [Multi] 新增：连接所有已发现的设备
    fun connectAll(context: Context) {
        val addresses = synchronized(_foundDevices) { _foundDevices.map { it.address } }
        if (addresses.isEmpty()) return
        connectMultiple(context, addresses)
    }

    // [Multi] 新增：批量连接核心逻辑
    private fun connectMultiple(context: Context, addresses: List<String>) {
        state.postValue(State.CONNECTING)
        errorMessage.postValue(null)
        stopScan() // 开始连接时停止扫描

        val service = SensorService(context.applicationContext)
        val configs = addresses.map { SensorConfig(it, NOTIFY_CHAR_UUID) }
        service.initialize(configs, ServiceConfig())
        service.startSensors()
        sensorService = service

        _connectedAddresses.clear()
        _connectedAddresses.addAll(addresses)
        connectedAddress = addresses.firstOrNull() // 兼容旧代码

        cancelScope()
        scope = CoroutineScope(Dispatchers.IO)
        scope!!.launch {
            val deadline = System.currentTimeMillis() + CONNECT_TIMEOUT_MS
            while (isActive) {
                val status = service.getStatus()
                // [Multi] 至少连上一个就算成功（与原有逻辑一致）
                if (status.connectedSensors > 0) {
                    withContext(Dispatchers.Main) {
                        state.value = State.CONNECTED
                        connectedCount.value = status.connectedSensors
                    }
                    startMonitor(service)
                    return@launch
                }
                if (System.currentTimeMillis() > deadline) {
                    service.stopSensors()
                    sensorService = null
                    _connectedAddresses.clear()
                    connectedAddress = null
                    withContext(Dispatchers.Main) {
                        state.value = State.ERROR
                        errorMessage.value = "Could not connect to sensors. Make sure they are on and nearby."
                    }
                    return@launch
                }
                delay(500)
            }
        }
    }

    private fun startMonitor(service: SensorService) {
        monitorJob = scope?.launch {
            while (isActive) {
                delay(2_000)
                val status = service.getStatus()
                // [Multi] 全部断开才认为断开
                if (status.connectedSensors == 0 && state.value == State.CONNECTED) {
                    withContext(Dispatchers.Main) {
                        state.value = State.IDLE
                    }
                    sensorService = null
                    _connectedAddresses.clear()
                    connectedAddress = null
                    break
                }
                // [Multi] 更新连接数量
                if (state.value == State.CONNECTED) {
                    connectedCount.postValue(status.connectedSensors)
                }
            }
        }
    }

    fun disconnect() {
        monitorJob?.cancel()
        cancelScope()
        sensorService?.stopSensors()
        sensorService = null
        _connectedAddresses.clear()
        connectedAddress = null
        state.postValue(State.IDLE)
        foundDevice.postValue(null)
        errorMessage.postValue(null)
        // [Multi]
        synchronized(_foundDevices) { _foundDevices.clear() }
        foundDevices.postValue(emptyList())
        connectedCount.postValue(0)
    }

    fun retryFromScan() {
        monitorJob?.cancel()
        cancelScope()
        sensorService?.stopSensors()
        sensorService = null
        _connectedAddresses.clear()
        connectedAddress = null
        foundDevice.postValue(null)
        errorMessage.postValue(null)
        state.postValue(State.IDLE)
        // [Multi]
        synchronized(_foundDevices) { _foundDevices.clear() }
        foundDevices.postValue(emptyList())
        connectedCount.postValue(0)
    }

    fun getService(): SensorService? = sensorService

    fun isConnected() = state.value == State.CONNECTED

    private fun cancelScope() {
        scope?.cancel()
        scope = null
    }
}