package com.openguard.core.fixtures

import com.openguard.core.api.DetectionApi
import com.openguard.core.detection.DetectionResult

/**
 * A configurable fake implementation of [DetectionApi] for use in unit tests.
 *
 * All check methods return pre-configured [DetectionResult] values.
 * Call counts are tracked so callers can assert how many times each method was invoked.
 */
class FakeDetectionApi(
    private var checkAllResult: DetectionResult = DetectionResult.CLEAN,
    private var checkRootJailbreakResult: DetectionResult = DetectionResult.CLEAN,
    private var checkDebuggerResult: DetectionResult = DetectionResult.CLEAN,
    private var checkHooksResult: DetectionResult = DetectionResult.CLEAN,
    private var checkEmulatorSimulatorResult: DetectionResult = DetectionResult.CLEAN,
    private var checkTamperResult: DetectionResult = DetectionResult.CLEAN,
) : DetectionApi {

    /** Number of times [checkAll] was called. */
    var checkAllCallCount: Int = 0
        private set

    /** Number of times [checkRootJailbreak] was called. */
    var checkRootCallCount: Int = 0
        private set

    /** Number of times [checkDebugger] was called. */
    var checkDebuggerCallCount: Int = 0
        private set

    /** Number of times [checkHooks] was called. */
    var checkHooksCallCount: Int = 0
        private set

    /** Number of times [checkEmulatorSimulator] was called. */
    var checkEmulatorCallCount: Int = 0
        private set

    /** Number of times [checkTamper] was called. */
    var checkTamperCallCount: Int = 0
        private set

    override suspend fun checkAll(): DetectionResult {
        checkAllCallCount++
        return checkAllResult
    }

    override suspend fun checkRootJailbreak(): DetectionResult {
        checkRootCallCount++
        return checkRootJailbreakResult
    }

    override suspend fun checkDebugger(): DetectionResult {
        checkDebuggerCallCount++
        return checkDebuggerResult
    }

    override suspend fun checkHooks(): DetectionResult {
        checkHooksCallCount++
        return checkHooksResult
    }

    override suspend fun checkEmulatorSimulator(): DetectionResult {
        checkEmulatorCallCount++
        return checkEmulatorSimulatorResult
    }

    override suspend fun checkTamper(): DetectionResult {
        checkTamperCallCount++
        return checkTamperResult
    }

    // --- Configuration helpers ---

    fun setCheckAllResult(result: DetectionResult) {
        checkAllResult = result
    }

    fun setCheckRootJailbreakResult(result: DetectionResult) {
        checkRootJailbreakResult = result
    }

    fun setCheckDebuggerResult(result: DetectionResult) {
        checkDebuggerResult = result
    }

    fun setCheckHooksResult(result: DetectionResult) {
        checkHooksResult = result
    }

    fun setCheckEmulatorSimulatorResult(result: DetectionResult) {
        checkEmulatorSimulatorResult = result
    }

    fun setCheckTamperResult(result: DetectionResult) {
        checkTamperResult = result
    }

    fun reset() {
        checkAllCallCount = 0
        checkRootCallCount = 0
        checkDebuggerCallCount = 0
        checkHooksCallCount = 0
        checkEmulatorCallCount = 0
        checkTamperCallCount = 0
        checkAllResult = DetectionResult.CLEAN
        checkRootJailbreakResult = DetectionResult.CLEAN
        checkDebuggerResult = DetectionResult.CLEAN
        checkHooksResult = DetectionResult.CLEAN
        checkEmulatorSimulatorResult = DetectionResult.CLEAN
        checkTamperResult = DetectionResult.CLEAN
    }
}
