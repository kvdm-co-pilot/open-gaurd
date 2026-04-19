package com.openguard.core.api

import com.openguard.core.detection.DetectionResult

/**
 * API for performing Runtime Application Self-Protection (RASP) checks.
 *
 * All functions are suspend functions and run off the main thread.
 * Results are returned as [DetectionResult] containing any detected [ThreatEvent]s.
 */
interface DetectionApi {

    /**
     * Runs all enabled threat detections and returns a combined result.
     *
     * This is the recommended way to perform a full security check. Individual
     * check methods are also available for targeted use cases.
     */
    suspend fun checkAll(): DetectionResult

    /**
     * Checks if the device is rooted (Android) or jailbroken (iOS).
     *
     * Uses a layered approach: file system checks, system property checks,
     * package manager checks, and native JNI checks (Android) or
     * kernel integrity checks (iOS).
     */
    suspend fun checkRootJailbreak(): DetectionResult

    /**
     * Checks if a debugger is attached to the application process.
     *
     * On Android: checks `Debug.isDebuggerConnected()`, `/proc/self/status` TracerPid,
     * and timing-based anomalies.
     * On iOS: checks `sysctl` P_TRACED flag and `getppid()`.
     */
    suspend fun checkDebugger(): DetectionResult

    /**
     * Checks if a runtime hooking framework is active.
     *
     * Detects: Frida (Android + iOS), Xposed/LSPosed/EdXposed (Android),
     * Cydia Substrate/MobileSubstrate (iOS).
     */
    suspend fun checkHooks(): DetectionResult

    /**
     * Checks if the app is running on an emulator (Android) or simulator (iOS).
     *
     * Emulator detection uses build properties, sensor availability, and
     * CPU architecture checks. Simulator detection uses runtime environment analysis.
     */
    suspend fun checkEmulatorSimulator(): DetectionResult

    /**
     * Checks if the application binary or resources have been tampered with.
     *
     * On Android: verifies the APK signing certificate and optionally compares
     * against a known-good hash.
     * On iOS: validates the code signature using `SecStaticCodeCheckValidity`.
     */
    suspend fun checkTamper(): DetectionResult
}
