package com.openguard.core

import com.openguard.core.detection.ThreatSeverity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [ThreatSeverity] enum — verifies the severity ordering, level values,
 * and that all six expected values exist.
 *
 * PCI DSS 4.0 / OWASP MASVS: severity ordering must be consistent so that
 * `highestSeverity` aggregation and filtering produce deterministic results.
 */
class ThreatSeverityTest {

    // ── Level values ──────────────────────────────────────────────────────────

    @Test
    fun `NONE has level 0`() {
        assertEquals(0, ThreatSeverity.NONE.level)
    }

    @Test
    fun `INFO has level 1`() {
        assertEquals(1, ThreatSeverity.INFO.level)
    }

    @Test
    fun `LOW has level 2`() {
        assertEquals(2, ThreatSeverity.LOW.level)
    }

    @Test
    fun `MEDIUM has level 3`() {
        assertEquals(3, ThreatSeverity.MEDIUM.level)
    }

    @Test
    fun `HIGH has level 4`() {
        assertEquals(4, ThreatSeverity.HIGH.level)
    }

    @Test
    fun `CRITICAL has level 5`() {
        assertEquals(5, ThreatSeverity.CRITICAL.level)
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `severity level values are strictly increasing in declaration order`() {
        val severities = ThreatSeverity.entries
        for (i in 0 until severities.size - 1) {
            assertTrue(
                severities[i].level < severities[i + 1].level,
                "Expected ${severities[i]}.level (${severities[i].level}) " +
                    "< ${severities[i + 1]}.level (${severities[i + 1]}.level)"
            )
        }
    }

    @Test
    fun `CRITICAL level is greater than HIGH level`() {
        assertTrue(ThreatSeverity.CRITICAL.level > ThreatSeverity.HIGH.level)
    }

    @Test
    fun `HIGH level is greater than MEDIUM level`() {
        assertTrue(ThreatSeverity.HIGH.level > ThreatSeverity.MEDIUM.level)
    }

    @Test
    fun `MEDIUM level is greater than LOW level`() {
        assertTrue(ThreatSeverity.MEDIUM.level > ThreatSeverity.LOW.level)
    }

    @Test
    fun `LOW level is greater than INFO level`() {
        assertTrue(ThreatSeverity.LOW.level > ThreatSeverity.INFO.level)
    }

    @Test
    fun `INFO level is greater than NONE level`() {
        assertTrue(ThreatSeverity.INFO.level > ThreatSeverity.NONE.level)
    }

    // ── Enum compareTo (ordinal-based) ────────────────────────────────────────

    @Test
    fun `CRITICAL is greater than all other severities by enum compareTo`() {
        val others = ThreatSeverity.entries.filter { it != ThreatSeverity.CRITICAL }
        for (other in others) {
            assertTrue(
                ThreatSeverity.CRITICAL > other,
                "Expected CRITICAL > $other"
            )
        }
    }

    @Test
    fun `NONE is less than all other severities by enum compareTo`() {
        val others = ThreatSeverity.entries.filter { it != ThreatSeverity.NONE }
        for (other in others) {
            assertTrue(
                ThreatSeverity.NONE < other,
                "Expected NONE < $other"
            )
        }
    }

    @Test
    fun `maxOf two severities returns the higher one`() {
        assertEquals(ThreatSeverity.CRITICAL, maxOf(ThreatSeverity.HIGH, ThreatSeverity.CRITICAL))
        assertEquals(ThreatSeverity.HIGH, maxOf(ThreatSeverity.LOW, ThreatSeverity.HIGH))
        assertEquals(ThreatSeverity.MEDIUM, maxOf(ThreatSeverity.MEDIUM, ThreatSeverity.INFO))
    }

    // ── Completeness ──────────────────────────────────────────────────────────

    @Test
    fun `there are exactly 6 severity levels`() {
        assertEquals(6, ThreatSeverity.entries.size)
    }

    @Test
    fun `all expected severity names are present`() {
        val names = ThreatSeverity.entries.map { it.name }.toSet()
        val expected = setOf("NONE", "INFO", "LOW", "MEDIUM", "HIGH", "CRITICAL")
        assertEquals(expected, names)
    }
}
