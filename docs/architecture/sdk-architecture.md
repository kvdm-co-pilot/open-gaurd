# OpenGuard SDK — Architecture Design Document

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Status:** Draft — For Review

---

## 1. Overview

OpenGuard is an open-source Runtime Application Self-Protection (RASP) SDK for Kotlin Multiplatform (KMP) projects. It provides a unified API for Android and iOS security features, with shared business logic in Kotlin and platform-specific native implementations accessed via the `expect`/`actual` KMP mechanism.

### 1.1 Design Goals

1. **Single API surface** — One Kotlin API that works on both Android and iOS via KMP
2. **Platform-native implementations** — Each detection uses the most effective platform-native approach
3. **PCI DSS 4.0 compliance** — Every feature maps to a specific compliance requirement
4. **Non-blocking by default** — All checks are designed to run off the main thread
5. **Extensible** — Plugin/callback architecture allows custom checks and responses
6. **Zero-trust defaults** — Secure by default; opt-in to relaxed policies
7. **Minimal attack surface** — SDK itself is hardened and validated
8. **Observable** — Comprehensive threat event reporting

---

## 2. System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Consumer Application                          │
│  (Compose Multiplatform / Native Android or iOS)                │
└──────────────────────────┬──────────────────────────────────────┘
                           │ OpenGuard Public API
┌──────────────────────────▼──────────────────────────────────────┐
│                  openguard-core (KMP Module)                     │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐ │
│  │ Detection   │  │  Crypto      │  │  Network Security        │ │
│  │ Engine      │  │  API         │  │  (Pinning, TLS)          │ │
│  └──────┬──────┘  └──────┬───────┘  └──────────┬──────────────┘ │
│         │                │                      │                │
│  ┌──────▼──────┐  ┌──────▼───────┐  ┌──────────▼──────────────┐ │
│  │ Threat      │  │ Secure       │  │  Audit Logging           │ │
│  │ Response    │  │ Storage API  │  │                          │ │
│  └──────┬──────┘  └──────────────┘  └──────────────────────────┘ │
│         │                                                         │
│  ┌──────▼────────────────────────────────────────────────────┐  │
│  │               expect/actual Platform Bridge               │  │
│  └──────┬────────────────────────────────┬───────────────────┘  │
└─────────┼────────────────────────────────┼─────────────────────┘
          │                                │
┌─────────▼──────────┐          ┌──────────▼──────────────┐
│  androidMain        │          │  iosMain                 │
│  (Kotlin/JVM)       │          │  (Kotlin/Native)         │
│                     │          │                          │
│  • RootDetector     │          │  • JailbreakDetector     │
│  • ArtHookDetector  │          │  • SubstrateDetector     │
│  • FridaDetector    │          │  • FridaDetector         │
│  • DebugDetector    │          │  • DebugDetector         │
│  • TamperDetector   │          │  • TamperDetector        │
│  • EmulatorDetector │          │  • SimulatorDetector     │
│  • AndroidKeystore  │          │  • SecureEnclave         │
│  • OkHttp Pinning   │          │  • URLSession Pinning    │
└─────────────────────┘          └──────────────────────────┘
```

---

## 3. Module Structure

```
open-guard/
├── openguard-core/                 # KMP shared module (published to Maven)
│   ├── src/
│   │   ├── commonMain/kotlin/
│   │   │   └── com/openguard/core/
│   │   │       ├── OpenGuard.kt            # Main entry point
│   │   │       ├── OpenGuardConfig.kt      # Configuration DSL
│   │   │       ├── api/
│   │   │       │   ├── DetectionApi.kt     # expect interface
│   │   │       │   ├── CryptoApi.kt        # expect interface
│   │   │       │   ├── NetworkApi.kt       # expect interface
│   │   │       │   └── StorageApi.kt       # expect interface
│   │   │       ├── detection/
│   │   │       │   ├── ThreatDetector.kt   # Common detector interface
│   │   │       │   ├── ThreatEvent.kt      # Threat event model
│   │   │       │   ├── ThreatSeverity.kt   # Severity enum
│   │   │       │   └── DetectionResult.kt  # Result wrapper
│   │   │       ├── crypto/
│   │   │       │   ├── CipherSpec.kt       # Cipher specification
│   │   │       │   └── KeySpec.kt          # Key specification
│   │   │       ├── network/
│   │   │       │   ├── PinningConfig.kt    # Pinning configuration
│   │   │       │   └── TlsConfig.kt        # TLS configuration
│   │   │       └── storage/
│   │   │           └── SecureStorageKey.kt # Storage key definition
│   │   ├── androidMain/kotlin/
│   │   │   └── com/openguard/
│   │   │       ├── detection/
│   │   │       │   ├── RootDetector.kt
│   │   │       │   ├── DebugDetector.kt
│   │   │       │   ├── HookDetector.kt
│   │   │       │   ├── EmulatorDetector.kt
│   │   │       │   └── TamperDetector.kt
│   │   │       ├── crypto/
│   │   │       │   └── AndroidCryptoImpl.kt
│   │   │       ├── network/
│   │   │       │   └── AndroidNetworkImpl.kt
│   │   │       └── storage/
│   │   │           └── AndroidSecureStorageImpl.kt
│   │   └── iosMain/kotlin/
│   │       └── com/openguard/
│   │           ├── detection/
│   │           │   ├── JailbreakDetector.kt
│   │           │   ├── DebugDetector.kt
│   │           │   ├── HookDetector.kt
│   │           │   ├── SimulatorDetector.kt
│   │           │   └── TamperDetector.kt
│   │           ├── crypto/
│   │           │   └── IosCryptoImpl.kt
│   │           ├── network/
│   │           │   └── IosNetworkImpl.kt
│   │           └── storage/
│   │               └── IosSecureStorageImpl.kt
│   └── build.gradle.kts
│
├── openguard-android/              # Android-only extras (optional)
│   └── build.gradle.kts
│
├── sample/                         # Demo application
│   ├── androidApp/
│   └── iosApp/
│
├── docs/                           # Documentation
├── build.gradle.kts                # Root build file
└── settings.gradle.kts             # Project settings
```

---

## 4. Public API Design

### 4.1 Initialization

```kotlin
// In Application.onCreate() (Android) or AppDelegate (iOS via Kotlin)
OpenGuard.initialize(
    context = context, // Android only, omit on iOS
    config = OpenGuardConfig {
        // Detection configuration
        detection {
            rootJailbreakDetection {
                enabled = true
                reaction = ThreatReaction.BLOCK_AND_REPORT
            }
            debuggerDetection {
                enabled = true
                reaction = ThreatReaction.BLOCK_AND_REPORT
            }
            hookDetection {
                enabled = true
                reaction = ThreatReaction.BLOCK_AND_REPORT
            }
            emulatorSimulatorDetection {
                enabled = true
                reaction = ThreatReaction.WARN_AND_REPORT
            }
            tamperDetection {
                enabled = true
                expectedSignatureHash = "sha256/..."
                reaction = ThreatReaction.BLOCK_AND_REPORT
            }
            screenshotPrevention {
                enabled = true
            }
        }
        // Network security
        network {
            certificatePinning {
                pin("api.example.com") {
                    addSha256Pin("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    addSha256Pin("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=") // backup
                    includeSubdomains = true
                }
                onPinningFailure = PinningFailureAction.BLOCK_AND_REPORT
            }
            minimumTlsVersion = TlsVersion.TLS_1_2
        }
        // Audit logging
        auditLog {
            enabled = true
            remoteEndpoint = "https://audit.example.com/events"
            localRetention = 7.days
            signEvents = true
        }
        // Threat response
        threatResponse {
            onCriticalThreat { event ->
                // App-level callback for critical threats
                showSecurityAlert(event)
            }
            onHighThreat { event ->
                reportToServer(event)
            }
        }
    }
)
```

### 4.2 Detection API

```kotlin
// Manual threat check (also runs automatically in background)
val result: DetectionResult = OpenGuard.detection.checkAll()

when (result.highestSeverity) {
    ThreatSeverity.CRITICAL -> /* Terminate session */
    ThreatSeverity.HIGH -> /* Restrict features */
    ThreatSeverity.MEDIUM -> /* Warn user */
    ThreatSeverity.LOW -> /* Log only */
    ThreatSeverity.NONE -> /* All clear */
}

// Individual checks
val rootResult = OpenGuard.detection.checkRootJailbreak()
val debugResult = OpenGuard.detection.checkDebugger()
val hookResult = OpenGuard.detection.checkHooks()
val tamperResult = OpenGuard.detection.checkTamper()
```

### 4.3 Secure Storage API

```kotlin
// Store a secret
OpenGuard.secureStorage.put(
    key = "payment_token",
    value = tokenBytes,
    accessControl = AccessControl.BIOMETRIC_REQUIRED
)

// Retrieve a secret (triggers biometric prompt if required)
val token: ByteArray? = OpenGuard.secureStorage.get("payment_token")

// Delete a secret
OpenGuard.secureStorage.delete("payment_token")

// Securely zero out a buffer
OpenGuard.secureStorage.secureZero(sensitiveBuffer)
```

### 4.4 Crypto API

```kotlin
// Generate a hardware-backed key
val keyHandle = OpenGuard.crypto.generateKey(
    alias = "master_key",
    spec = KeySpec(
        algorithm = KeyAlgorithm.AES,
        keySize = 256,
        hardwareBacked = true,
        requireBiometric = false,
        purposes = setOf(KeyPurpose.ENCRYPT, KeyPurpose.DECRYPT)
    )
)

// Encrypt data
val encrypted = OpenGuard.crypto.encrypt(
    data = plaintext,
    keyAlias = "master_key",
    cipher = CipherSpec.AES_256_GCM
)

// Decrypt data
val decrypted = OpenGuard.crypto.decrypt(
    encryptedData = encrypted,
    keyAlias = "master_key"
)

// Secure hash
val hash = OpenGuard.crypto.hash(
    data = input,
    algorithm = HashAlgorithm.SHA256
)

// TOTP generation
val otp = OpenGuard.crypto.totp.generate(
    secret = encryptedSecret,
    algorithm = TOTPAlgorithm.SHA256
)
```

### 4.5 Network API

```kotlin
// Get a pre-configured HTTP client with pinning applied
// Android (OkHttp)
val okHttpClient = OpenGuard.network.android.configuredOkHttpClient()

// iOS (URLSession)
val urlSession = OpenGuard.network.ios.configuredURLSession()

// Verify certificate manually
val isValid = OpenGuard.network.verifyCertificate(
    host = "api.example.com",
    certificateDerData = certBytes
)
```

### 4.6 Threat Event Model

```kotlin
data class ThreatEvent(
    val id: String,                     // UUID
    val timestamp: Long,                // Unix timestamp ms
    val type: ThreatType,               // ROOT_DETECTED, DEBUGGER_DETECTED, etc.
    val severity: ThreatSeverity,       // CRITICAL, HIGH, MEDIUM, LOW, INFO
    val platform: Platform,             // ANDROID, IOS
    val sdkVersion: String,             // OpenGuard SDK version
    val appVersion: String,             // Host app version
    val deviceId: String,               // Hashed device identifier
    val metadata: Map<String, String>,  // Detection-specific metadata
    val signature: ByteArray            // HMAC-SHA256 signature
)

enum class ThreatType {
    ROOT_DETECTED,
    JAILBREAK_DETECTED,
    DEBUGGER_DETECTED,
    HOOK_DETECTED,
    EMULATOR_DETECTED,
    SIMULATOR_DETECTED,
    TAMPER_DETECTED,
    CERT_PINNING_FAILED,
    REPACKAGING_DETECTED,
    SCREENSHOT_PREVENTED,
    SCREEN_RECORDING_DETECTED,
    OVERLAY_ATTACK_DETECTED,
    SECURE_BOOT_FAILED,
    INTEGRITY_CHECK_FAILED
}
```

---

## 5. Threading Model

```
Main Thread                    Background Thread (OpenGuard)
     │                                    │
     │──── OpenGuard.initialize() ───────►│
     │                                    │── Start periodic checks (30s interval)
     │                                    │── Run initial full check
     │                                    │
     │◄─── ThreatCallback(event) ─────────│ (dispatched to Main)
     │                                    │
     │──── openguard.detection.check() ──►│
     │◄─── DetectionResult ───────────────│ (suspend function, await on coroutine)
```

- All detections run on a **dedicated background thread** (not main thread)
- Threat callbacks are dispatched on the **main thread** by default (configurable)
- All detection APIs are **suspend functions** for use with coroutines
- Periodic checks run every **30 seconds** by default (configurable)
- Initial check runs **synchronously** during `initialize()` to block app start on critical threats

---

## 6. Platform-Specific Implementation Notes

### 6.1 Android

**Root Detection Stack:**
```
Layer 1: File system checks (su binary, Magisk paths) — Fast, low reliability
Layer 2: System property checks (ro.debuggable, ro.secure) — Medium reliability
Layer 3: Package manager checks (Magisk, SuperSU apps) — Medium reliability
Layer 4: Native JNI checks (stat(), access()) — Higher reliability
Layer 5: Play Integrity API (server-side verdict) — Highest reliability
```

**Hook Detection Approach:**
- Scan `/proc/self/maps` for known agent libraries
- Scan thread names for Frida signatures
- TCP port scan for Frida default server port (27042)
- ART method structure inspection for Xposed hooks

**Keystore Integration:**
```kotlin
// Android Keystore — hardware-backed on supported devices
KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    .initialize(
        KeyGenParameterSpec.Builder(alias, PURPOSE_SIGN or PURPOSE_VERIFY)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(DIGEST_SHA256)
            .setIsStrongBoxBacked(true) // StrongBox (dedicated security chip)
            .setUserAuthenticationRequired(true)
            .build()
    )
```

### 6.2 iOS

**Jailbreak Detection Stack:**
```
Layer 1: File existence checks (Cydia, bash, sshd) — Fast, low reliability
Layer 2: URL scheme checks (cydia://) — Low reliability
Layer 3: Sandbox escape attempt — Medium reliability
Layer 4: Dynamic library injection check — Medium-high reliability
Layer 5: Kernel integrity check (sysctl) — High reliability
```

**Hook Detection Approach:**
- `_dyld_image_count()` inspection for unexpected libraries
- Function prologue verification for inline hooks
- Symbol table integrity verification
- Mach-O `__DATA.__got` section integrity check

**Secure Enclave Integration:**
```swift
// iOS Secure Enclave — hardware-backed cryptography
let access = SecAccessControlCreateWithFlags(
    kCFAllocatorDefault,
    kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
    [.biometryCurrentSet, .privateKeyUsage],
    nil
)!
let key = try CryptoKit.SecureEnclave.P256.KeyAgreement.PrivateKey(
    accessControl: access
)
```

### 6.3 KMP Interop Strategy

```kotlin
// commonMain — expect declaration
expect class PlatformRootDetector {
    suspend fun detect(): DetectionResult
}

// androidMain — actual implementation
actual class PlatformRootDetector {
    actual suspend fun detect(): DetectionResult {
        return withContext(Dispatchers.IO) {
            AndroidRootDetector().detect()
        }
    }
}

// iosMain — actual implementation
actual class PlatformRootDetector {
    actual suspend fun detect(): DetectionResult {
        return IosJailbreakDetector().detect()
    }
}
```

---

## 7. Security Hardening of the SDK Itself

The OpenGuard SDK must itself be resistant to tampering and bypass.

### 7.1 Anti-Bypass Measures

| Measure | Implementation |
|---------|----------------|
| Detection redundancy | Multiple independent methods for each check |
| Obfuscated detection strings | Frida port "27042" stored obfuscated |
| Timing jitter | Add random delays to detection to prevent timing attacks |
| Dead code insertion | Insert misleading code paths |
| API availability checks | Verify SDK methods haven't been hooked |
| Checksum verification | SDK verifies its own integrity at runtime |

### 7.2 Bypass Resistance Principles

1. **Defense in depth**: Never rely on a single detection method
2. **Fail closed**: If a check cannot be performed, assume threat present
3. **Asymmetric information**: The attacker must bypass ALL checks; we only need ONE to succeed
4. **Timing randomization**: Vary check intervals to prevent timing-based bypass
5. **Code integrity**: Verify the detector code itself hasn't been modified

---

## 8. Distribution Architecture

### 8.1 Maven Distribution (Android/KMP)

Published to Maven Central:
```
com.openguard:openguard-core:{version}        # KMP shared module
com.openguard:openguard-android:{version}     # Android extras
```

### 8.2 Swift Package Manager (iOS)

```swift
// Package.swift
.package(url: "https://github.com/openguard/openguard-ios.git", from: "1.0.0")
```

### 8.3 CocoaPods (iOS fallback)

```ruby
pod 'OpenGuard', '~> 1.0'
```

### 8.4 Kotlin Multiplatform Integration

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.openguard:openguard-core:1.0.0")
            }
        }
    }
}
```

---

## 9. Versioning & Release Strategy

- **Semantic Versioning**: `MAJOR.MINOR.PATCH`
- **Security patches**: Released within 72 hours of discovery
- **Pin updates**: Documentation and migration guide for pin rotation
- **Deprecation policy**: 6 months notice for breaking API changes
- **LTS releases**: Annual LTS releases with 2-year support window

---

## 10. Testing Strategy

### 10.1 Unit Tests
- Mock platform detectors for common logic testing
- Crypto algorithm correctness tests
- Configuration validation tests

### 10.2 Integration Tests
- Real device detection tests (emulator/real device matrix)
- Jailbreak/root detection on known compromised devices
- Frida bypass attempt tests (automated)

### 10.3 Penetration Test Suite
- Automated pen test mode (`OpenGuard.penTest.*`)
- Test vectors for each detection method
- Bypass attempt simulation

### 10.4 Compliance Tests
- Automated PCI DSS 4.0 checklist verification
- MASVS test vectors
- Certificate pinning bypass attempt tests
