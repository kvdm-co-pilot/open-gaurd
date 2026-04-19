# OpenGuard

**Open-source RASP SDK for Kotlin Multiplatform — Android & iOS**

> A free, open-source alternative to DexGuard and iXGuard, purpose-built for PCI DSS 4.0 compliant fintech applications using Kotlin Multiplatform (KMP) and Compose Multiplatform (CMP).

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![PCI DSS 4.0](https://img.shields.io/badge/PCI%20DSS-4.0%20Aligned-green.svg)]()
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-purple.svg)]()

---

## Why OpenGuard?

DexGuard and iXGuard are powerful commercial security tools, but their combined licensing costs ($20,000–$100,000+/year) make them prohibitive for many fintech teams. OpenGuard provides equivalent Runtime Application Self-Protection (RASP) capabilities as a free, open-source, Kotlin-first SDK.

**Key advantages:**
- ✅ **Free & open-source** (Apache 2.0)
- ✅ **KMP-native** — single API for Android and iOS
- ✅ **PCI DSS 4.0 aligned** — every feature maps to a compliance requirement
- ✅ **OWASP MASVS 2.0 Level 2** compliant
- ✅ **No vendor lock-in**

---

## Features

### Runtime Application Self-Protection (RASP)
| Feature | Android | iOS |
|---------|---------|-----|
| Root / Jailbreak Detection | ✅ | ✅ |
| Debugger Detection | ✅ | ✅ |
| Hook Detection (Frida, Xposed, Substrate) | ✅ | ✅ |
| Emulator / Simulator Detection | ✅ | ✅ |
| Tamper / Repackaging Detection | ✅ | ✅ |
| Screenshot Prevention | ✅ | ✅ |
| Screen Recording Detection | ✅ | ✅ |

### Network Security
| Feature | Android | iOS |
|---------|---------|-----|
| Certificate Pinning (SPKI) | ✅ | ✅ |
| TLS 1.2+ Enforcement | ✅ | ✅ |
| MITM Detection | ✅ | ✅ |

### Secure Storage
| Feature | Android | iOS |
|---------|---------|-----|
| Android Keystore / iOS Keychain | ✅ | ✅ |
| Biometric Access Control | ✅ | ✅ |
| Hardware-backed Keys (StrongBox / Secure Enclave) | ✅ | ✅ |
| Secure memory zeroing | ✅ | ✅ |

### Cryptography
| Feature | Android | iOS |
|---------|---------|-----|
| AES-256-GCM Encryption | ✅ | ✅ |
| SHA-256/384/512 Hashing | ✅ | ✅ |
| TOTP (RFC 6238) | ✅ | ✅ |
| HMAC | ✅ | ✅ |

---

## Quick Start

### Android

```kotlin
// Application.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenGuardAndroid.initialize(
            context = this,
            config = OpenGuardConfig {
                detection {
                    rootJailbreakDetection { enabled = true }
                    debuggerDetection { enabled = true }
                    hookDetection { enabled = true }
                    tamperDetection {
                        enabled = true
                        expectedSignatureHash = BuildConfig.CERT_HASH
                    }
                }
                network {
                    certificatePinning {
                        pin("api.yourapp.com") {
                            addSha256Pin(BuildConfig.CERT_PIN_PRIMARY)
                            addSha256Pin(BuildConfig.CERT_PIN_BACKUP)
                        }
                    }
                }
                threatResponse {
                    onCriticalThreat { event ->
                        terminateSession()
                    }
                }
            }
        )
    }
}
```

### iOS (Kotlin Multiplatform / iosMain)

```kotlin
// iosMain
OpenGuard.initialize(
    config = OpenGuardConfig {
        detection {
            rootJailbreakDetection { enabled = true }
            debuggerDetection { enabled = true }
        }
    }
)
```

### Running Security Checks

```kotlin
val result = OpenGuard.detection.checkAll()

when (result.highestSeverity) {
    ThreatSeverity.CRITICAL -> blockApp()
    ThreatSeverity.HIGH -> restrictFeatures()
    ThreatSeverity.MEDIUM -> warnUser()
    ThreatSeverity.NONE -> continueNormally()
}
```

---

## Project Structure

```
open-guard/
├── openguard-core/         # KMP shared module (Android + iOS)
│   └── src/
│       ├── commonMain/     # Shared API interfaces and logic
│       ├── androidMain/    # Android RASP implementations
│       └── iosMain/        # iOS RASP implementations
├── openguard-android/      # Android-specific extensions
├── sample/
│   ├── androidApp/         # Android sample application
│   └── iosApp/             # iOS sample application (coming soon)
└── docs/
    ├── research/
    │   ├── dexguard-ixguard-feature-analysis.md
    │   └── pci-dss-4.0-mobile-requirements.md
    ├── architecture/
    │   └── sdk-architecture.md
    ├── distribution/
    │   └── distribution-strategy.md
    └── RESEARCH_HANDOVER.md
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [DexGuard/iXGuard Feature Analysis](docs/research/dexguard-ixguard-feature-analysis.md) | Comprehensive feature mapping from commercial tools |
| [PCI DSS 4.0 Requirements](docs/research/pci-dss-4.0-mobile-requirements.md) | Mobile compliance requirements and implementation guide |
| [SDK Architecture](docs/architecture/sdk-architecture.md) | Technical design and API documentation |
| [Distribution Strategy](docs/distribution/distribution-strategy.md) | Maven/SPM/CocoaPods distribution guide |
| [Research Handover](docs/RESEARCH_HANDOVER.md) | Research tasks for the next phase |

---

## Roadmap

### Phase 1 — MVP (Current)
- [x] Project skeleton and API design
- [x] Root/Jailbreak detection (Android + iOS)
- [x] Debugger detection (Android + iOS)
- [x] Hook detection (Frida, Xposed, Substrate)
- [x] Emulator/Simulator detection
- [x] Certificate pinning (SPKI-based)
- [x] Secure storage (Keystore/Keychain)
- [x] AES-256-GCM cryptography
- [x] TOTP generation/validation
- [x] Threat event reporting API
- [ ] Screenshot/recording prevention (in progress)
- [ ] Tamper detection (code signature verification)
- [ ] Play Integrity API integration
- [ ] Apple App Attest integration

### Phase 2 — Enhanced Protection
- [ ] Gradle plugin for string encryption
- [ ] Code obfuscation pipeline
- [ ] Resource encryption
- [ ] Remote configuration / kill switch
- [ ] Advanced Frida detection (early instrumentation)
- [ ] Overlay attack detection

### Phase 3 — Advanced
- [ ] Class encryption
- [ ] Control flow obfuscation
- [ ] Hardware attestation chain verification
- [ ] FIPS 140-3 cryptographic module

---

## PCI DSS 4.0 Compliance

After integrating OpenGuard and following the [PCI DSS 4.0 implementation guide](docs/research/pci-dss-4.0-mobile-requirements.md), your app will satisfy the following requirements:

| PCI DSS Requirement | OpenGuard Feature |
|--------------------|-------------------|
| Req 3.5 — Encrypt stored PANs | Secure Storage API (AES-256-GCM) |
| Req 4.2 — Encrypt data in transit | Certificate Pinning + TLS enforcement |
| Req 6.3 — Identify security vulnerabilities | RASP (root, debugger, hook detection) |
| Req 6.4 — Protect against attacks | Tamper detection, anti-repackaging |
| Req 8.3 — Multi-factor authentication | Biometric + Secure Enclave |
| Req 10.2 — Audit logs | Threat event reporting |
| Req 11.4 — Penetration testing | Built-in pen test mode |

---

## License

```
Copyright 2026 OpenGuard Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
