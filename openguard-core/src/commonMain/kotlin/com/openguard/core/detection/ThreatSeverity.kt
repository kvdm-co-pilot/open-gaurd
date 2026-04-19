package com.openguard.core.detection

/**
 * Severity level of a detected threat.
 */
enum class ThreatSeverity(val level: Int) {
    /** No threat detected. */
    NONE(0),
    /** Informational event, no action required. */
    INFO(1),
    /** Low severity — log and monitor. */
    LOW(2),
    /** Medium severity — warn user and report. */
    MEDIUM(3),
    /** High severity — restrict functionality and report. */
    HIGH(4),
    /** Critical severity — block operation and terminate session. */
    CRITICAL(5);

    operator fun compareTo(other: ThreatSeverity): Int = this.level - other.level
}
