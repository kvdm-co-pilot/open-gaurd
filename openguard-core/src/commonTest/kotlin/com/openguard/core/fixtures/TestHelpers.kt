package com.openguard.core.fixtures

import com.openguard.core.detection.DetectionResult
import com.openguard.core.detection.Platform
import com.openguard.core.detection.ThreatEvent
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType

/**
 * Test helper utilities for creating test data objects.
 */
object TestHelpers {

    private var idCounter = 0

    /** Creates a [ThreatEvent] with sensible test defaults. Only specify what matters for the test. */
    fun createThreatEvent(
        id: String = "test-id-${idCounter++}",
        timestampMs: Long = 1_000_000L,
        type: ThreatType = ThreatType.ROOT_DETECTED,
        severity: ThreatSeverity = ThreatSeverity.HIGH,
        platform: Platform = Platform.ANDROID,
        sdkVersion: String = "0.1.0-alpha",
        appVersion: String = "1.0.0",
        deviceId: String = "test-device-id",
        metadata: Map<String, String> = emptyMap(),
        signature: ByteArray = ByteArray(0),
    ): ThreatEvent = ThreatEvent(
        id = id,
        timestampMs = timestampMs,
        type = type,
        severity = severity,
        platform = platform,
        sdkVersion = sdkVersion,
        appVersion = appVersion,
        deviceId = deviceId,
        metadata = metadata,
        signature = signature,
    )

    /** Creates a [DetectionResult] containing a single [ThreatEvent] with the given severity. */
    fun createSingleThreatResult(
        severity: ThreatSeverity,
        type: ThreatType = ThreatType.ROOT_DETECTED,
    ): DetectionResult = DetectionResult(
        threats = listOf(createThreatEvent(severity = severity, type = type))
    )

    /** Creates a [DetectionResult] containing threats at each of the given severities. */
    fun createMultiThreatResult(vararg severities: ThreatSeverity): DetectionResult = DetectionResult(
        threats = severities.mapIndexed { idx, severity ->
            createThreatEvent(id = "multi-$idx", severity = severity)
        }
    )
}
