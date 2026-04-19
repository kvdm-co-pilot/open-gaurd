package com.openguard.core

import com.openguard.core.detection.Platform
import com.openguard.core.detection.ThreatEvent
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType
import com.openguard.core.fixtures.TestHelpers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Tests for [ThreatEvent] — creation, equality semantics, toString, defaults.
 *
 * OWASP MASVS-RESILIENCE: event identity (id + timestampMs + type) uniquely identifies
 * a threat for deduplication and audit log integrity.
 */
class ThreatEventTest {

    // ── Construction ──────────────────────────────────────────────────────────

    @Test
    fun `ThreatEvent can be created with all required fields`() {
        val event = ThreatEvent(
            id = "evt-001",
            timestampMs = 1_700_000_000_000L,
            type = ThreatType.ROOT_DETECTED,
            severity = ThreatSeverity.CRITICAL,
            platform = Platform.ANDROID,
            sdkVersion = "0.1.0-alpha",
            appVersion = "2.0.0",
            deviceId = "device-abc123",
        )

        assertEquals("evt-001", event.id)
        assertEquals(1_700_000_000_000L, event.timestampMs)
        assertEquals(ThreatType.ROOT_DETECTED, event.type)
        assertEquals(ThreatSeverity.CRITICAL, event.severity)
        assertEquals(Platform.ANDROID, event.platform)
        assertEquals("0.1.0-alpha", event.sdkVersion)
        assertEquals("2.0.0", event.appVersion)
        assertEquals("device-abc123", event.deviceId)
    }

    @Test
    fun `metadata defaults to empty map when not provided`() {
        val event = TestHelpers.createThreatEvent()
        assertTrue(event.metadata.isEmpty(), "Default metadata should be empty")
    }

    @Test
    fun `signature defaults to empty ByteArray when not provided`() {
        val event = TestHelpers.createThreatEvent()
        assertEquals(0, event.signature.size, "Default signature should be empty")
    }

    @Test
    fun `ThreatEvent stores metadata key-value pairs correctly`() {
        val meta = mapOf("su_path" to "/system/bin/su", "build_tags" to "test-keys")
        val event = TestHelpers.createThreatEvent(metadata = meta)

        assertEquals("/system/bin/su", event.metadata["su_path"])
        assertEquals("test-keys", event.metadata["build_tags"])
        assertEquals(2, event.metadata.size)
    }

    @Test
    fun `ThreatEvent stores HMAC signature bytes correctly`() {
        val sig = ByteArray(32) { it.toByte() }
        val event = TestHelpers.createThreatEvent(signature = sig)

        assertTrue(sig.contentEquals(event.signature))
        assertEquals(32, event.signature.size)
    }

    // ── Equality ──────────────────────────────────────────────────────────────

    @Test
    fun `two events with same id, timestampMs, and type are equal`() {
        val base = ThreatEvent(
            id = "same-id",
            timestampMs = 1_000L,
            type = ThreatType.DEBUGGER_DETECTED,
            severity = ThreatSeverity.CRITICAL,
            platform = Platform.ANDROID,
            sdkVersion = "0.1.0-alpha",
            appVersion = "1.0",
            deviceId = "device-1",
        )
        val sameIdentity = ThreatEvent(
            id = "same-id",
            timestampMs = 1_000L,
            type = ThreatType.DEBUGGER_DETECTED,
            severity = ThreatSeverity.LOW,          // different severity — should NOT affect equality
            platform = Platform.IOS,                // different platform — should NOT affect equality
            sdkVersion = "0.2.0",                   // different sdk version
            appVersion = "9.9",
            deviceId = "device-2",
        )

        assertEquals(base, sameIdentity)
    }

    @Test
    fun `two events with different IDs are not equal`() {
        val event1 = TestHelpers.createThreatEvent(id = "id-A")
        val event2 = TestHelpers.createThreatEvent(id = "id-B")

        assertNotEquals(event1, event2)
    }

    @Test
    fun `two events with different timestamps are not equal`() {
        val event1 = TestHelpers.createThreatEvent(timestampMs = 1_000L)
        val event2 = TestHelpers.createThreatEvent(timestampMs = 2_000L)

        assertNotEquals(event1, event2)
    }

    @Test
    fun `two events with different types are not equal`() {
        val event1 = TestHelpers.createThreatEvent(type = ThreatType.ROOT_DETECTED)
        val event2 = TestHelpers.createThreatEvent(type = ThreatType.HOOK_DETECTED)

        assertNotEquals(event1, event2)
    }

    @Test
    fun `an event is equal to itself`() {
        val event = TestHelpers.createThreatEvent()
        assertEquals(event, event)
    }

    @Test
    fun `an event is not equal to null`() {
        val event = TestHelpers.createThreatEvent()
        assertFalse(event.equals(null))
    }

    @Test
    fun `an event is not equal to an object of a different type`() {
        val event = TestHelpers.createThreatEvent()
        assertFalse(event.equals("not a ThreatEvent"))
    }

    // ── hashCode ──────────────────────────────────────────────────────────────

    @Test
    fun `equal events have the same hashCode`() {
        val event1 = ThreatEvent(
            id = "hash-id",
            timestampMs = 5_000L,
            type = ThreatType.EMULATOR_DETECTED,
            severity = ThreatSeverity.HIGH,
            platform = Platform.ANDROID,
            sdkVersion = "0.1.0-alpha",
            appVersion = "1.0",
            deviceId = "d1",
        )
        val event2 = ThreatEvent(
            id = "hash-id",
            timestampMs = 5_000L,
            type = ThreatType.EMULATOR_DETECTED,
            severity = ThreatSeverity.MEDIUM, // different
            platform = Platform.IOS,          // different
            sdkVersion = "0.2.0",             // different
            appVersion = "2.0",
            deviceId = "d2",
        )

        assertEquals(event1.hashCode(), event2.hashCode())
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    fun `toString includes event id`() {
        val event = TestHelpers.createThreatEvent(id = "evt-xyz")
        assertTrue(event.toString().contains("evt-xyz"), "toString should include the event id")
    }

    @Test
    fun `toString includes threat type`() {
        val event = TestHelpers.createThreatEvent(type = ThreatType.HOOK_DETECTED)
        assertTrue(
            event.toString().contains("HOOK_DETECTED"),
            "toString should include the threat type"
        )
    }

    @Test
    fun `toString includes severity`() {
        val event = TestHelpers.createThreatEvent(severity = ThreatSeverity.CRITICAL)
        assertTrue(
            event.toString().contains("CRITICAL"),
            "toString should include the severity"
        )
    }

    @Test
    fun `toString includes platform`() {
        val event = TestHelpers.createThreatEvent(platform = Platform.ANDROID)
        assertTrue(
            event.toString().contains("ANDROID"),
            "toString should include the platform"
        )
    }

    @Test
    fun `toString includes timestamp`() {
        val event = TestHelpers.createThreatEvent(timestampMs = 1_234_567L)
        assertTrue(
            event.toString().contains("1234567"),
            "toString should include the timestamp"
        )
    }

    // ── All ThreatType values ─────────────────────────────────────────────────

    @Test
    fun `ThreatEvent can be constructed for every ThreatType`() {
        for (type in ThreatType.entries) {
            val event = TestHelpers.createThreatEvent(type = type)
            assertEquals(type, event.type)
        }
    }

    // ── Both Platform values ──────────────────────────────────────────────────

    @Test
    fun `ThreatEvent supports ANDROID platform`() {
        val event = TestHelpers.createThreatEvent(platform = Platform.ANDROID)
        assertEquals(Platform.ANDROID, event.platform)
    }

    @Test
    fun `ThreatEvent supports IOS platform`() {
        val event = TestHelpers.createThreatEvent(platform = Platform.IOS)
        assertEquals(Platform.IOS, event.platform)
    }
}
