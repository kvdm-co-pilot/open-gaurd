package com.openguard.android.detection

import android.os.Build
import android.os.Debug
import com.openguard.core.OpenGuardConfig
import com.openguard.core.api.DetectionApi
import com.openguard.core.detection.DetectionResult
import com.openguard.core.detection.Platform
import com.openguard.core.detection.ThreatEvent
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Android implementation of [DetectionApi].
 *
 * Uses a layered detection approach for each threat type. Multiple independent
 * checks are used to improve reliability and resistance to bypass.
 */
internal class AndroidDetectionApi(private val config: OpenGuardConfig) : DetectionApi {

    override suspend fun checkAll(): DetectionResult = withContext(Dispatchers.IO) {
        val threats = mutableListOf<ThreatEvent>()
        val detectionConfig = config.detection

        if (detectionConfig.rootJailbreakDetection.enabled) {
            threats.addAll(checkRootJailbreak().threats)
        }
        if (detectionConfig.debuggerDetection.enabled) {
            threats.addAll(checkDebugger().threats)
        }
        if (detectionConfig.hookDetection.enabled) {
            threats.addAll(checkHooks().threats)
        }
        if (detectionConfig.emulatorSimulatorDetection.enabled) {
            threats.addAll(checkEmulatorSimulator().threats)
        }
        if (detectionConfig.tamperDetection.enabled) {
            threats.addAll(checkTamper().threats)
        }

        DetectionResult(threats = threats)
    }

    override suspend fun checkRootJailbreak(): DetectionResult = withContext(Dispatchers.IO) {
        val evidence = mutableMapOf<String, String>()

        // Layer 1: Check for su binary in common paths
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su",
        )
        val foundSuPath = suPaths.firstOrNull { File(it).exists() }
        if (foundSuPath != null) {
            evidence["su_binary_path"] = foundSuPath
        }

        // Layer 2: Check build tags for test-keys
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            evidence["build_tags"] = buildTags
        }

        // Layer 3: Check for known root-related packages
        val rootPackages = listOf(
            "com.topjohnwu.magisk",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "eu.chainfire.supersu",
            "com.kingroot.kinguser",
            "com.kingo.root",
        )
        // Package check is done in a separate detector to avoid requiring Context here.
        // See RootPackageDetector for the full implementation.

        // Layer 4: Check Magisk-specific paths
        val magiskPaths = listOf(
            "/sbin/.magisk",
            "/sbin/.core/mirror",
            "/sbin/.core/img",
            "/data/adb/magisk",
        )
        val foundMagiskPath = magiskPaths.firstOrNull { File(it).exists() }
        if (foundMagiskPath != null) {
            evidence["magisk_path"] = foundMagiskPath
        }

        // Layer 5: Check if /system is mounted read-write (indicator of root)
        // This is approximated by checking write access to normally read-only locations
        val systemWritable = try {
            val testFile = File("/system/openguard_test_${System.currentTimeMillis()}")
            testFile.createNewFile().also { if (it) testFile.delete() }
        } catch (_: Exception) {
            false
        }
        if (systemWritable) {
            evidence["system_partition_writable"] = "true"
        }

        if (evidence.isNotEmpty()) {
            DetectionResult(
                threats = listOf(
                    buildThreatEvent(
                        type = ThreatType.ROOT_DETECTED,
                        severity = ThreatSeverity.CRITICAL,
                        metadata = evidence,
                    )
                )
            )
        } else {
            DetectionResult.CLEAN
        }
    }

    override suspend fun checkDebugger(): DetectionResult = withContext(Dispatchers.IO) {
        val evidence = mutableMapOf<String, String>()

        // Check 1: Standard Android Debug API
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
            evidence["debug_api"] = "connected"
        }

        // Check 2: /proc/self/status TracerPid — non-zero means being traced
        try {
            val statusLines = File("/proc/self/status").readLines()
            val tracerPidLine = statusLines.firstOrNull { it.startsWith("TracerPid:") }
            val tracerPid = tracerPidLine?.substringAfter("TracerPid:")?.trim()?.toIntOrNull()
            if (tracerPid != null && tracerPid != 0) {
                evidence["tracer_pid"] = tracerPid.toString()
            }
        } catch (_: Exception) {
            // Cannot read /proc/self/status — treat as potentially suspicious
        }

        // Check 3: Check for JDWP debugger port (23946)
        // Note: Full port scan is done in native code — this is a placeholder
        val debuggable = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) &&
            (android.os.Debug.isDebuggerConnected())
        if (debuggable) {
            evidence["jdwp_connected"] = "true"
        }

        if (evidence.isNotEmpty()) {
            DetectionResult(
                threats = listOf(
                    buildThreatEvent(
                        type = ThreatType.DEBUGGER_DETECTED,
                        severity = ThreatSeverity.CRITICAL,
                        metadata = evidence,
                    )
                )
            )
        } else {
            DetectionResult.CLEAN
        }
    }

    override suspend fun checkHooks(): DetectionResult = withContext(Dispatchers.IO) {
        val evidence = mutableMapOf<String, String>()

        // Check 1: Scan /proc/self/maps for known hook frameworks
        try {
            val mapsContent = File("/proc/self/maps").readText()
            val hookSignatures = listOf(
                "frida" to "frida_agent",
                "xposed" to "XposedBridge",
                "substrate" to "libsubstrate",
                "dobby" to "libdobby",
            )
            hookSignatures.forEach { (name, signature) ->
                if (mapsContent.contains(signature, ignoreCase = true)) {
                    evidence["maps_$name"] = signature
                }
            }
        } catch (_: Exception) {
            // Could not read maps
        }

        // Check 2: Check for Frida server port (27042) — TCP connect attempt
        // Full implementation requires native socket code; placeholder here
        val fridaPortOpen = isFridaPortOpen()
        if (fridaPortOpen) {
            evidence["frida_port"] = "27042"
        }

        // Check 3: Scan thread names for Frida signatures
        try {
            val threadGroup = Thread.currentThread().threadGroup
            if (threadGroup != null) {
                val threads = Array(threadGroup.activeCount() + 10) { null as Thread? }
                threadGroup.enumerate(threads, true)
                val fridaThreadNames = listOf("gum-js-loop", "gmain", "gdbus", "pool-frida")
                threads.filterNotNull().forEach { thread ->
                    val name = thread.name.lowercase()
                    fridaThreadNames.forEach { signature ->
                        if (name.contains(signature)) {
                            evidence["frida_thread"] = thread.name
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Thread enumeration failed
        }

        if (evidence.isNotEmpty()) {
            DetectionResult(
                threats = listOf(
                    buildThreatEvent(
                        type = ThreatType.HOOK_DETECTED,
                        severity = ThreatSeverity.CRITICAL,
                        metadata = evidence,
                    )
                )
            )
        } else {
            DetectionResult.CLEAN
        }
    }

    override suspend fun checkEmulatorSimulator(): DetectionResult = withContext(Dispatchers.IO) {
        val evidence = mutableMapOf<String, String>()

        // Check build properties
        val buildModel = Build.MODEL.lowercase()
        val buildManufacturer = Build.MANUFACTURER.lowercase()
        val buildProduct = Build.PRODUCT.lowercase()
        val buildHardware = Build.HARDWARE.lowercase()
        val buildBrand = Build.BRAND.lowercase()
        val buildDevice = Build.DEVICE.lowercase()
        val buildFingerprint = Build.FINGERPRINT.lowercase()

        val emulatorIndicators = mapOf(
            "model" to buildModel,
            "manufacturer" to buildManufacturer,
            "product" to buildProduct,
            "hardware" to buildHardware,
            "brand" to buildBrand,
            "device" to buildDevice,
            "fingerprint" to buildFingerprint,
        )

        val emulatorKeywords = listOf(
            "generic", "unknown", "google_sdk", "emulator", "android sdk built for x86",
            "goldfish", "ranchu", "sdk_gphone", "vbox", "genymotion", "bluestacks",
            "nox", "andy ", "ttvm_", "droid4x",
        )

        emulatorIndicators.forEach { (key, value) ->
            emulatorKeywords.forEach { keyword ->
                if (value.contains(keyword)) {
                    evidence["build_$key"] = value
                }
            }
        }

        // QEMU-specific device files
        val qemuFiles = listOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
        )
        qemuFiles.firstOrNull { File(it).exists() }?.let {
            evidence["qemu_file"] = it
        }

        // Check if running on x86 architecture (common for emulators)
        val cpuAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        if (cpuAbi.contains("x86") && !cpuAbi.contains("arm")) {
            evidence["cpu_abi"] = cpuAbi
        }

        if (evidence.isNotEmpty()) {
            DetectionResult(
                threats = listOf(
                    buildThreatEvent(
                        type = ThreatType.EMULATOR_DETECTED,
                        severity = ThreatSeverity.HIGH,
                        metadata = evidence,
                    )
                )
            )
        } else {
            DetectionResult.CLEAN
        }
    }

    override suspend fun checkTamper(): DetectionResult = withContext(Dispatchers.IO) {
        // Signature verification requires Context which is injected at the Android level.
        // The actual implementation is in AndroidTamperDetector.
        // This base implementation returns clean; Context-aware check is in the Android module.
        DetectionResult.CLEAN
    }

    private fun isFridaPortOpen(): Boolean {
        return try {
            val socket = java.net.Socket()
            socket.connect(
                java.net.InetSocketAddress("127.0.0.1", 27042),
                100 // 100ms timeout
            )
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun buildThreatEvent(
        type: ThreatType,
        severity: ThreatSeverity,
        metadata: Map<String, String>,
    ): ThreatEvent = ThreatEvent(
        id = UUID.randomUUID().toString(),
        timestampMs = System.currentTimeMillis(),
        type = type,
        severity = severity,
        platform = Platform.ANDROID,
        sdkVersion = com.openguard.core.OpenGuard.VERSION,
        appVersion = "unknown",
        deviceId = getDeviceId(),
        metadata = metadata,
    )

    private fun getDeviceId(): String {
        // Return a stable, non-PII device identifier
        return Build.ID.hashCode().toString(16)
    }
}
