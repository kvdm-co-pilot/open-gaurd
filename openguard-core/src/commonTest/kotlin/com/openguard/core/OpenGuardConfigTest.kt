package com.openguard.core

import com.openguard.core.detection.DetectionConfig
import com.openguard.core.detection.ThreatReaction
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType
import com.openguard.core.fixtures.TestHelpers
import com.openguard.core.network.TlsVersion
import com.openguard.core.storage.AuditLogConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests for [OpenGuardConfig] DSL builder and [ThreatResponseCallbacks].
 *
 * PCI DSS 4.0 / OWASP MASVS: The configuration must default to maximum protection
 * and only relax security when explicitly requested by the integrator.
 */
class OpenGuardConfigTest {

    // ── secureDefaults() ──────────────────────────────────────────────────────

    @Test
    fun `secureDefaults creates config with root detection enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.detection.rootJailbreakDetection.enabled)
    }

    @Test
    fun `secureDefaults creates config with debugger detection enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.detection.debuggerDetection.enabled)
    }

    @Test
    fun `secureDefaults creates config with hook detection enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.detection.hookDetection.enabled)
    }

    @Test
    fun `secureDefaults creates config with emulator detection enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.detection.emulatorSimulatorDetection.enabled)
    }

    @Test
    fun `secureDefaults creates config with tamper detection enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.detection.tamperDetection.enabled)
    }

    @Test
    fun `secureDefaults creates config with screenshot prevention enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.detection.screenshotPrevention.enabled)
    }

    @Test
    fun `secureDefaults uses BLOCK_AND_REPORT reaction for root detection`() {
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(ThreatReaction.BLOCK_AND_REPORT, config.detection.rootJailbreakDetection.reaction)
    }

    @Test
    fun `secureDefaults uses BLOCK_AND_REPORT reaction for debugger detection`() {
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(ThreatReaction.BLOCK_AND_REPORT, config.detection.debuggerDetection.reaction)
    }

    @Test
    fun `secureDefaults uses BLOCK_AND_REPORT reaction for hook detection`() {
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(ThreatReaction.BLOCK_AND_REPORT, config.detection.hookDetection.reaction)
    }

    @Test
    fun `secureDefaults uses WARN_AND_REPORT reaction for emulator detection`() {
        // Emulator is less severe than root/debug/hook, so default is warn-not-block
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(ThreatReaction.WARN_AND_REPORT, config.detection.emulatorSimulatorDetection.reaction)
    }

    @Test
    fun `secureDefaults uses BLOCK_AND_REPORT reaction for tamper detection`() {
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(ThreatReaction.BLOCK_AND_REPORT, config.detection.tamperDetection.reaction)
    }

    @Test
    fun `secureDefaults periodic interval is 30 seconds`() {
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(30L, config.detection.periodicCheckIntervalSeconds)
    }

    @Test
    fun `secureDefaults audit log is enabled`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.auditLog.enabled)
    }

    @Test
    fun `secureDefaults audit log signs events`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.auditLog.signEvents)
    }

    @Test
    fun `secureDefaults audit log has no remote endpoint`() {
        val config = OpenGuardConfig.secureDefaults()
        assertNull(config.auditLog.remoteEndpoint)
    }

    @Test
    fun `secureDefaults network config detects MITM`() {
        val config = OpenGuardConfig.secureDefaults()
        assertTrue(config.network.detectMitm)
    }

    @Test
    fun `secureDefaults network minimum TLS version is TLS 1 2`() {
        val config = OpenGuardConfig.secureDefaults()
        assertEquals(TlsVersion.TLS_1_2, config.network.minimumTlsVersion)
    }

    // ── DSL builder overrides ──────────────────────────────────────────────────

    @Test
    fun `DSL builder can disable root detection`() {
        val config = OpenGuardConfig {
            detection {
                rootJailbreakDetection {
                    enabled = false
                }
            }
        }
        assertFalse(config.detection.rootJailbreakDetection.enabled)
    }

    @Test
    fun `DSL builder can override root detection reaction`() {
        val config = OpenGuardConfig {
            detection {
                rootJailbreakDetection {
                    enabled = true
                    reaction = ThreatReaction.LOG_ONLY
                }
            }
        }
        assertEquals(ThreatReaction.LOG_ONLY, config.detection.rootJailbreakDetection.reaction)
    }

    @Test
    fun `DSL builder can disable debugger detection`() {
        val config = OpenGuardConfig {
            detection {
                debuggerDetection {
                    enabled = false
                }
            }
        }
        assertFalse(config.detection.debuggerDetection.enabled)
    }

    @Test
    fun `DSL builder can disable hook detection`() {
        val config = OpenGuardConfig {
            detection {
                hookDetection {
                    enabled = false
                    reaction = ThreatReaction.WARN_AND_REPORT
                }
            }
        }
        assertFalse(config.detection.hookDetection.enabled)
        assertEquals(ThreatReaction.WARN_AND_REPORT, config.detection.hookDetection.reaction)
    }

    @Test
    fun `DSL builder can disable emulator detection`() {
        val config = OpenGuardConfig {
            detection {
                emulatorSimulatorDetection {
                    enabled = false
                }
            }
        }
        assertFalse(config.detection.emulatorSimulatorDetection.enabled)
    }

    @Test
    fun `DSL builder can override periodic check interval`() {
        val config = OpenGuardConfig {
            detection {
                periodicCheckIntervalSeconds(60L)
            }
        }
        assertEquals(60L, config.detection.periodicCheckIntervalSeconds)
    }

    @Test
    fun `DSL builder can set audit log remote endpoint`() {
        val config = OpenGuardConfig {
            auditLog {
                remoteEndpoint = "https://audit.example.com/events"
            }
        }
        assertEquals("https://audit.example.com/events", config.auditLog.remoteEndpoint)
    }

    @Test
    fun `DSL builder can disable audit log event signing`() {
        val config = OpenGuardConfig {
            auditLog {
                signEvents = false
            }
        }
        assertFalse(config.auditLog.signEvents)
    }

    @Test
    fun `DSL builder can set audit log retention days`() {
        val config = OpenGuardConfig {
            auditLog {
                localRetentionDays = 30
            }
        }
        assertEquals(30, config.auditLog.localRetentionDays)
    }

    @Test
    fun `DSL builder can disable audit log entirely`() {
        val config = OpenGuardConfig {
            auditLog {
                enabled = false
            }
        }
        assertFalse(config.auditLog.enabled)
    }

    @Test
    fun `DSL builder can set minimum TLS version to 1 3`() {
        val config = OpenGuardConfig {
            network {
                minimumTlsVersion = TlsVersion.TLS_1_3
            }
        }
        assertEquals(TlsVersion.TLS_1_3, config.network.minimumTlsVersion)
    }

    @Test
    fun `DSL builder can disable MITM detection`() {
        val config = OpenGuardConfig {
            network {
                detectMitm = false
            }
        }
        assertFalse(config.network.detectMitm)
    }

    @Test
    fun `DSL builder can set tamper detection expected signature hash`() {
        val hash = "AAABBBCCC111222333=="
        val config = OpenGuardConfig {
            detection {
                tamperDetection {
                    expectedSignatureHash = hash
                }
            }
        }
        assertEquals(hash, config.detection.tamperDetection.expectedSignatureHash)
    }

    @Test
    fun `DSL builder can disable screenshot prevention`() {
        val config = OpenGuardConfig {
            detection {
                screenshotPrevention {
                    enabled = false
                }
            }
        }
        assertFalse(config.detection.screenshotPrevention.enabled)
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    fun `periodicCheckIntervalSeconds rejects values below 10`() {
        assertFailsWith<IllegalArgumentException> {
            DetectionConfig.Builder().periodicCheckIntervalSeconds(9L)
        }
    }

    @Test
    fun `periodicCheckIntervalSeconds rejects zero`() {
        assertFailsWith<IllegalArgumentException> {
            DetectionConfig.Builder().periodicCheckIntervalSeconds(0L)
        }
    }

    @Test
    fun `periodicCheckIntervalSeconds rejects negative values`() {
        assertFailsWith<IllegalArgumentException> {
            DetectionConfig.Builder().periodicCheckIntervalSeconds(-1L)
        }
    }

    @Test
    fun `periodicCheckIntervalSeconds accepts exactly 10`() {
        val config = OpenGuardConfig {
            detection {
                periodicCheckIntervalSeconds(10L)
            }
        }
        assertEquals(10L, config.detection.periodicCheckIntervalSeconds)
    }

    @Test
    fun `periodicCheckIntervalSeconds error message mentions 10 second minimum`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            DetectionConfig.Builder().periodicCheckIntervalSeconds(5L)
        }
        assertTrue(
            exception.message?.contains("10") == true,
            "Error message should mention the 10 second minimum: ${exception.message}"
        )
    }

    // ── ThreatResponseCallbacks ───────────────────────────────────────────────

    @Test
    fun `onAnyThreat callback is invoked for all threat severities`() {
        val receivedEvents = mutableListOf<String>()

        val config = OpenGuardConfig {
            threatResponse {
                onAnyThreat { event -> receivedEvents.add(event.id) }
            }
        }

        val severities = listOf(
            ThreatSeverity.INFO,
            ThreatSeverity.LOW,
            ThreatSeverity.MEDIUM,
            ThreatSeverity.HIGH,
            ThreatSeverity.CRITICAL,
        )
        for (severity in severities) {
            config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = severity))
        }

        assertEquals(5, receivedEvents.size)
    }

    @Test
    fun `onCriticalThreat callback is invoked only for CRITICAL threats`() {
        var criticalCount = 0

        val config = OpenGuardConfig {
            threatResponse {
                onCriticalThreat { criticalCount++ }
            }
        }

        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.HIGH))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.CRITICAL))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.MEDIUM))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.CRITICAL))

        assertEquals(2, criticalCount)
    }

    @Test
    fun `onHighThreat callback is invoked only for HIGH threats`() {
        var highCount = 0

        val config = OpenGuardConfig {
            threatResponse {
                onHighThreat { highCount++ }
            }
        }

        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.CRITICAL))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.HIGH))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.MEDIUM))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.HIGH))

        assertEquals(2, highCount)
    }

    @Test
    fun `onMediumThreat callback is invoked only for MEDIUM threats`() {
        var mediumCount = 0

        val config = OpenGuardConfig {
            threatResponse {
                onMediumThreat { mediumCount++ }
            }
        }

        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.MEDIUM))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.HIGH))
        config.threatResponseCallbacks.dispatch(TestHelpers.createThreatEvent(severity = ThreatSeverity.MEDIUM))

        assertEquals(2, mediumCount)
    }

    @Test
    fun `onAnyThreat and onCriticalThreat are both invoked for CRITICAL events`() {
        var anyCount = 0
        var criticalCount = 0

        val config = OpenGuardConfig {
            threatResponse {
                onAnyThreat { anyCount++ }
                onCriticalThreat { criticalCount++ }
            }
        }

        config.threatResponseCallbacks.dispatch(
            TestHelpers.createThreatEvent(severity = ThreatSeverity.CRITICAL)
        )

        assertEquals(1, anyCount, "onAnyThreat should fire for CRITICAL")
        assertEquals(1, criticalCount, "onCriticalThreat should fire for CRITICAL")
    }

    @Test
    fun `callbacks are optional - no crash when no callbacks are registered`() {
        val config = OpenGuardConfig {} // no threatResponse block
        // Should not throw
        config.threatResponseCallbacks.dispatch(
            TestHelpers.createThreatEvent(severity = ThreatSeverity.CRITICAL)
        )
    }

    @Test
    fun `LOW severity event does not invoke onCriticalThreat or onHighThreat or onMediumThreat`() {
        var criticalCount = 0
        var highCount = 0
        var mediumCount = 0

        val config = OpenGuardConfig {
            threatResponse {
                onCriticalThreat { criticalCount++ }
                onHighThreat { highCount++ }
                onMediumThreat { mediumCount++ }
            }
        }

        config.threatResponseCallbacks.dispatch(
            TestHelpers.createThreatEvent(severity = ThreatSeverity.LOW)
        )

        assertEquals(0, criticalCount)
        assertEquals(0, highCount)
        assertEquals(0, mediumCount)
    }

    // ── ThreatType coverage ───────────────────────────────────────────────────

    @Test
    fun `callbacks receive the correct ThreatType from dispatched events`() {
        val receivedTypes = mutableListOf<ThreatType>()

        val config = OpenGuardConfig {
            threatResponse {
                onAnyThreat { receivedTypes.add(it.type) }
            }
        }

        config.threatResponseCallbacks.dispatch(
            TestHelpers.createThreatEvent(type = ThreatType.ROOT_DETECTED, severity = ThreatSeverity.INFO)
        )
        config.threatResponseCallbacks.dispatch(
            TestHelpers.createThreatEvent(type = ThreatType.HOOK_DETECTED, severity = ThreatSeverity.INFO)
        )

        assertEquals(listOf(ThreatType.ROOT_DETECTED, ThreatType.HOOK_DETECTED), receivedTypes)
    }
}
