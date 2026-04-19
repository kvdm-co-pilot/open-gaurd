package com.openguard.core.detection

/**
 * How the SDK reacts when a specific threat is detected.
 */
enum class ThreatReaction {
    /**
     * Log the event silently. The app continues to function normally.
     * Use for low-risk detections in production or for analytics.
     */
    LOG_ONLY,

    /**
     * Report the threat to the configured audit log endpoint.
     * The app continues to function normally.
     */
    REPORT_ONLY,

    /**
     * Report the threat AND invoke the registered threat callback.
     * The app can choose how to react in the callback.
     */
    WARN_AND_REPORT,

    /**
     * Block the current operation, report the threat, and invoke the callback.
     * Use for high-severity threats where the operation should not proceed.
     */
    BLOCK_AND_REPORT,

    /**
     * Immediately terminate the application.
     * Use for critical threats where continuing is a security risk.
     * This is the most aggressive response and should only be used
     * after careful consideration of the user experience impact.
     */
    TERMINATE_APP,
}
