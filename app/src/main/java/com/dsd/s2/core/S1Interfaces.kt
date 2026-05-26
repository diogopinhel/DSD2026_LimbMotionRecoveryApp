package com.dsd.s2.core

import com.dsd.s1.model.SensorSample
import com.dsd.s1.model.SensorStatus

interface S1DataSource {
    fun read(): List<SensorSample>
    fun status(): SensorStatus
}

interface S1SessionControl {
    fun start(sessionMetaData: Map<String, Any>): Map<String, Any?>
    fun stop(): Map<String, Any?>
}

interface S1Module {
    val sensor: S1DataSource
    val session: S1SessionControl
}
