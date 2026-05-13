package com.dsd.s2

import android.content.Context
import android.util.Log
import com.dsd.s2.buffer.AsyncBuffer
import com.dsd.s2.core.*
import com.dsd.s2.model.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "S2Module"

class S2SessionControl(
    private var s1Real: S1Module?,
    private val s1Sim: S1Module,
    private val context: Context? = null
) {
    var useSimulator: Boolean = s1Real == null
        private set

    var isActive: Boolean = false
        private set

    var buffer: AsyncBuffer? = null
        private set

    private var core: DataAcqCore? = null
    private var sessionContext: SessionContext? = null
    private var startTime: String? = null
    private var currentS1: S1Module? = null

    fun setMode(useSimulator: Boolean) {
        check(!isActive) { "Cannot switch mode during active session" }
        check(useSimulator || s1Real != null) { "No real BLE sensors configured." }
        this.useSimulator = useSimulator
        Log.i(TAG, "Data source mode set to: ${if (useSimulator) "simulator" else "real BLE"}")
    }

    fun setRealS1(s1RealModule: S1Module) {
        check(!isActive) { "Cannot reconfigure sensors during active session" }
        s1Real = s1RealModule
        Log.i(TAG, "Real S1 module updated")
    }

    fun start(
        sessionId: Int,
        userId: Int,
        sensorJointMapping: Map<String, String> = emptyMap(),
        payloadStatus: String = ""
    ): StartResult {
        if (isActive) {
            return StartResult(success = false, errorMessage = "session_already_active")
        }

        currentS1 = if (useSimulator) s1Sim else s1Real

        val s1Result = currentS1!!.session.start(
            mapOf("sessionId" to sessionId, "payloadStatus" to payloadStatus)
        )
        if (s1Result["success"] != true) {
            return StartResult(
                success = false,
                errorMessage = s1Result["error"]?.toString() ?: "s1_start_failed"
            )
        }

        sessionContext = SessionContext(
            sessionId = sessionId,
            userId = userId,
            sensorJointMapping = sensorJointMapping,
            payloadStatus = payloadStatus
        )

        buffer = AsyncBuffer(sessionContext!!)

        var bindMode = BINDMODE_BACK
        if ("-" in payloadStatus) {
            val suffix = payloadStatus.substringAfterLast("-")
            if (suffix in listOf(BINDMODE_BACK, BINDMODE_PORT, BINDMODE_SCREEN)) {
                bindMode = suffix
            }
        }

        core = DataAcqCore(
            dataSource = currentS1!!.sensor,
            sensorJointMapping = sensorJointMapping,
            bindMode = bindMode,
            onSamples = { buffer!!.pushSamples(it) },
            onAngles = { buffer!!.pushAngles(it) },
            onError = { buffer!!.pushError(it) },
            context = context
        )
        core!!.start()

        startTime = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        isActive = true
        Log.i(TAG, "S2 session started: sessionId=$sessionId userId=$userId payloadStatus=$payloadStatus")
        return StartResult(success = true, errorMessage = null)
    }

    fun stop(): SessionSummary {
        check(isActive) { "No active session" }

        core!!.stop()
        currentS1!!.session.stop()

        val endTime = Instant.now().atOffset(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val summary = SessionSummary(
            sessionId = sessionContext!!.sessionId,
            sampleCount = core!!.sampleCount,
            errorCount = core!!.errorCount,
            startTime = startTime!!,
            endTime = endTime
        )

        Log.i(TAG, "S2 session stopped: ${summary.sampleCount} samples, ${summary.errorCount} errors")

        isActive = false
        core = null
        sessionContext = null
        startTime = null
        currentS1 = null

        return summary
    }
}

class S2DataProvider(private val sessionControl: S2SessionControl) {
    fun read(): FormatData {
        val buf = sessionControl.buffer
            ?: throw RuntimeException("No active session — cannot read data")
        return buf.drain("s2_data_read")
    }
}

class S2Module(
    s1RealModule: S1Module?,
    s1SimModule: S1Module,
    context: Context? = null
) {
    val session = S2SessionControl(s1RealModule, s1SimModule, context)
    val data = S2DataProvider(session)
}
