# DexGuard & iXGuard Feature Analysis
## OpenGuard Research Document — For Research Agent Handover

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Purpose:** Comprehensive feature mapping of DexGuard (Android) and iXGuard (iOS) to guide the OpenGuard open-source RASP SDK implementation for Kotlin Multiplatform.

---

## 1. Overview

### 1.1 DexGuard (Android)
DexGuard, developed by GuardSquare, is a commercial security and obfuscation tool for Android applications. It extends ProGuard with advanced security hardening features specifically designed for Android DEX bytecode and native libraries.

**Primary Use Cases:**
- Mobile banking and fintech applications
- Healthcare apps with HIPAA requirements
- Government and defense mobile solutions
- Any application requiring PCI DSS, GDPR, or SOC2 compliance

### 1.2 iXGuard (iOS)
iXGuard, also developed by GuardSquare, is the iOS counterpart to DexGuard. It operates on iOS binaries (Mach-O format), Swift, and Objective-C codebases to provide equivalent protection.

**Primary Use Cases:**
- iOS fintech and banking apps
- Enterprise security applications
- Applications handling sensitive PII
- PCI DSS compliant payment applications

---

## 2. Feature Categories

### 2.1 Code Obfuscation

#### DexGuard (Android)
| Feature | Description | Complexity to Implement |
|---------|-------------|------------------------|
| **Identifier Obfuscation** | Renames classes, methods, fields to meaningless names beyond ProGuard's capability | Medium |
| **String Encryption** | Encrypts string literals in DEX bytecode; decryption happens at runtime via a generated class | High |
| **Class Encryption** | Encrypts entire DEX classes; decrypts on first class load | Very High |
| **Reflection Obfuscation** | Replaces reflective calls with obfuscated equivalents | High |
| **Control Flow Obfuscation** | Inserts dead code, flattens control flow, adds opaque predicates | Very High |
| **Resource Encryption** | Encrypts raw assets, strings.xml, layouts, and other resources | High |
| **Native Library Obfuscation** | Obfuscates `.so` native libraries (LLVM-based) | Very High |
| **Arithmetic Obfuscation** | Replaces simple arithmetic with complex equivalent expressions | High |

#### iXGuard (iOS)
| Feature | Description | Complexity to Implement |
|---------|-------------|------------------------|
| **Symbol Obfuscation** | Obfuscates Objective-C/Swift method names, class names | Medium |
| **String Encryption** | Encrypts string literals in Mach-O binaries | High |
| **Control Flow Obfuscation** | Restructures control flow to impede reverse engineering | Very High |
| **Resource Encryption** | Encrypts bundle resources, plists, images | High |
| **Swift Name Mangling** | Advanced mangling of Swift symbols | High |

> **Research Agent Task:** Investigate LLVM-based obfuscation passes (Hikari, o-llvm) as open-source alternatives. Investigate R8 and ProGuard as bases for Android identifier obfuscation.

---

### 2.2 Runtime Application Self-Protection (RASP)

RASP is the most critical feature for PCI DSS 4.0 compliance. These checks run **inside the application at runtime**.

#### 2.2.1 Root/Jailbreak Detection

##### Android — Root Detection
| Check | Method | Bypass Resistance |
|-------|--------|-------------------|
| **SU Binary Detection** | Checks for `su` binary in common paths | Low (easily hidden) |
| **Build Tag Verification** | Checks `android.os.Build.TAGS` for `test-keys` | Low |
| **Package Manager Check** | Detects Magisk, SuperSU, KingRoot packages | Medium |
| **Dangerous Props Check** | Checks system properties like `ro.debuggable=1` | Medium |
| **RW System Partition** | Verifies `/system` is mounted read-only | Medium |
| **Native SU Check** | Uses JNI to call native `stat()` on su paths | High |
| **Magisk Detection** | Detects Magisk module directory structure, Zygisk | High |
| **Integrity Check via Play Integrity API** | Google Play Integrity API verdict | High (server-side) |
| **Kernel Exploit Detection** | Checks for known kernel vulnerabilities | High |
| **SafetyNet / Attestation** | Hardware-backed attestation | Very High |

##### iOS — Jailbreak Detection
| Check | Method | Bypass Resistance |
|-------|--------|-------------------|
| **Cydia/Sileo App Check** | Checks for Cydia, Sileo, Zebra, installation paths | Low |
| **Suspicious File Existence** | Checks for `/bin/bash`, `/usr/sbin/sshd`, `/etc/apt` | Low |
| **URL Scheme Check** | Attempts to open `cydia://` URL scheme | Low |
| **Sandbox Escape** | Attempts to write outside sandbox | Medium |
| **Dynamic Library Injection** | Checks `_dyld_image_count()` for unexpected libraries | Medium |
| **Fork Check** | Jailbroken devices may allow `fork()` | Medium |
| **Symbolic Link Check** | Checks for symlinks replacing system files | Medium |
| **Substrate/TweakBox Detection** | Detects Cydia Substrate, Electra, unc0ver artifacts | High |
| **Kernel Integrity (KPP/PAC)** | Checks if kernel protections are intact | Very High |

> **Research Agent Task:** Research LibTAL and open-source jailbreak/root detection libraries. Research Rootbeer (Android), DTTJailbreakDetection (iOS). Analyze bypass techniques (Magisk Hide, Liberty Lite) to ensure adequate counter-measures.

#### 2.2.2 Debugger Detection

##### Android
| Check | Method |
|-------|--------|
| `android.os.Debug.isDebuggerConnected()` | Standard API check |
| `/proc/self/status` TracerPid | Non-zero means debugger attached |
| `ptrace(PTRACE_TRACEME)` via JNI | Native self-tracing trick |
| Timing-based detection | `Debug.threadCpuTimeNanos()` delta anomalies |
| Port 23946 check | Default JDWP debugger port |
| `JDWP` thread detection | Scan running threads for JDWP |

##### iOS
| Check | Method |
|-------|--------|
| `sysctl` with `P_TRACED` flag | Checks if process is being traced |
| `ptrace(PT_DENY_ATTACH)` | Prevents debugger from attaching |
| Exception port manipulation | Checks Mach exception ports |
| Timing checks | Detect debugger-induced slowdowns |
| `getppid()` check | Parent process should be `launchd` (PID 1) |

> **Research Agent Task:** Research anti-anti-debugging techniques to understand what attackers do, and design defenses accordingly. Research LLDB and GDB detection specifics for iOS.

#### 2.2.3 Hook/Frida Detection

##### Android (Frida Detection)
| Check | Method |
|-------|--------|
| **Port 27042 scan** | Default Frida server port TCP scan |
| **Named pipe detection** | `/data/local/tmp/frida-*` files |
| **Frida thread detection** | Scan thread names for `gum-js-loop`, `gmain` |
| **Memory map scanning** | Parse `/proc/self/maps` for `frida-agent` |
| **Inline hook detection** | Check function prologues for JMP instructions |
| **Xposed detection** | Detect XposedBridge, EdXposed, LSPosed in classpath |
| **Stack trace analysis** | Inspect stack frames for Xposed hooks |
| **ART hook detection** | Check ART method structures for hook signatures |

##### iOS (Frida/Substrate Detection)
| Check | Method |
|-------|--------|
| **Frida port scan** | TCP scan for port 27042 |
| **`_dyld_get_image_name` inspection** | Scan loaded dynamic libraries |
| **Substrate detection** | Check for `MobileSubstrate.dylib`, `CydiaSubstrate` |
| **FridaGadget detection** | Check for `FridaGadget` in loaded images |
| **`__DATA.__got` integrity** | Verify GOT entries haven't been replaced |
| **Function prologue check** | Verify no inline hooks at known function entry points |
| **Fishhook detection** | Detect rebinding of symbol table entries |

> **Research Agent Task:** Research Frida detection bypass techniques (Frida early instrumentation, custom transports) to ensure detections remain effective. Research how to detect advanced hook frameworks like Dobby.

#### 2.2.4 Emulator/Simulator Detection

##### Android — Emulator Detection
| Check | Method |
|-------|--------|
| Build properties | `ro.product.model`, `ro.hardware`, `ro.kernel.qemu` |
| Sensor availability | Emulators lack real accelerometers/GPS |
| IMEI/Device ID patterns | `000000000000000` patterns |
| Network operator | Emulators use `Android` as operator |
| CPU ABI | `x86`/`x86_64` often indicates emulator |
| Battery properties | Emulator battery properties differ |
| Installed apps | Look for emulator-specific apps |
| OpenGL renderer | Check for software rendering strings |
| `/dev/socket/qemud` existence | QEMU-specific device files |
| BlueStacks/Nox artifacts | Detect specific emulator products |

##### iOS — Simulator Detection
| Check | Method |
|-------|--------|
| `TARGET_OS_SIMULATOR` macro | Compile-time (can be stripped) |
| Architecture check | Simulators run `x86_64`/`arm64` on specific OS builds |
| `UIDevice.current.model` | Returns "iPhone Simulator" on simulators |
| `/System/Library/Extensions` | Simulators lack real kernel extensions |
| Runtime environment check | Detect Xcode simulator runtime |

#### 2.2.5 Tamper Detection

##### Android
| Check | Method |
|-------|--------|
| **APK Signature Verification** | Verify signing certificate against hardcoded hash |
| **APK Hash Verification** | Hash the APK/DEX bytes and compare |
| **Classes.dex Integrity** | Hash `classes.dex` at runtime |
| **Installer Package Check** | Verify app installed from Play Store |
| **Resource Integrity** | Hash resources.arsc |
| **Play Integrity API** | Google server-side app integrity verdict |
| **Native Library Integrity** | Hash `.so` files |

##### iOS
| Check | Method |
|-------|--------|
| **Code Signature Verification** | Use `SecStaticCodeCheckValidity` |
| **Bundle ID Verification** | Compare runtime bundle ID with embedded value |
| **Embedded Provisioning Check** | Verify provisioning profile |
| **MachO Hash Verification** | Hash Mach-O segments at runtime |
| **App Store Receipt Validation** | Validate StoreKit receipt |

#### 2.2.6 Screenshot / Screen Recording Prevention

##### Android
| Method | Implementation |
|--------|----------------|
| `FLAG_SECURE` on Window | Prevents screenshots and appears black in app switcher |
| Detect `MediaProjection` | Monitor for screen capture service |
| Overlay detection | Detect `TYPE_APPLICATION_OVERLAY` windows |

##### iOS
| Method | Implementation |
|--------|----------------|
| `UITextField.isSecureTextEntry` | Automatically blurs in screenshots |
| `UIScreen.isCaptured` | iOS 11+ screen recording detection |
| Secure text fields for sensitive data | Use secure input for all sensitive views |
| Window-level blur on background | Blur app content when backgrounded |

---

### 2.3 Network Security

#### Certificate Pinning
| Platform | Feature |
|----------|---------|
| **Android** | Custom `X509TrustManager`, `OkHttp CertificatePinner`, Network Security Config |
| **iOS** | `URLSession` delegate with `ServerTrustPolicy`, TLS pinning |
| **Both** | SPKI (Subject Public Key Info) pinning — more flexible than cert pinning |
| **Both** | Multi-pin support (primary + backup pins) |
| **Both** | Pin rotation without app update (via remote config) |

#### SSL/TLS Protections
| Feature | Description |
|---------|-------------|
| Minimum TLS version enforcement (TLS 1.2+) | Reject downgrade attempts |
| Cipher suite restriction | Only allow strong cipher suites |
| HSTS enforcement | HTTP Strict Transport Security |
| Certificate transparency verification | Verify CT logs for issued certificates |
| Proxy/MITM detection | Detect installed proxy certificates |

> **Research Agent Task:** Research TrustKit (iOS), OkHttp CertificatePinner (Android), and SPKI pinning implementations. Design pin rotation protocol for fintech apps.

---

### 2.4 Secure Storage

#### Android
| Feature | Implementation |
|---------|----------------|
| **Android Keystore** | Store keys in hardware-backed secure enclave |
| **EncryptedSharedPreferences** | Jetpack Security encrypted preferences |
| **EncryptedFile** | Jetpack Security encrypted file I/O |
| **BiometricKeyguard** | Key access gated by biometric auth |
| **Key attestation** | Verify key resides in hardware |

#### iOS
| Feature | Implementation |
|---------|----------------|
| **Keychain Services** | Store secrets in iOS Keychain |
| **Secure Enclave** | Hardware-backed key generation (CryptoKit) |
| **Data Protection API** | File-level encryption tied to device unlock state |
| **Biometric key access** | `LAContext` + Keychain access control |

---

### 2.5 Anti-Reverse Engineering

#### Android
| Feature | Description |
|---------|-------------|
| Code obfuscation pipeline | R8 + custom transforms |
| Reflection obfuscation | Replace `Class.forName()` with obfuscated equivalents |
| Debug symbol stripping | Strip all native debug symbols |
| Log sanitization | Remove/encrypt all Log calls in release |
| Backup prevention | `android:allowBackup="false"` enforcement |

#### iOS
| Feature | Description |
|---------|-------------|
| Symbol stripping | Strip Swift/ObjC symbol tables |
| Debug info removal | Remove DWARF debug information |
| dSYM management | Securely manage dSYM files off-device |
| Class-dump prevention | Obfuscate ObjC runtime metadata |

---

### 2.6 Threat Response & Reporting

| Feature | Description |
|---------|-------------|
| **Threat callbacks** | App receives callbacks when threats detected |
| **Configurable reactions** | Kill app, show warning, restrict features, log silently |
| **Remote threat reporting** | Send threat events to backend analytics |
| **Threat severity levels** | CRITICAL, HIGH, MEDIUM, LOW, INFO |
| **Audit trail** | Tamper-proof log of security events |
| **Kill switch** | Remote ability to disable compromised app instances |

---

## 3. Feature Priority Matrix for OpenGuard

### Phase 1 — MVP (PCI DSS 4.0 Critical)
| Feature | Priority | PCI DSS Requirement |
|---------|----------|---------------------|
| Root/Jailbreak Detection | P0 | Req 6.3, 6.4 |
| Debugger Detection | P0 | Req 6.3 |
| Certificate Pinning | P0 | Req 6.5, 4.2 |
| Tamper Detection (signature) | P0 | Req 6.3 |
| Secure Storage API | P0 | Req 3.5, 8.6 |
| Screen Capture Prevention | P0 | Req 3.3 |
| Hook Detection (Frida/Xposed) | P1 | Req 6.3 |
| Emulator Detection | P1 | Req 6.3 |
| Threat Reporting API | P1 | Req 10.3 |

### Phase 2 — Enhanced Protection
| Feature | Priority |
|---------|----------|
| Code obfuscation (Gradle plugin) | P2 |
| String encryption | P2 |
| Control flow obfuscation | P2 |
| Resource encryption | P2 |
| SSL/TLS enforcement | P2 |
| Remote configuration | P2 |
| Kill switch | P2 |

### Phase 3 — Advanced (DexGuard/iXGuard Parity)
| Feature | Priority |
|---------|----------|
| Class encryption | P3 |
| Native library obfuscation | P3 |
| Arithmetic obfuscation | P3 |
| Hardware attestation integration | P3 |
| Advanced anti-hooking | P3 |

---

## 4. Technical Research Requirements for Research Agent

### 4.1 Open-Source Libraries to Evaluate

#### Android
| Library | Purpose | License |
|---------|---------|---------|
| [RootBeer](https://github.com/scottyab/rootbeer) | Root detection | Apache 2.0 |
| [SafetyNetSample](https://github.com/googlesamples/android-play-safetynet) | SafetyNet reference | Apache 2.0 |
| [TrustKit Android](https://github.com/datatheorem/TrustKit-Android) | Certificate pinning | MIT |
| [Android Keystore System](https://developer.android.com/training/articles/keystore) | Secure key storage | Built-in |
| [Jetpack Security](https://developer.android.com/jetpack/androidx/releases/security) | EncryptedSharedPreferences | Apache 2.0 |
| [Google Play Integrity](https://developer.android.com/google/play/integrity) | App attestation | Google TOS |
| [Dexprotector (open parts)](https://dexprotector.com) | Reference implementation | Commercial |

#### iOS
| Library | Purpose | License |
|---------|---------|---------|
| [DTTJailbreakDetection](https://github.com/thii/DTTJailbreakDetection) | Jailbreak detection | MIT |
| [TrustKit](https://github.com/datatheorem/TrustKit) | Certificate pinning | MIT |
| [IOSSecuritySuite](https://github.com/securing/IOSSecuritySuite) | Comprehensive iOS security | MIT |
| [Swift CryptoKit](https://developer.apple.com/documentation/cryptokit) | Cryptographic operations | Built-in |
| [LocalAuthentication](https://developer.apple.com/documentation/localauthentication) | Biometric auth | Built-in |

### 4.2 Academic & Security Research to Review
- "Frida: Dynamic Instrumentation Toolkit" — understand detection vectors
- "Android Security: Attacks and Defenses" — Anmol Misra & Abhishek Dubey
- "The Mobile Application Hacker's Handbook" — Dominic Chell et al.
- OWASP Mobile Security Testing Guide (MSTG)
- OWASP Mobile Application Verification Standard (MASVS) Level 2
- GuardSquare whitepapers on obfuscation techniques
- "ShadowGuard" research papers on control flow obfuscation

### 4.3 Standards & Compliance to Map
- **PCI DSS v4.0** — Full mobile requirements mapping (see separate doc)
- **OWASP MASVS 2.0** — Mobile App Verification Standard
- **NIST SP 800-163** — Vetting Mobile Apps for Enterprise Use
- **FIPS 140-3** — Cryptographic module requirements
- **EMVCo Mobile Payment** — EMV contactless payment security

### 4.4 Specific Research Questions for Agent

1. **Frida Detection Evasion**: Research the latest Frida techniques (early instrumentation, custom transports on non-standard ports) and design detections that work against them.

2. **Magisk Module Detection**: As Magisk Hide/DenyList evolves, what are reliable methods to detect root that survive MagiskHide?

3. **ART Hook Detection**: How do Xposed/EdXposed/LSPosed modify ART method structures? What reliable runtime checks exist?

4. **iOS Kernel Integrity**: With KPP and KTRR (Kernel Text Readonly Region) on modern iPhones, what jailbreak detection methods remain viable?

5. **Play Integrity API vs SafetyNet**: SafetyNet is deprecated — research Play Integrity API migration, server-side verdict validation, and nonce handling.

6. **SPKI Pinning vs Certificate Pinning**: Document the trade-offs and implement the more resilient SPKI approach.

7. **KMP Native Interop**: Research best practices for calling platform-native security APIs from Kotlin Multiplatform (expect/actual mechanism vs. direct native interop).

8. **Obfuscation Build Pipeline**: Research Kotlin compiler plugins as a mechanism for implementing string encryption and class transformation in the build pipeline.

9. **iOS App Attestation**: Research Apple's DeviceCheck and App Attest APIs as alternatives to hardware attestation.

10. **Remote Configuration Security**: How can a kill-switch / remote config be implemented securely without introducing a new attack surface?

---

## 5. Competitive Analysis

### 5.1 DexGuard Pricing (as of 2025)
- Enterprise license: ~$10,000–$50,000/year depending on tier
- Per-application licensing available
- Makes it impractical for smaller fintech teams or startups

### 5.2 iXGuard Pricing
- Similar pricing tier to DexGuard
- Combined DexGuard + iXGuard bundle available

### 5.3 OpenGuard Value Proposition
- **Free and open-source** (Apache 2.0)
- **Kotlin Multiplatform native** — single shared codebase for Android + iOS
- **PCI DSS 4.0 aligned** — compliance-first design
- **Community maintained** — transparent security through openness
- **Extensible** — plugin architecture for custom checks
- **No vendor lock-in** — migrate freely

### 5.4 Commercial Alternatives to Evaluate
| Product | Platform | Pricing |
|---------|---------|---------|
| DexGuard | Android | ~$10K+/year |
| iXGuard | iOS | ~$10K+/year |
| Guardsquare Mobile App Shielding | Both | Custom |
| Arxan (now Digital.ai) | Both | Enterprise |
| Promon SHIELD | Both | Enterprise |
| Verimatrix | Both | Enterprise |
| AppSealing | Both | SaaS, per-MAU |
| Appdome | Both | SaaS |

---

## 6. Glossary

| Term | Definition |
|------|-----------|
| **RASP** | Runtime Application Self-Protection — security checks running inside the app at runtime |
| **DEX** | Dalvik Executable — Android bytecode format |
| **Mach-O** | Mach Object — iOS/macOS binary format |
| **SPKI** | Subject Public Key Info — public key pinning method more resilient than certificate pinning |
| **KPP** | Kernel Patch Protection — Apple's protection against kernel modifications |
| **ART** | Android Runtime — Android's managed runtime (replaced Dalvik) |
| **Frida** | Popular dynamic instrumentation framework used for app analysis and reverse engineering |
| **Substrate** | Cydia Substrate (MobileSubstrate) — iOS hooking framework |
| **Xposed** | Android hooking framework that modifies ART method structures |
| **JNI** | Java Native Interface — allows Java/Kotlin to call C/C++ code |
| **KMP** | Kotlin Multiplatform — framework for sharing Kotlin code across platforms |
| **CMP** | Compose Multiplatform — Kotlin multiplatform UI framework |
