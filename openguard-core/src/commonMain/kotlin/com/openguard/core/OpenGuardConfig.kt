package com.openguard.core

import com.openguard.core.detection.DetectionConfig
import com.openguard.core.network.NetworkConfig
import com.openguard.core.storage.AuditLogConfig
import com.openguard.core.detection.ThreatEvent
import com.openguard.core.detection.ThreatSeverity

/**
 * Main configuration for the OpenGuard SDK.
 *
 * Build using the [OpenGuardConfig] DSL function:
 * ```kotlin
 * OpenGuardConfig {
 *     detection { ... }
 *     network { ... }
 *     auditLog { ... }
 * }
 * ```
 */
class OpenGuardConfig internal constructor(
    val detection: DetectionConfig,
    val network: NetworkConfig,
    val auditLog: AuditLogConfig,
    val threatResponseCallbacks: ThreatResponseCallbacks,
) {
    companion object {
        /**
         * Returns a configuration with all security features enabled using secure defaults.
         * Suitable as a starting point; customize as needed.
         */
        fun secureDefaults(): OpenGuardConfig = OpenGuardConfig(
            detection = DetectionConfig.secureDefaults(),
            network = NetworkConfig.default(),
            auditLog = AuditLogConfig.default(),
            threatResponseCallbacks = ThreatResponseCallbacks(),
        )
    }
}

/**
 * DSL builder for [OpenGuardConfig].
 */
class OpenGuardConfigBuilder {
    private var detectionConfig = DetectionConfig.secureDefaults()
    private var networkConfig = NetworkConfig.default()
    private var auditLogConfig = AuditLogConfig.default()
    private val threatResponseCallbacks = ThreatResponseCallbacks()

    fun detection(block: DetectionConfig.Builder.() -> Unit) {
        detectionConfig = DetectionConfig.Builder().apply(block).build()
    }

    fun network(block: NetworkConfig.Builder.() -> Unit) {
        networkConfig = NetworkConfig.Builder().apply(block).build()
    }

    fun auditLog(block: AuditLogConfig.Builder.() -> Unit) {
        auditLogConfig = AuditLogConfig.Builder().apply(block).build()
    }

    fun threatResponse(block: ThreatResponseCallbacks.Builder.() -> Unit) {
        val builder = ThreatResponseCallbacks.Builder()
        builder.block()
        builder.applyTo(threatResponseCallbacks)
    }

    internal fun build(): OpenGuardConfig = OpenGuardConfig(
        detection = detectionConfig,
        network = networkConfig,
        auditLog = auditLogConfig,
        threatResponseCallbacks = threatResponseCallbacks,
    )
}

/**
 * Creates an [OpenGuardConfig] using the DSL builder.
 */
fun OpenGuardConfig(block: OpenGuardConfigBuilder.() -> Unit): OpenGuardConfig =
    OpenGuardConfigBuilder().apply(block).build()

/**
 * Callbacks invoked when threats are detected. Called on the main thread.
 */
class ThreatResponseCallbacks {
    internal var onCriticalThreat: ((ThreatEvent) -> Unit)? = null
    internal var onHighThreat: ((ThreatEvent) -> Unit)? = null
    internal var onMediumThreat: ((ThreatEvent) -> Unit)? = null
    internal var onAnyThreat: ((ThreatEvent) -> Unit)? = null

    class Builder {
        private var onCriticalThreat: ((ThreatEvent) -> Unit)? = null
        private var onHighThreat: ((ThreatEvent) -> Unit)? = null
        private var onMediumThreat: ((ThreatEvent) -> Unit)? = null
        private var onAnyThreat: ((ThreatEvent) -> Unit)? = null

        fun onCriticalThreat(callback: (ThreatEvent) -> Unit) {
            onCriticalThreat = callback
        }

        fun onHighThreat(callback: (ThreatEvent) -> Unit) {
            onHighThreat = callback
        }

        fun onMediumThreat(callback: (ThreatEvent) -> Unit) {
            onMediumThreat = callback
        }

        fun onAnyThreat(callback: (ThreatEvent) -> Unit) {
            onAnyThreat = callback
        }

        internal fun applyTo(target: ThreatResponseCallbacks) {
            target.onCriticalThreat = onCriticalThreat
            target.onHighThreat = onHighThreat
            target.onMediumThreat = onMediumThreat
            target.onAnyThreat = onAnyThreat
        }
    }

    /**
     * Dispatches a [ThreatEvent] to the appropriate registered callback.
     */
    internal fun dispatch(event: ThreatEvent) {
        onAnyThreat?.invoke(event)
        when (event.severity) {
            ThreatSeverity.CRITICAL -> onCriticalThreat?.invoke(event)
            ThreatSeverity.HIGH -> onHighThreat?.invoke(event)
            ThreatSeverity.MEDIUM -> onMediumThreat?.invoke(event)
            else -> Unit
        }
    }
}
