package com.openguard.core.detection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.openguard.core.api.DetectionApi

/**
 * The core detection engine that coordinates periodic and on-demand threat checks.
 *
 * Runs all enabled detectors on a background coroutine and dispatches
 * [ThreatEvent]s to the configured callback.
 */
internal class DetectionEngine(
    private val detectionApi: DetectionApi,
    private val config: DetectionConfig,
    private val onThreat: (ThreatEvent) -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var periodicJob: Job? = null

    /**
     * Starts the detection engine. Runs an initial check immediately,
     * then continues with periodic checks at the configured interval.
     */
    fun start() {
        periodicJob = scope.launch {
            // Initial check on startup
            runDetections()
            // Periodic checks
            while (isActive) {
                delay(config.periodicCheckIntervalSeconds * 1_000L)
                runDetections()
            }
        }
    }

    /**
     * Stops the detection engine and cancels all pending checks.
     */
    fun stop() {
        periodicJob?.cancel()
        periodicJob = null
    }

    private suspend fun runDetections() {
        val result = detectionApi.checkAll()
        result.threats.forEach { event -> onThreat(event) }
    }
}
