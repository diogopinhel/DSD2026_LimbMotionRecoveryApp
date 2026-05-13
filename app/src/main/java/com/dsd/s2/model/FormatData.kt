package com.dsd.s2.model

import com.dsd.s1.model.SensorSample

data class FormatData(
    val sessionContext: SessionContext,
    val sensorData: List<SensorSample>,
    val targetAngles: List<TargetAngle>,
    val errors: List<ErrorEvent>
)
