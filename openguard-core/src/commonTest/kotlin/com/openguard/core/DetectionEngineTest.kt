package com.openguard.core

import com.openguard.core.detection.DetectionConfig
import com.openguard.core.detection.DetectionEngine
import com.openguard.core.detection.DetectionResult
import com.openguard.core.detection.ThreatEvent
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType
import com.openguard.core.fixtures.FakeDetectionApi
import com.openguard.core.fixtures.TestHelpers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [DetectionEngine] — start/stop lifecycle and threat dispatch.
 *
 * Uses [FakeDetectionApi] to control what threats are returned, and
 * [runBlocking] + [delay] to give the engine's background coroutine time
 * to complete its initial check before asserting.
 */
class DetectionEngineTest {

    /**
     * Creates a [DetectionConfig] with a very long periodic interval so that
     * only the initial check fires during tests (not subsequent periodic ones).
     */
    private fun testDetectionConfig(intervalSeconds: Long = 3600L): DetectionConfig =
        DetectionConfig.Builder().apply {
            periodicCheckIntervalSeconds(intervalSeconds)
        }.build()

    // ── Lifecycle: no crash ───────────────────────────────────────────────────

    @Test
    fun `DetectionEngine can be created without errors`() {
        val fake = FakeDetectionApi()
        // Should not throw
        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = {},
        )
        engine.stop() // safe to stop before starting
    }

    @Test
    fun `stop can be called before start without crashing`() {
        val engine = DetectionEngine(
            detectionApi = FakeDetectionApi(),
            config = testDetectionConfig(),
            onThreat = {},
        )
        engine.stop() // idempotent — must not throw
        engine.stop() // second call also safe
    }

    @Test
    fun `stop can be called multiple times after start without crashing`() = runBlocking {
        val engine = DetectionEngine(
            detectionApi = FakeDetectionApi(),
            config = testDetectionConfig(),
            onThreat = {},
        )
        engine.start()
        delay(100)
        engine.stop()
        engine.stop() // second call must not throw
    }

    // ── Initial check fires immediately on start ───────────────────────────────

    @Test
    fun `checkAll is called on start`() = runBlocking {
        val fake = FakeDetectionApi()
        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = {},
        )

        engine.start()
        delay(300) // give background coroutine time to run the initial check
        engine.stop()

        assertTrue(
            fake.checkAllCallCount >= 1,
            "checkAll should have been called at least once after start(), but was called ${fake.checkAllCallCount} time(s)"
        )
    }

    // ── Threat dispatch ───────────────────────────────────────────────────────

    @Test
    fun `threats from checkAll are dispatched to the onThreat callback`() = runBlocking {
        val expectedEvent = TestHelpers.createThreatEvent(
            id = "engine-test-evt",
            severity = ThreatSeverity.CRITICAL,
            type = ThreatType.ROOT_DETECTED,
        )
        val fake = FakeDetectionApi(
            checkAllResult = DetectionResult(threats = listOf(expectedEvent))
        )
        val received = mutableListOf<ThreatEvent>()

        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = { received.add(it) },
        )

        engine.start()
        delay(300) // wait for initial check
        engine.stop()

        assertTrue(received.isNotEmpty(), "At least one event should have been dispatched")
        // Verify the dispatched event matches by identity fields
        val dispatched = received.first()
        assertEquals(expectedEvent.id, dispatched.id)
        assertEquals(expectedEvent.type, dispatched.type)
    }

    @Test
    fun `multiple threats in a single checkAll result are all dispatched`() = runBlocking {
        val event1 = TestHelpers.createThreatEvent(id = "multi-1", type = ThreatType.ROOT_DETECTED)
        val event2 = TestHelpers.createThreatEvent(id = "multi-2", type = ThreatType.HOOK_DETECTED)
        val event3 = TestHelpers.createThreatEvent(id = "multi-3", type = ThreatType.DEBUGGER_DETECTED)

        val fake = FakeDetectionApi(
            checkAllResult = DetectionResult(threats = listOf(event1, event2, event3))
        )
        val received = mutableListOf<ThreatEvent>()

        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = { received.add(it) },
        )

        engine.start()
        delay(300)
        engine.stop()

        assertEquals(3, received.size, "All 3 threat events should be dispatched")
        val receivedIds = received.map { it.id }.toSet()
        assertTrue(receivedIds.contains("multi-1"))
        assertTrue(receivedIds.contains("multi-2"))
        assertTrue(receivedIds.contains("multi-3"))
    }

    @Test
    fun `clean result does not invoke the onThreat callback`() = runBlocking {
        val fake = FakeDetectionApi(checkAllResult = DetectionResult.CLEAN)
        var callbackInvoked = false

        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = { callbackInvoked = true },
        )

        engine.start()
        delay(300)
        engine.stop()

        assertFalse(callbackInvoked, "onThreat should NOT be called when checkAll returns CLEAN")
    }

    // ── Periodic checks ───────────────────────────────────────────────────────

    @Test
    fun `after stop no further checks are triggered`() = runBlocking {
        val fake = FakeDetectionApi()
        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(intervalSeconds = 10L), // short-ish interval
            onThreat = {},
        )

        engine.start()
        delay(200)       // let initial check run
        engine.stop()
        val countAfterStop = fake.checkAllCallCount
        delay(200)       // wait to see if more checks fire (they should NOT)

        assertEquals(
            countAfterStop,
            fake.checkAllCallCount,
            "No additional checkAll calls should happen after stop()"
        )
    }

    // ── Threat severity varieties ─────────────────────────────────────────────

    @Test
    fun `onThreat receives HIGH severity events correctly`() = runBlocking {
        val highEvent = TestHelpers.createThreatEvent(
            id = "high-evt",
            severity = ThreatSeverity.HIGH,
        )
        val fake = FakeDetectionApi(checkAllResult = DetectionResult(threats = listOf(highEvent)))
        val received = mutableListOf<ThreatSeverity>()

        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = { received.add(it.severity) },
        )

        engine.start()
        delay(300)
        engine.stop()

        assertTrue(received.contains(ThreatSeverity.HIGH))
    }

    @Test
    fun `onThreat receives MEDIUM severity events correctly`() = runBlocking {
        val medEvent = TestHelpers.createThreatEvent(
            id = "med-evt",
            severity = ThreatSeverity.MEDIUM,
        )
        val fake = FakeDetectionApi(checkAllResult = DetectionResult(threats = listOf(medEvent)))
        val received = mutableListOf<ThreatSeverity>()

        val engine = DetectionEngine(
            detectionApi = fake,
            config = testDetectionConfig(),
            onThreat = { received.add(it.severity) },
        )

        engine.start()
        delay(300)
        engine.stop()

        assertTrue(received.contains(ThreatSeverity.MEDIUM))
    }
}
