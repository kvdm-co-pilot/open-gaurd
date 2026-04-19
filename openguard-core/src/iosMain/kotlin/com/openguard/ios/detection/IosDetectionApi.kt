package com.openguard.ios.detection

import com.openguard.core.OpenGuard
import com.openguard.core.OpenGuardConfig
import com.openguard.core.api.DetectionApi
import com.openguard.core.detection.DetectionResult
import com.openguard.core.detection.Platform
import com.openguard.core.detection.ThreatEvent
import com.openguard.core.detection.ThreatSeverity
import com.openguard.core.detection.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIDevice
import platform.posix.getpid
import platform.posix.getppid
import kotlin.uuid.Uuid

/**
 * iOS implementation of [DetectionApi].
 *
 * Uses Kotlin/Native interop with iOS Foundation, UIKit, and Darwin frameworks
 * for jailbreak, debugger, hook, simulator, and tamper detection.
 */
internal class IosDetectionApi(private val config: OpenGuardConfig) : DetectionApi {

    override suspend fun checkAll(): DetectionResult = withContext(Dispatchers.Default) {
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

    override suspend fun checkRootJailbreak(): DetectionResult = withContext(Dispatchers.Default) {
        val evidence = mutableMapOf<String, String>()
        val fileManager = NSFileManager.defaultManager

        // Layer 1: Check for jailbreak-related files
        val jailbreakPaths = listOf(
            "/Applications/Cydia.app",
            "/Applications/Sileo.app",
            "/Applications/Zebra.app",
            "/Applications/Installer.app",
            "/Library/MobileSubstrate/MobileSubstrate.dylib",
            "/bin/bash",
            "/usr/sbin/sshd",
            "/etc/apt",
            "/private/var/lib/apt/",
            "/private/var/tmp/cydia.log",
            "/usr/bin/ssh",
            "/var/jb", // rootless jailbreak path
        )
        jailbreakPaths.forEach { path ->
            if (fileManager.fileExistsAtPath(path)) {
                evidence["jailbreak_file"] = path
            }
        }

        // Layer 2: Attempt to open Cydia URL scheme
        val cydiaUrl = NSURL.URLWithString("cydia://package/com.example")
        // Note: UIApplication.sharedApplication.canOpenURL requires main thread
        // This check is done via native bridging in production implementation

        // Layer 3: Attempt sandbox escape — writing to /private/
        val escapePath = "/private/openguard_jailbreak_test_${System.currentTimeMillis()}"
        val canEscape = try {
            fileManager.createFileAtPath(escapePath, null, null)
        } catch (_: Exception) {
            false
        }
        if (canEscape) {
            evidence["sandbox_escape"] = escapePath
            fileManager.removeItemAtPath(escapePath, null)
        }

        // Layer 4: Check for suspicious dynamic libraries
        val imageCount = platform.darwin.dyld_image_count()
        for (i in 0u until imageCount) {
            val imageName = platform.darwin.dyld_get_image_name(i)
            if (imageName != null) {
                val name = imageName.toString()
                val jailbreakLibSignatures = listOf(
                    "MobileSubstrate", "CydiaSubstrate", "SubstrateLoader",
                    "libswiHook", "libhooker",
                )
                jailbreakLibSignatures.forEach { sig ->
                    if (name.contains(sig, ignoreCase = true)) {
                        evidence["dylib_$sig"] = name
                    }
                }
            }
        }

        if (evidence.isNotEmpty()) {
            DetectionResult(
                threats = listOf(
                    buildThreatEvent(
                        type = ThreatType.JAILBREAK_DETECTED,
                        severity = ThreatSeverity.CRITICAL,
                        metadata = evidence,
                    )
                )
            )
        } else {
            DetectionResult.CLEAN
        }
    }

    override suspend fun checkDebugger(): DetectionResult = withContext(Dispatchers.Default) {
        val evidence = mutableMapOf<String, String>()

        // Check 1: getppid() — on a non-jailbroken device, parent is launchd (PID 1)
        val ppid = getppid()
        if (ppid != 1) {
            evidence["ppid"] = ppid.toString()
        }

        // Check 2: sysctl check for P_TRACED flag
        // This requires native interop — placeholder
        val isBeingDebugged = checkSysctlTracedFlag()
        if (isBeingDebugged) {
            evidence["sysctl_traced"] = "true"
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

    override suspend fun checkHooks(): DetectionResult = withContext(Dispatchers.Default) {
        val evidence = mutableMapOf<String, String>()

        // Check for Frida and other hooking frameworks in loaded images
        val imageCount = platform.darwin.dyld_image_count()
        for (i in 0u until imageCount) {
            val imageName = platform.darwin.dyld_get_image_name(i)
            if (imageName != null) {
                val name = imageName.toString()
                val hookSignatures = listOf(
                    "FridaGadget", "frida-gadget", "frida-agent",
                    "cynject", "libcycript",
                    "SubstrateInserter", "SubstrateBootstrap",
                    "libhooker",
                )
                hookSignatures.forEach { sig ->
                    if (name.contains(sig, ignoreCase = true)) {
                        evidence["hook_lib"] = name
                    }
                }
            }
        }

        // Check Frida port (27042)
        if (isFridaPortOpen()) {
            evidence["frida_port"] = "27042"
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

    override suspend fun checkEmulatorSimulator(): DetectionResult = withContext(Dispatchers.Default) {
        val evidence = mutableMapOf<String, String>()

        // Check: UIDevice model contains "Simulator"
        val model = UIDevice.currentDevice.model
        if (model.contains("Simulator", ignoreCase = true)) {
            evidence["device_model"] = model
        }

        // Check: targetEnvironment(simulator) — at compile time; runtime check via architecture
        // On Apple Silicon, simulator also runs arm64 so architecture alone is insufficient
        // Check for simulator-specific paths
        val simulatorPaths = listOf(
            "/Applications/Xcode.app",
        )
        val fileManager = NSFileManager.defaultManager
        simulatorPaths.firstOrNull { fileManager.fileExistsAtPath(it) }?.let {
            evidence["simulator_path"] = it
        }

        if (evidence.isNotEmpty()) {
            DetectionResult(
                threats = listOf(
                    buildThreatEvent(
                        type = ThreatType.SIMULATOR_DETECTED,
                        severity = ThreatSeverity.HIGH,
                        metadata = evidence,
                    )
                )
            )
        } else {
            DetectionResult.CLEAN
        }
    }

    override suspend fun checkTamper(): DetectionResult = withContext(Dispatchers.Default) {
        // Code signature validation requires Security framework interop
        // Full implementation uses SecStaticCodeCheckValidity via native bridging
        // Placeholder for Phase 1 — implemented in Phase 2 with native interop
        DetectionResult.CLEAN
    }

    private fun checkSysctlTracedFlag(): Boolean {
        // Full implementation uses sysctl(CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid())
        // and checks kp_proc.p_flag & P_TRACED
        // Requires native interop — returns false as a safe default in this skeleton
        return false
    }

    private fun isFridaPortOpen(): Boolean {
        return try {
            // TODO: Implement TCP connect to 127.0.0.1:27042 via POSIX sockets
            // Returns false as safe default in this skeleton
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun buildThreatEvent(
        type: ThreatType,
        severity: ThreatSeverity,
        metadata: Map<String, String>,
    ): ThreatEvent = ThreatEvent(
        id = Uuid.random().toString(),
        timestampMs = System.currentTimeMillis(),
        type = type,
        severity = severity,
        platform = Platform.IOS,
        sdkVersion = OpenGuard.VERSION,
        appVersion = "unknown",
        deviceId = getDeviceId(),
        metadata = metadata,
    )

    private fun getDeviceId(): String {
        return UIDevice.currentDevice.identifierForVendor?.UUIDString?.hashCode()?.toString(16)
            ?: "unknown"
    }
}
