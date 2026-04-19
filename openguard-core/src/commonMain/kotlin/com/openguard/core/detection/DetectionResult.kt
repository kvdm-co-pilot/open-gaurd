package com.openguard.core.detection

/**
 * The result of running one or more threat detections.
 */
data class DetectionResult(
    /** All threats that were detected during this check. */
    val threats: List<ThreatEvent>,
) {
    /** Returns true if no threats were detected. */
    val isClean: Boolean get() = threats.isEmpty()

    /** Returns the highest severity among all detected threats. */
    val highestSeverity: ThreatSeverity
        get() = threats.maxOfOrNull { it.severity } ?: ThreatSeverity.NONE

    /** Returns threats filtered to a specific severity or higher. */
    fun threatsAtOrAbove(severity: ThreatSeverity): List<ThreatEvent> =
        threats.filter { it.severity.level >= severity.level }

    /** Returns true if any critical threats were detected. */
    val hasCriticalThreats: Boolean
        get() = threats.any { it.severity == ThreatSeverity.CRITICAL }

    companion object {
        /** Represents a clean (no threats detected) result. */
        val CLEAN = DetectionResult(threats = emptyList())
    }
}
