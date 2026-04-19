package com.openguard.core.detection

/**
 * A security threat event emitted by the OpenGuard RASP engine.
 *
 * Events are tamper-evident — each event contains an HMAC-SHA256 [signature]
 * over the event payload, allowing server-side integrity verification.
 */
data class ThreatEvent(
    /** Unique identifier for this event (UUID v4). */
    val id: String,
    /** Unix timestamp in milliseconds when the threat was detected. */
    val timestampMs: Long,
    /** The type of threat detected. */
    val type: ThreatType,
    /** Severity level of the detected threat. */
    val severity: ThreatSeverity,
    /** Platform where the detection occurred. */
    val platform: Platform,
    /** Version of the OpenGuard SDK. */
    val sdkVersion: String,
    /** Version of the host application. */
    val appVersion: String,
    /** A stable, hashed device identifier (not personally identifiable). */
    val deviceId: String,
    /** Additional detection-specific metadata (e.g., which method triggered the detection). */
    val metadata: Map<String, String> = emptyMap(),
    /**
     * HMAC-SHA256 signature over the event payload for tamper-evident audit logs.
     * Computed using the SDK's audit signing key.
     */
    val signature: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreatEvent) return false
        return id == other.id && timestampMs == other.timestampMs && type == other.type
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String =
        "ThreatEvent(id=$id, type=$type, severity=$severity, platform=$platform, ts=$timestampMs)"
}

/**
 * Target platform.
 */
enum class Platform {
    ANDROID,
    IOS,
}
