package com.openguard.core.detection

/**
 * The type of threat that was detected.
 */
enum class ThreatType {
    /** Device is rooted (Android). */
    ROOT_DETECTED,
    /** Device is jailbroken (iOS). */
    JAILBREAK_DETECTED,
    /** A debugger is attached to the process. */
    DEBUGGER_DETECTED,
    /** A runtime hooking framework (Frida, Xposed, Substrate) was detected. */
    HOOK_DETECTED,
    /** The app is running on an emulator (Android). */
    EMULATOR_DETECTED,
    /** The app is running on a simulator (iOS). */
    SIMULATOR_DETECTED,
    /** The app binary or resources have been tampered with. */
    TAMPER_DETECTED,
    /** Certificate pinning validation failed. */
    CERT_PINNING_FAILED,
    /** The app has been repackaged (different signing certificate). */
    REPACKAGING_DETECTED,
    /** A screenshot was taken of a protected screen. */
    SCREENSHOT_PREVENTED,
    /** Screen recording was detected on a protected screen. */
    SCREEN_RECORDING_DETECTED,
    /** An overlay attack was detected on the payment screen. */
    OVERLAY_ATTACK_DETECTED,
    /** App integrity check failed (Play Integrity / App Attest). */
    INTEGRITY_CHECK_FAILED,
    /** An unknown or custom threat type. */
    UNKNOWN,
}
