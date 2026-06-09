package com.example.limbmotionrecoveryapp.session

import com.dsd.s1.model.SensorSample
import com.dsd.s2.model.ErrorEvent
import com.dsd.s2.model.FormatData
import com.dsd.s2.model.SessionContext
import com.dsd.s2.model.TargetAngle
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * M1 自研仿真传感器 —— 完全替代 S1/S2 模块
 *
 * 职责：
 * - 单例模式，在 SessionController.buildS2() 中初始化
 * - 根据 payloadStatus（运动类型）生成三种不同的关节角度模式
 * - 后台 10Hz 持续生成 FormatData 并缓存
 * - read() 时合并缓存一次性交付；缓存为空则立即生成
 * - 不生成错误事件，errors 始终为空列表
 *
 * 注意：下方 createFakeSessionContext() 中的 SessionContext 构造参数为假设值，
 * 若与实际 SessionContext 定义不符，请根据编译报错手动调整。
 */
class M1SimulatedSensor private constructor() {

    // -------------------------------------------------------------------------
    // 单例
    // -------------------------------------------------------------------------
    companion object {
        @Volatile
        private var INSTANCE: M1SimulatedSensor? = null

        fun getInstance(): M1SimulatedSensor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: M1SimulatedSensor().also { INSTANCE = it }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 返回类型（兼容 SessionController 的极简壳）
    // -------------------------------------------------------------------------
    data class StartResult(val success: Boolean, val errorMessage: String? = null)
    data class StopResult(
        val sampleCount: Int,
        val errorCount: Int,
        val startTime: String,
        val endTime: String
    )

    // -------------------------------------------------------------------------
    // 常量
    // -------------------------------------------------------------------------
    private val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

    private val sensorMacsLeft = listOf(
        "D7:27:2D:8F:6A:4C",   // L1
        "E1:B8:34:05:DE:E9",   // L2
        "D9:BC:B5:1E:39:35"    // L3
    )
    private val sensorMacsRight = listOf(
        "D2:26:08:77:94:1B",   // R1
        "D5:17:71:B2:B2:67",   // R2
        "C1:18:C7:C3:AA:49"    // R3
    )
    private val allSensorMacs = sensorMacsLeft + sensorMacsRight

    // -------------------------------------------------------------------------
    // 状态
    // -------------------------------------------------------------------------
    private var isRunning = false
    private var currentSessionId = 0
    private var currentUserId = 0
    private var currentExerciseType = "bend_knee_10"
    private var sessionStartTime = 0L
    private var sampleCounter = 0

    private val buffer = ArrayDeque<FormatData>()
    private var generatorTimer: Timer? = null

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    fun start(sessionId: Int, userId: Int, payloadStatus: String): StartResult {
        if (isRunning) return StartResult(success = true)

        currentSessionId = sessionId
        currentUserId = userId
        currentExerciseType = payloadStatus
        sessionStartTime = System.currentTimeMillis()
        sampleCounter = 0
        buffer.clear()

        isRunning = true
        startGeneratorTimer()
        return StartResult(success = true)
    }

    fun stop(): StopResult {
        if (!isRunning) {
            return StopResult(0, 0, "", "")
        }
        isRunning = false
        stopGeneratorTimer()

        val endTime = System.currentTimeMillis()
        return StopResult(
            sampleCount = sampleCounter,
            errorCount = 0,
            startTime = formatter.format(Instant.ofEpochMilli(sessionStartTime)),
            endTime = formatter.format(Instant.ofEpochMilli(endTime))
        )
    }

    /** 重置内部状态，供 SessionController.reset() 调用 */
    fun reset() {
        isRunning = false
        stopGeneratorTimer()
        buffer.clear()
        sampleCounter = 0
        currentSessionId = 0
        currentUserId = 0
        currentExerciseType = "bend_knee_10"
    }

    // -------------------------------------------------------------------------
    // 数据读取（核心）
    // -------------------------------------------------------------------------

    /**
     * 读取数据。
     * - 若缓存中有积压帧，合并所有帧后一次性返回并清空缓存。
     * - 若缓存为空，立即现场生成一帧返回（不阻塞）。
     */
    fun read(): FormatData {
        val frames = synchronized(buffer) {
            if (buffer.isEmpty()) {
                return generateFrame()
            }
            val snapshot = buffer.toList()
            buffer.clear()
            snapshot
        }

        // 合并多帧：sensorData 和 targetAngles 平铺拼接
        return FormatData(
            sessionContext = frames.last().sessionContext,
            sensorData = frames.flatMap { it.sensorData },
            targetAngles = frames.flatMap { it.targetAngles },
            errors = emptyList()
        )
    }

    // -------------------------------------------------------------------------
    // 后台生成器
    // -------------------------------------------------------------------------

    private fun startGeneratorTimer() {
        stopGeneratorTimer()
        generatorTimer = Timer("M1SensorGen", true).apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (!isRunning) return
                    val frame = generateFrame()
                    synchronized(buffer) {
                        buffer.add(frame)
                    }
                }
            }, 0L, 100L) // 10 Hz
        }
    }

    private fun stopGeneratorTimer() {
        generatorTimer?.cancel()
        generatorTimer = null
    }

    // -------------------------------------------------------------------------
    // 帧生成
    // -------------------------------------------------------------------------

    private fun generateFrame(): FormatData {
        val now = System.currentTimeMillis()
        sampleCounter++

        val (leftAngle, rightAngle) = computeAngles(now)

        val sensorData = allSensorMacs.map { mac ->
            generateSensorSample(mac, now, leftAngle, rightAngle)
        }

        val targetAngles = listOf(
            TargetAngle(timestamp = now, angleID = "left_knee", angle = leftAngle),
            TargetAngle(timestamp = now, angleID = "right_knee", angle = rightAngle)
        )

        return FormatData(
            sessionContext = createFakeSessionContext(currentSessionId, currentUserId, currentExerciseType),
            sensorData = sensorData,
            targetAngles = targetAngles,
            errors = emptyList()
        )
    }

    // -------------------------------------------------------------------------
    // 角度计算（三种运动模式）
    // -------------------------------------------------------------------------

    private fun computeAngles(now: Long): Pair<Double, Double> {
        val t = (now - sessionStartTime) / 1000.0
        return when (currentExerciseType) {
            "squat" -> {
                // 深蹲：左右完全同步，0° ~ 120°
                val v = (sin(t * 0.5) + 1.0) / 2.0 * 120.0
                v to v
            }
            "march_in_place" -> {
                // 原地踏步：左右反相交替，0° ~ 60°
                val left = (sin(t * 0.8) + 1.0) / 2.0 * 60.0
                val right = (sin(t * 0.8 + PI) + 1.0) / 2.0 * 60.0
                left to right
            }
            "lying_flat" -> {
                // 平躺抬腿：左右独立，0° ~ 90°，相位差 120°
                val left = (sin(t * 0.5) + 1.0) / 2.0 * 90.0
                val right = (sin(t * 0.5 + 2.0 * PI / 3.0) + 1.0) / 2.0 * 90.0
                left to right
            }
            else -> {
                // 默认：左右同步 0° ~ 90°
                val v = (sin(t * 0.5) + 1.0) / 2.0 * 90.0
                v to v
            }
        }
    }

    // -------------------------------------------------------------------------
    // 传感器样本生成（基于角度简化反推）
    // -------------------------------------------------------------------------

    private fun generateSensorSample(mac: String, timestamp: Long, leftAngle: Double, rightAngle: Double): SensorSample {
        val isLeft = mac in sensorMacsLeft
        val angle = if (isLeft) leftAngle else rightAngle
        val rad = Math.toRadians(angle)

        // 简化物理模型：角度越大，水平加速度分量越大
        val baseAccX = (kotlin.math.sin(rad) * 2.0).toFloat()
        val baseAccZ = 9.8f + (kotlin.math.cos(rad) * 1.0f).toFloat()

        return SensorSample(
            /* 1 */ timestamp,
            /* 2 */ mac,
            /* 3 */ "",                         // deviceName（Controller 未使用）
            /* 4 */ baseAccX + randomNoise(0.1f),
            /* 5 */ randomNoise(0.1f),
            /* 6 */ baseAccZ + randomNoise(0.2f),
            /* 7 */ randomNoise(0.5f),
            /* 8 */ (angle * 0.02).toFloat() + randomNoise(0.2f),
            /* 9 */ randomNoise(0.3f),
            /* 10 */ if (isLeft) angle.toFloat() else 0f,   // roll
            /* 11 */ if (!isLeft) angle.toFloat() else 0f,  // pitch
            /* 12 */ randomNoise(5f)                        // yaw
        )
    }

    // -------------------------------------------------------------------------
    // 辅助
    // -------------------------------------------------------------------------

    private fun randomNoise(amplitude: Float): Float {
        return Random.nextFloat() * 2 * amplitude - amplitude
    }

    /**
     * 构造假的 SessionContext。
     * 若下方构造参数与实际 SessionContext 不符，请手动修改。
     */
    private fun createFakeSessionContext(sessionId: Int, userId: Int, payloadStatus: String): SessionContext {
        return SessionContext(
            sessionId = sessionId,
            userId = userId,
            sensorJointMapping = emptyMap(),
            payloadStatus = payloadStatus
        )
    }
}