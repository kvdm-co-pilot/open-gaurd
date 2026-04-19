# OpenGuard — Research Agent Handover Document

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Status:** Ready for Research Agent  
**Classification:** Internal / Research

---

## 1. Project Context

**Project:** OpenGuard — Open-source RASP SDK for Kotlin Multiplatform  
**Goal:** Replace DexGuard (Android) and iXGuard (iOS) with a free, open-source alternative  
**Tech Stack:** Kotlin Multiplatform (KMP), Compose Multiplatform, Android, iOS  
**Compliance Target:** PCI DSS 4.0, OWASP MASVS 2.0 Level 2  
**Primary Use Case:** Fintech/payment applications

**Why we're building this:**
- DexGuard + iXGuard combined licensing costs are prohibitive (~$20K-$100K+/year)
- No KMP-native open-source alternative exists
- Want full control over security implementation
- Community-maintained transparency

---

## 2. Documents to Review First

Before starting research tasks, review these foundational documents:

| Document | Location | Purpose |
|----------|----------|---------|
| DexGuard/iXGuard Feature Analysis | `docs/research/dexguard-ixguard-feature-analysis.md` | Comprehensive feature mapping |
| PCI DSS 4.0 Requirements | `docs/research/pci-dss-4.0-mobile-requirements.md` | Compliance requirements |
| SDK Architecture | `docs/architecture/sdk-architecture.md` | Technical design |
| Distribution Strategy | `docs/distribution/distribution-strategy.md` | How SDK is published/consumed |

---

## 3. Research Tasks (Priority Ordered)

### TASK-001: Root Detection Bypass Resistance Research [Priority: P0]

**Objective:** Identify the most reliable root detection methods that survive Magisk Hide/DenyList and other modern bypass techniques.

**Deliverable:** Technical report with:
1. Top 5 root detection methods ranked by bypass resistance (2025/2026)
2. Corresponding bypass techniques for each method
3. Recommended implementation approach for OpenGuard
4. Code samples in Kotlin/JNI for recommended approaches

**Research Questions:**
- Does Magisk DenyList completely hide root from apps? What leaks remain?
- Is Play Integrity API the only reliable server-side check now that SafetyNet is deprecated?
- How do banking apps (Revolut, N26, Monzo) detect root in 2025/2026?
- What is the current state of `ro.boot.verifiedbootstate` checks?
- How does StrongBox attestation chain verification work?

**Starting Resources:**
- [Magisk GitHub](https://github.com/topjohnwu/Magisk)
- [Play Integrity API docs](https://developer.android.com/google/play/integrity)
- [RootBeer source](https://github.com/scottyab/rootbeer)
- OWASP MSTG: Android Testing Guide (Anti-Reversing section)

---

### TASK-002: iOS Jailbreak Detection Research [Priority: P0]

**Objective:** Identify reliable jailbreak detection methods for modern iOS (iOS 15-18) with BootROM exploits (palera1n, checkm8) and newer rootless jailbreaks.

**Deliverable:** Technical report with:
1. Jailbreak detection methods per jailbreak type (tethered, semi-tethered, rootless)
2. Current state of rootless jailbreaks and what they change
3. Recommended multi-layered detection approach
4. Swift + Kotlin Native code samples

**Research Questions:**
- What is the security posture of rootless jailbreaks (palera1n) — what checks survive?
- How does `SecStaticCodeCheckValidity` work and can it be bypassed?
- What does Apple's App Attest API provide and how does it verify device integrity?
- How do major banking apps detect jailbreak on iOS 17/18?
- Can `_dyld_shared_cache_contains_path` be used for detection?

**Starting Resources:**
- [IOSSecuritySuite GitHub](https://github.com/securing/IOSSecuritySuite)
- [Apple App Attest documentation](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity)
- [palera1n technical analysis](https://github.com/palera1n/palera1n)
- Jailbreak detection bypass research papers

---

### TASK-003: Frida Detection Research [Priority: P0]

**Objective:** Research state-of-the-art Frida detection and evasion techniques for both Android and iOS.

**Deliverable:** Technical report with:
1. All known Frida injection methods (gadget, server, early instrumentation)
2. Detection method for each injection technique
3. Known detection bypasses and counter-measures
4. Code implementation guide

**Research Questions:**
- How does Frida early instrumentation work and how can it be detected?
- What happens when Frida uses a non-standard port — how do we detect it?
- Can Frida operate over USB without exposing a network port? How to detect?
- How does `frida-gadget` embedded mode evade detection?
- What are reliable cross-platform (Android+iOS) Frida detection signatures?

**Starting Resources:**
- [Frida documentation](https://frida.re/docs/)
- [Anti-Frida techniques research](https://github.com/b-mueller/frida-detection-demo)
- OWASP MSTG Testing section on dynamic analysis

---

### TASK-004: Certificate Pinning Best Practices [Priority: P0]

**Objective:** Design the optimal certificate pinning implementation for a fintech KMP app.

**Deliverable:** Technical design document with:
1. SPKI pinning vs certificate pinning vs public key pinning comparison
2. Pin rotation strategy without app update
3. Backup pin recommendation (min 2 pins)
4. Integration with OkHttp (Android) and URLSession (iOS)
5. How to handle pin failures (UX + security balance)

**Research Questions:**
- What is the recommended approach for pin rotation in a live payment app?
- Can HPKP (HTTP Public Key Pinning) headers be used as a supplement?
- How does TrustKit handle pin rotation via a remote configuration?
- What are the risks of pinning too aggressively (outage risk)?
- How should Let's Encrypt certificate rotation be handled in pinning?

**Starting Resources:**
- [TrustKit Android](https://github.com/datatheorem/TrustKit-Android)
- [TrustKit iOS](https://github.com/datatheorem/TrustKit)
- [OWASP Certificate Pinning Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Pinning_Cheat_Sheet.html)
- [Google SPKI documentation](https://www.chromium.org/Home/chromium-security/certtransparency/)

---

### TASK-005: KMP Security Interop Research [Priority: P1]

**Objective:** Research best patterns for exposing platform-native security APIs through Kotlin Multiplatform.

**Deliverable:** Technical design document with:
1. `expect`/`actual` pattern for security-critical code
2. JNI/C++ interop for Android native security checks
3. Kotlin/Native interop with iOS Security framework APIs
4. Thread safety considerations for RASP checks in KMP
5. Memory management for sensitive data in KMP (CharArray vs String)

**Research Questions:**
- What are the performance implications of crossing the KMP bridge frequently?
- How can we call `SecItemAdd`/`SecItemCopyMatching` from Kotlin/Native?
- How can we call Android Keystore APIs optimally from KMP?
- What is the recommended way to handle `ByteArray` zero-fill in KMP for both platforms?
- Can we use Kotlin Coroutines for async security checks in KMP reliably?

**Starting Resources:**
- [KMP documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Kotlin/Native interop with Swift/Obj-C](https://kotlinlang.org/docs/native-objc-interop.html)
- [KMP security samples](https://github.com/JetBrains/kotlin-multiplatform-samples)

---

### TASK-006: Code Obfuscation Pipeline Research [Priority: P2]

**Objective:** Research open-source approaches to implement string encryption and class obfuscation as a Kotlin compiler plugin.

**Deliverable:** Technical feasibility report with:
1. Kotlin compiler plugin API for bytecode transformation
2. R8 custom rules for enhanced obfuscation
3. Open-source obfuscation tools evaluation (Hikari, o-llvm for native)
4. String encryption implementation approach

**Research Questions:**
- Can a Kotlin compiler plugin intercept string literals and encrypt them?
- What is the state of LLVM-based obfuscation (Hikari) for Android NDK builds?
- How does DexGuard's string encryption work at the DEX level?
- Can R8's custom extensions be used for semantic-preserving transformations?
- What is the performance overhead of runtime string decryption?

**Starting Resources:**
- [Kotlin compiler plugin docs](https://kotlinlang.org/docs/kotlin-compiler-plugins.html)
- [Hikari LLVM obfuscator](https://github.com/HikariObfuscator/Hikari)
- [Android R8 documentation](https://developer.android.com/studio/build/shrink-code)

---

### TASK-007: Apple App Attest & Android Play Integrity [Priority: P1]

**Objective:** Design a unified server-side attestation flow using Apple's App Attest and Google's Play Integrity API.

**Deliverable:** System design document with:
1. App Attest flow (client + server)
2. Play Integrity API flow (client + server)
3. Unified OpenGuard attestation API design
4. Server-side validation implementation guide
5. Handling attestation in offline scenarios

**Research Questions:**
- How does Apple's App Attest assertion vs attestation work?
- What does Play Integrity API's `MEETS_STRONG_INTEGRITY` verdict mean exactly?
- How often should attestation be requested (per session? per transaction?)?
- How do we bind the attestation result to a specific transaction (nonce)?
- Can attestation be cached? For how long?

**Starting Resources:**
- [Apple App Attest](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity)
- [Google Play Integrity](https://developer.android.com/google/play/integrity/overview)
- [FIDO Device Attestation](https://fidoalliance.org/specs/fido-v2.0-rd-20180702/fido-server-rfc8555-v2.0-rd-20180702.html)

---

### TASK-008: Secure Storage Research [Priority: P1]

**Objective:** Design the optimal secure storage API using Android Keystore and iOS Secure Enclave.

**Deliverable:** Technical design with:
1. Android Keystore key hierarchy design
2. iOS Keychain + Secure Enclave integration design  
3. Biometric authentication integration (BiometricPrompt / LAContext)
4. Key rotation strategy
5. Migration strategy (old keys → new keys)
6. Cross-device scenarios (new device, backup/restore)

**Research Questions:**
- What is StrongBox and how do we verify a key is StrongBox-backed?
- How does iOS Secure Enclave key generation work with `kSecAttrTokenIDSecureEnclave`?
- What are the constraints of Secure Enclave keys (no export, limited algorithms)?
- How should we handle biometric enrollment change (new fingerprint added)?
- What happens to Keystore keys on Android factory reset vs device migration?

---

### TASK-009: Overlay Attack & Accessibility Service Detection [Priority: P1]

**Objective:** Research detection of overlay attacks and malicious accessibility services on Android.

**Deliverable:** Technical report with:
1. Types of overlay attacks (TYPE_APPLICATION_OVERLAY, etc.)
2. Malicious accessibility service detection methods
3. Keylogger detection (custom InputMethodService)
4. Android 12+ restricted permission changes affecting detection
5. Recommended mitigations

**Research Questions:**
- How does `Settings.canDrawOverlays()` relate to payment screen protection?
- Can we detect if a malicious app is reading our screen via accessibility?
- What are the limitations of `FLAG_SECURE` against overlay attacks?
- How do banks like Banco Inter detect overlay attacks?

---

### TASK-010: PCI DSS 4.0 Mobile Pen Test Guide [Priority: P2]

**Objective:** Create a comprehensive pen testing guide for OpenGuard-integrated apps.

**Deliverable:** Pen test guide document with:
1. Complete test cases for each RASP detection
2. Tools list (Frida, objection, apktool, jadx, Hopper, r2)
3. Expected outcomes (pass/fail criteria)
4. Bypass attempt documentation
5. Remediation recommendations

**Research Questions:**
- What is the current OWASP MSTG test case list for MASVS-RESILIENCE?
- What automated tools exist for mobile app pen testing (MobSF, Drozer, objection)?
- How should pen test results be documented for PCI DSS QSA review?
- What is the minimum pen test frequency required by PCI DSS 4.0 (Req 11.4)?

---

## 4. Architecture Decisions Needed from Research

Based on research outcomes, the following architecture decisions must be made:

| Decision | Options | Research Needed |
|----------|---------|----------------|
| AD-001: Primary root detection method | Play Integrity + file checks vs. hardware attestation only | TASK-001 |
| AD-002: Certificate pinning approach | SPKI vs cert hash vs TrustKit | TASK-004 |
| AD-003: Frida detection primary method | Port scan vs memory map vs thread scan | TASK-003 |
| AD-004: Obfuscation pipeline | Compiler plugin vs Gradle transform vs R8 | TASK-006 |
| AD-005: Attestation frequency | Per-session vs per-transaction vs periodic | TASK-007 |
| AD-006: Secure Enclave key types | P-256 ECDH vs RSA vs AES-256 | TASK-008 |
| AD-007: Threat event transport | HTTPS batch vs real-time vs local-only | New research |

---

## 5. Open Questions (Non-Blocking)

These can be addressed after the initial SDK implementation:

1. **License Selection:** Apache 2.0 is proposed — confirm no license conflicts with incorporated libraries
2. **Community Governance:** How will CVE disclosures be handled? Private disclosure period?
3. **Certification:** Should we pursue FIPS 140-3 certification for the crypto module?
4. **Commercial Support:** Will there be a commercial support tier?
5. **Obfuscation Legality:** Confirm compliance implications of open-source obfuscation tools in different jurisdictions

---

## 6. Glossary

| Term | Definition |
|------|-----------|
| **RASP** | Runtime Application Self-Protection |
| **MASVS** | OWASP Mobile Application Verification Standard |
| **MSTG** | OWASP Mobile Security Testing Guide |
| **QSA** | Qualified Security Assessor (PCI DSS auditor) |
| **SPKI** | Subject Public Key Info (public key pinning) |
| **KMP** | Kotlin Multiplatform |
| **CMP** | Compose Multiplatform |
| **ART** | Android Runtime |
| **JNI** | Java Native Interface |
| **KPP** | Kernel Patch Protection (iOS) |
| **StrongBox** | Dedicated security chip in high-end Android devices |
| **Frida** | Dynamic instrumentation framework used by security researchers and attackers |
| **Magisk** | Android root solution with hide capabilities |
| **palera1n** | Semi-tethered rootless jailbreak for iOS |
| **Cydia Substrate** | iOS hooking framework (MobileSubstrate) |
| **objection** | Runtime mobile exploration powered by Frida |

---

## 7. Handover Checklist

Before handing over to research agent, confirm:
- [x] Feature analysis document complete
- [x] PCI DSS 4.0 requirements mapped
- [x] SDK architecture designed
- [x] Distribution strategy documented
- [x] Research tasks defined and prioritized
- [x] Architecture decisions identified
- [x] Project skeleton created (Kotlin files, build files)
- [ ] Research agent assigned and briefed
- [ ] Research timeline established (suggested: 2-3 weeks)
- [ ] Research output format agreed upon (markdown documents)
- [ ] Stakeholder sign-off on Phase 1 feature scope
