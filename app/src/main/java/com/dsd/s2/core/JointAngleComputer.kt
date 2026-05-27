package com.dsd.s2.core

import com.dsd.s1.model.SensorSample
import com.dsd.s2.model.TargetAngle
import kotlin.math.*

const val BINDMODE_BACK = "back"
const val BINDMODE_PORT = "port"
const val BINDMODE_SCREEN = "screen"

private const val PAIR_TIME_THRESHOLD_MS = 100L
private const val MAX_SAMPLE_AGE_MS = 2000L

fun parseJointPairs(mapping: Map<String, String>): Map<String, Pair<String, String>> {
    val jointSensors = mutableMapOf<String, MutableList<String>>()
    for ((sensorId, jointName) in mapping) {
        jointSensors.getOrPut(jointName) { mutableListOf() }.add(sensorId)
    }
    val pairs = mutableMapOf<String, Pair<String, String>>()
    for ((jointName, sensorIds) in jointSensors) {
        if (sensorIds.size >= 2) {
            pairs[jointName] = Pair(sensorIds[0], sensorIds[1])
        }
    }
    return pairs
}

fun screenNormalWorld(sample: SensorSample): Triple<Double, Double, Double> {
    val r = Math.toRadians(sample.roll.toDouble())
    val p = Math.toRadians(sample.pitch.toDouble())
    val sr = sin(r); val cr = cos(r)
    val sp = sin(p); val cp = cos(p)
    return Triple(sp * cr, -sr, cp * cr)
}

fun longEdgeWorld(sample: SensorSample): Triple<Double, Double, Double> {
    val r = Math.toRadians(sample.roll.toDouble())
    val p = Math.toRadians(sample.pitch.toDouble())
    val y = Math.toRadians(sample.yaw.toDouble())
    val sr = sin(r); val cr = cos(r)
    val sp = sin(p); val cp = cos(p)
    val sy = sin(y); val cy = cos(y)
    return Triple(
        cy * sp * sr - sy * cr,
        sy * sp * sr + cy * cr,
        cp * sr
    )
}

fun computeAngleBetweenSegments(
    sampleA: SensorSample,
    sampleB: SensorSample,
    bindMode: String
): Double {
    val (va, vb) = if (bindMode == BINDMODE_PORT) {
        longEdgeWorld(sampleA) to longEdgeWorld(sampleB)
    } else {
        screenNormalWorld(sampleA) to screenNormalWorld(sampleB)
    }
    var dot = va.first * vb.first + va.second * vb.second + va.third * vb.third
    dot = dot.coerceIn(-1.0, 1.0)
    return Math.toDegrees(acos(dot))
}

class JointAngleComputer(
    private val jointPairs: Map<String, Pair<String, String>>,
    private val bindMode: String = BINDMODE_BACK,
    private val timeThresholdMs: Long = PAIR_TIME_THRESHOLD_MS
) {
    private val cache = mutableMapOf<String, MutableList<Pair<Long, SensorSample>>>()
    private val consumed = mutableMapOf<String, MutableSet<Long>>()

    fun feedSamples(samples: List<SensorSample>): List<TargetAngle> {
        for (s in samples) {
            cache.getOrPut(s.deviceId) { mutableListOf() }.add(s.timestamp to s)
        }
        val angles = mutableListOf<TargetAngle>()
        for ((jointName, pair) in jointPairs) {
            angles.addAll(matchAndCompute(jointName, pair.first, pair.second))
        }
        pruneOldSamples()
        return angles
    }

    private fun matchAndCompute(jointName: String, idA: String, idB: String): List<TargetAngle> {
        val samplesA = cache[idA] ?: return emptyList()
        val samplesB = cache[idB] ?: return emptyList()
        if (samplesA.isEmpty() || samplesB.isEmpty()) return emptyList()

        val consumedA = consumed.getOrPut(idA) { mutableSetOf() }
        val consumedB = consumed.getOrPut(idB) { mutableSetOf() }

        val results = mutableListOf<TargetAngle>()
        val usedB = mutableSetOf<Int>()

        for ((aTs, aSample) in samplesA) {
            if (aTs in consumedA) continue
            var bestIdx = -1
            var bestTs = 0L
            var bestSample: SensorSample? = null
            var bestDiff = timeThresholdMs + 1

            for ((bIdx, pair) in samplesB.withIndex()) {
                val (bTs, bSample) = pair
                if (bIdx in usedB || bTs in consumedB) continue
                val diff = abs(aTs - bTs)
                if (diff <= timeThresholdMs && diff < bestDiff) {
                    bestDiff = diff
                    bestIdx = bIdx
                    bestTs = bTs
                    bestSample = bSample
                }
            }

            if (bestSample != null) {
                usedB.add(bestIdx)
                val angle = computeAngleBetweenSegments(aSample, bestSample, bindMode)
                val avgTs = (aTs + bestTs) / 2
                results.add(TargetAngle(
                    timestamp = avgTs,
                    angleID = jointName,
                    angle = (angle * 100).roundToLong() / 100.0
                ))
                consumedA.add(aTs)
                consumedB.add(bestTs)
            }
        }
        return results
    }

    private fun pruneOldSamples() {
        val cutoff = System.currentTimeMillis() - MAX_SAMPLE_AGE_MS
        for (sensorId in cache.keys.toList()) {
            cache[sensorId]?.removeAll { it.first <= cutoff }
            consumed[sensorId]?.removeAll { it <= cutoff }
        }
    }
}
