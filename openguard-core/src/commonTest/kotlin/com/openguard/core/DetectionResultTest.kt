package com.openguard.core

import com.openguard.core.detection.DetectionResult
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType
import com.openguard.core.fixtures.TestHelpers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [DetectionResult] — aggregation logic, clean-state helpers,
 * severity filtering, and the CLEAN singleton.
 *
 * PCI DSS 4.0 requirement: severity aggregation must correctly identify
 * the highest-severity threat so that the appropriate reaction is applied.
 */
class DetectionResultTest {

    // ── CLEAN singleton ───────────────────────────────────────────────────────

    @Test
    fun `CLEAN singleton has an empty threats list`() {
        assertTrue(DetectionResult.CLEAN.threats.isEmpty())
    }

    @Test
    fun `CLEAN singleton isClean is true`() {
        assertTrue(DetectionResult.CLEAN.isClean)
    }

    @Test
    fun `CLEAN singleton highestSeverity is NONE`() {
        assertEquals(ThreatSeverity.NONE, DetectionResult.CLEAN.highestSeverity)
    }

    @Test
    fun `CLEAN singleton hasCriticalThreats is false`() {
        assertFalse(DetectionResult.CLEAN.hasCriticalThreats)
    }

    @Test
    fun `CLEAN singleton threatsAtOrAbove returns empty list`() {
        assertTrue(DetectionResult.CLEAN.threatsAtOrAbove(ThreatSeverity.NONE).isEmpty())
        assertTrue(DetectionResult.CLEAN.threatsAtOrAbove(ThreatSeverity.INFO).isEmpty())
        assertTrue(DetectionResult.CLEAN.threatsAtOrAbove(ThreatSeverity.CRITICAL).isEmpty())
    }

    // ── isClean ───────────────────────────────────────────────────────────────

    @Test
    fun `isClean returns true when threats list is empty`() {
        val result = DetectionResult(threats = emptyList())
        assertTrue(result.isClean)
    }

    @Test
    fun `isClean returns false when at least one threat is present`() {
        val result = TestHelpers.createSingleThreatResult(ThreatSeverity.LOW)
        assertFalse(result.isClean)
    }

    // ── highestSeverity ───────────────────────────────────────────────────────

    @Test
    fun `highestSeverity returns NONE for empty threats list`() {
        val result = DetectionResult(threats = emptyList())
        assertEquals(ThreatSeverity.NONE, result.highestSeverity)
    }

    @Test
    fun `highestSeverity returns the only severity when list has one threat`() {
        assertEquals(
            ThreatSeverity.HIGH,
            TestHelpers.createSingleThreatResult(ThreatSeverity.HIGH).highestSeverity
        )
    }

    @Test
    fun `highestSeverity returns CRITICAL when a critical threat exists among others`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.LOW,
            ThreatSeverity.CRITICAL,
            ThreatSeverity.MEDIUM,
        )
        assertEquals(ThreatSeverity.CRITICAL, result.highestSeverity)
    }

    @Test
    fun `highestSeverity returns the maximum level across all threats`() {
        for (expectedMax in ThreatSeverity.entries) {
            if (expectedMax == ThreatSeverity.NONE) continue // NONE means no threats

            val result = TestHelpers.createMultiThreatResult(
                ThreatSeverity.NONE,  // included for realism
                expectedMax,
            )
            assertEquals(expectedMax, result.highestSeverity)
        }
    }

    // ── hasCriticalThreats ────────────────────────────────────────────────────

    @Test
    fun `hasCriticalThreats is false when no CRITICAL threat exists`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.LOW, ThreatSeverity.MEDIUM, ThreatSeverity.HIGH
        )
        assertFalse(result.hasCriticalThreats)
    }

    @Test
    fun `hasCriticalThreats is true when a CRITICAL threat exists`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.HIGH, ThreatSeverity.CRITICAL
        )
        assertTrue(result.hasCriticalThreats)
    }

    @Test
    fun `hasCriticalThreats is true even when CRITICAL is the only threat`() {
        val result = TestHelpers.createSingleThreatResult(ThreatSeverity.CRITICAL)
        assertTrue(result.hasCriticalThreats)
    }

    // ── threatsAtOrAbove ──────────────────────────────────────────────────────

    @Test
    fun `threatsAtOrAbove NONE returns all threats`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.LOW, ThreatSeverity.MEDIUM, ThreatSeverity.HIGH
        )
        assertEquals(3, result.threatsAtOrAbove(ThreatSeverity.NONE).size)
    }

    @Test
    fun `threatsAtOrAbove MEDIUM returns only MEDIUM and above`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.INFO,
            ThreatSeverity.LOW,
            ThreatSeverity.MEDIUM,
            ThreatSeverity.HIGH,
            ThreatSeverity.CRITICAL,
        )
        val filtered = result.threatsAtOrAbove(ThreatSeverity.MEDIUM)
        assertEquals(3, filtered.size)
        assertTrue(filtered.all { it.severity.level >= ThreatSeverity.MEDIUM.level })
    }

    @Test
    fun `threatsAtOrAbove CRITICAL returns only CRITICAL threats`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.LOW, ThreatSeverity.HIGH, ThreatSeverity.CRITICAL
        )
        val filtered = result.threatsAtOrAbove(ThreatSeverity.CRITICAL)
        assertEquals(1, filtered.size)
        assertEquals(ThreatSeverity.CRITICAL, filtered.first().severity)
    }

    @Test
    fun `threatsAtOrAbove returns empty when no threats meet the threshold`() {
        val result = TestHelpers.createMultiThreatResult(
            ThreatSeverity.LOW, ThreatSeverity.INFO
        )
        assertTrue(result.threatsAtOrAbove(ThreatSeverity.HIGH).isEmpty())
    }

    // ── Threats list contents ─────────────────────────────────────────────────

    @Test
    fun `DetectionResult preserves all threats in the list`() {
        val events = listOf(
            TestHelpers.createThreatEvent(id = "e1", type = ThreatType.ROOT_DETECTED),
            TestHelpers.createThreatEvent(id = "e2", type = ThreatType.HOOK_DETECTED),
            TestHelpers.createThreatEvent(id = "e3", type = ThreatType.DEBUGGER_DETECTED),
        )
        val result = DetectionResult(threats = events)
        assertEquals(3, result.threats.size)
        assertEquals("e1", result.threats[0].id)
        assertEquals("e2", result.threats[1].id)
        assertEquals("e3", result.threats[2].id)
    }

    @Test
    fun `two DetectionResults with equal threats lists are equal`() {
        val threats = listOf(
            TestHelpers.createThreatEvent(id = "shared-id", timestampMs = 100L)
        )
        val result1 = DetectionResult(threats = threats)
        val result2 = DetectionResult(threats = threats)
        assertEquals(result1, result2)
    }
}
