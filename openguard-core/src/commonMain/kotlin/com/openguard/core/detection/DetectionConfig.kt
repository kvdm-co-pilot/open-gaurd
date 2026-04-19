package com.openguard.core.detection

/**
 * Configuration for the detection engine.
 */
class DetectionConfig private constructor(
    val rootJailbreakDetection: DetectionFeatureConfig,
    val debuggerDetection: DetectionFeatureConfig,
    val hookDetection: DetectionFeatureConfig,
    val emulatorSimulatorDetection: DetectionFeatureConfig,
    val tamperDetection: TamperDetectionConfig,
    val screenshotPrevention: ScreenProtectionConfig,
    val periodicCheckIntervalSeconds: Long,
) {
    companion object {
        /** Creates a configuration with all security features enabled at maximum protection. */
        fun secureDefaults(): DetectionConfig = Builder().build()
    }

    class Builder {
        private var rootJailbreakDetection = DetectionFeatureConfig(
            enabled = true,
            reaction = ThreatReaction.BLOCK_AND_REPORT,
        )
        private var debuggerDetection = DetectionFeatureConfig(
            enabled = true,
            reaction = ThreatReaction.BLOCK_AND_REPORT,
        )
        private var hookDetection = DetectionFeatureConfig(
            enabled = true,
            reaction = ThreatReaction.BLOCK_AND_REPORT,
        )
        private var emulatorSimulatorDetection = DetectionFeatureConfig(
            enabled = true,
            reaction = ThreatReaction.WARN_AND_REPORT,
        )
        private var tamperDetection = TamperDetectionConfig(
            enabled = true,
            expectedSignatureHash = null,
            reaction = ThreatReaction.BLOCK_AND_REPORT,
        )
        private var screenshotPrevention = ScreenProtectionConfig(enabled = true)
        private var periodicCheckIntervalSeconds = 30L

        fun rootJailbreakDetection(block: DetectionFeatureConfig.Builder.() -> Unit) {
            rootJailbreakDetection = DetectionFeatureConfig.Builder().apply(block).build()
        }

        fun debuggerDetection(block: DetectionFeatureConfig.Builder.() -> Unit) {
            debuggerDetection = DetectionFeatureConfig.Builder().apply(block).build()
        }

        fun hookDetection(block: DetectionFeatureConfig.Builder.() -> Unit) {
            hookDetection = DetectionFeatureConfig.Builder().apply(block).build()
        }

        fun emulatorSimulatorDetection(block: DetectionFeatureConfig.Builder.() -> Unit) {
            emulatorSimulatorDetection = DetectionFeatureConfig.Builder().apply(block).build()
        }

        fun tamperDetection(block: TamperDetectionConfig.Builder.() -> Unit) {
            tamperDetection = TamperDetectionConfig.Builder().apply(block).build()
        }

        fun screenshotPrevention(block: ScreenProtectionConfig.Builder.() -> Unit) {
            screenshotPrevention = ScreenProtectionConfig.Builder().apply(block).build()
        }

        fun periodicCheckIntervalSeconds(seconds: Long) {
            require(seconds >= 10) { "Periodic check interval must be at least 10 seconds." }
            periodicCheckIntervalSeconds = seconds
        }

        internal fun build(): DetectionConfig = DetectionConfig(
            rootJailbreakDetection = rootJailbreakDetection,
            debuggerDetection = debuggerDetection,
            hookDetection = hookDetection,
            emulatorSimulatorDetection = emulatorSimulatorDetection,
            tamperDetection = tamperDetection,
            screenshotPrevention = screenshotPrevention,
            periodicCheckIntervalSeconds = periodicCheckIntervalSeconds,
        )
    }
}

/**
 * Configuration for a basic detection feature.
 */
data class DetectionFeatureConfig(
    val enabled: Boolean,
    val reaction: ThreatReaction,
) {
    class Builder {
        var enabled: Boolean = true
        var reaction: ThreatReaction = ThreatReaction.BLOCK_AND_REPORT

        internal fun build() = DetectionFeatureConfig(enabled = enabled, reaction = reaction)
    }
}

/**
 * Configuration for tamper detection (app signature verification).
 */
data class TamperDetectionConfig(
    val enabled: Boolean,
    /**
     * Expected SHA-256 hash of the signing certificate (Base64-encoded).
     * If null, the check uses the certificate embedded at first run.
     */
    val expectedSignatureHash: String?,
    val reaction: ThreatReaction,
) {
    class Builder {
        var enabled: Boolean = true
        var expectedSignatureHash: String? = null
        var reaction: ThreatReaction = ThreatReaction.BLOCK_AND_REPORT

        internal fun build() = TamperDetectionConfig(
            enabled = enabled,
            expectedSignatureHash = expectedSignatureHash,
            reaction = reaction,
        )
    }
}

/**
 * Configuration for screenshot and screen recording protection.
 */
data class ScreenProtectionConfig(
    val enabled: Boolean,
) {
    class Builder {
        var enabled: Boolean = true

        internal fun build() = ScreenProtectionConfig(enabled = enabled)
    }
}
