# PCI DSS 4.0 Mobile Application Requirements
## OpenGuard Compliance Mapping Document

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Standard:** PCI DSS v4.0 (March 2022, effective March 2025 for new requirements)  
**Scope:** Mobile payment applications (Android & iOS) using Kotlin Multiplatform

---

## 1. Executive Summary

PCI DSS v4.0 introduces significant changes that directly impact mobile payment applications. This document maps PCI DSS 4.0 requirements to specific OpenGuard SDK features and implementation guidance for fintech teams building PCI DSS compliant mobile applications.

**Key Changes in PCI DSS 4.0 vs 3.2.1 relevant to mobile:**
- Increased focus on targeted risk analysis
- New requirements for authentication (multi-factor, software tokens)
- Stronger cryptography requirements
- Explicit requirements for mobile payment software
- New "Customized Approach" option alongside the prescriptive approach

---

## 2. PCI DSS 4.0 Requirements Applicable to Mobile Apps

### 2.1 Requirement 1 — Network Security Controls

#### Req 1.3: Network access controls between trusted and untrusted networks
**Mobile Implication:** Mobile apps communicate over untrusted networks (cellular, public Wi-Fi).

| OpenGuard Feature | Implementation |
|-------------------|----------------|
| Certificate Pinning | Prevents MITM attacks on public networks |
| TLS 1.2+ Enforcement | Blocks protocol downgrade attacks |
| Proxy/MITM Detection | Detects installed proxy certificates |
| Network monitoring | Alert on unexpected connections |

**Compliance Evidence Required:**
- [ ] Certificate pinning enabled for all payment API endpoints
- [ ] TLS configuration documented and enforced
- [ ] Network security configuration reviewed annually

---

### 2.2 Requirement 2 — Apply Secure Configurations

#### Req 2.2: System components configured and managed securely

| OpenGuard Feature | Implementation |
|-------------------|----------------|
| Debuggable flag check | Verify `android:debuggable=false` in release |
| Backup prevention | Verify `android:allowBackup=false` |
| Exported component check | Warn on over-exposed activities/services |
| iOS ATS enforcement | App Transport Security always enabled |
| Minimum OS version | Enforce minimum Android API / iOS version |

**Compliance Evidence Required:**
- [ ] App manifest security configuration reviewed
- [ ] No debug builds deployed to production
- [ ] Secure configuration baseline documented

---

### 2.3 Requirement 3 — Protect Stored Account Data

This is one of the most critical requirements for mobile payment apps.

#### Req 3.1: Processes and mechanisms for protecting stored account data documented
#### Req 3.2: Storage of account data minimized

| Rule | OpenGuard Feature |
|------|------------------|
| Do NOT store full PAN | SDK guidance + lint warning on SharedPreferences usage |
| Do NOT store CVV/CVC | Enforced by design — SDK never accepts CVV for storage |
| Do NOT store track data | SDK guidance documentation |
| Truncated PAN only (if stored) | Utility functions for PAN truncation |

#### Req 3.3: Sensitive authentication data (SAD) not retained after authorization

**OpenGuard Implementation:**
```kotlin
// OpenGuard enforces clearing of sensitive buffers
OpenGuard.secureStorage.clearSensitiveData()
// Provides zeroing utilities for ByteArray/CharArray
secureZero(panBuffer)
```

#### Req 3.4: PAN rendered unreadable anywhere it is stored

| OpenGuard Feature | Standard |
|-------------------|---------|
| AES-256 encryption | FIPS 140-2/3 approved |
| Android Keystore integration | Hardware-backed key storage |
| iOS Secure Enclave integration | Hardware-backed key storage |
| Key rotation support | Annual or on-compromise rotation |

#### Req 3.5: Primary account numbers secured with strong cryptography

**Cryptography Requirements:**
- AES-256 for symmetric encryption
- RSA-2048+ or ECC P-256+ for asymmetric
- SHA-256+ for hashing (never MD5/SHA-1 for security)
- PBKDF2, bcrypt, or Argon2 for password hashing

#### Req 3.7: Cryptographic keys protected

| OpenGuard Feature | Implementation |
|-------------------|----------------|
| Hardware key storage | Android Keystore / iOS Secure Enclave |
| Key access control | Biometric or device credential requirement |
| Key attestation | Verify hardware storage via attestation |
| Key rotation API | Automated key rotation support |
| No plaintext key export | Keys never leave secure enclave |

---

### 2.4 Requirement 4 — Protect Cardholder Data with Strong Cryptography During Transmission

#### Req 4.2: PAN protected with strong cryptography during transmission

| Requirement | OpenGuard Feature |
|-------------|------------------|
| TLS 1.2 minimum | TLS version enforcement |
| Strong cipher suites only | Cipher suite allowlist |
| Certificate validity | Certificate expiry monitoring |
| Certificate pinning | SPKI/Certificate pinning |
| No transmission over unencrypted channels | Network security policy enforcement |

---

### 2.5 Requirement 5 — Protect All Systems Against Malware

#### Req 5.2: Malware is detected and protected against
#### Req 5.4: Phishing attacks are protected against

**Mobile Context:** Anti-malware for mobile focuses on detecting compromised environments.

| OpenGuard Feature | Addresses |
|-------------------|-----------|
| Root/Jailbreak Detection | Detects compromised OS environment |
| Hook Detection (Frida/Xposed) | Detects malware injection frameworks |
| Overlay Attack Detection | Detects screen overlay phishing |
| Fake keyboard detection | Detects keylogger keyboards |
| Accessibility service detection | Detects malicious accessibility services |
| App clone detection | Detects repackaged malicious clones |

---

### 2.6 Requirement 6 — Develop and Maintain Secure Systems and Software

This is the **most directly relevant** requirement for mobile app security.

#### Req 6.2: Bespoke and custom software developed securely

| Requirement | OpenGuard Implementation |
|-------------|--------------------------|
| Security training | Developer integration guide |
| Security requirements defined | This document + SDK API design |
| Secure design review | Architecture decision records |
| Code review | Automated security linting |
| Testing for vulnerabilities | Security test suite |

#### Req 6.3: Security vulnerabilities are identified and addressed

| Requirement | OpenGuard Implementation |
|-------------|--------------------------|
| Vulnerability identification | Automated dependency scanning |
| Severity ranking (CVSS) | Critical/High/Medium/Low classification |
| Patch within 1 month (critical) | SDK release process commitment |
| Penetration testing | Pen test guide included |

#### Req 6.3.3: All software components are protected from known vulnerabilities

**OpenGuard Approach:**
- Dependency vulnerability scanning in CI/CD
- Regular SDK updates with security patches
- CVE tracking for all dependencies
- SBOM (Software Bill of Materials) generation

#### Req 6.4: Public-facing web applications are protected against attacks

**Mobile equivalent — App Store distributed apps:**

| OWASP Mobile Top 10 | OpenGuard Defense |
|---------------------|-------------------|
| M1: Improper Platform Usage | API misuse detection, secure defaults |
| M2: Insecure Data Storage | Secure Storage API |
| M3: Insecure Communication | Certificate Pinning + TLS enforcement |
| M4: Insecure Authentication | Biometric + Secure Session management |
| M5: Insufficient Cryptography | Cryptography API with strong defaults |
| M6: Insecure Authorization | Access control guidance |
| M7: Client Code Quality | Linting + static analysis |
| M8: Code Tampering | Tamper detection |
| M9: Reverse Engineering | Anti-analysis features |
| M10: Extraneous Functionality | Debug mode detection |

#### Req 6.4.3: Payment page scripts managed

**Mobile equivalent:** All third-party SDKs integrated into the payment app must be:
- [ ] Inventoried and documented
- [ ] Reviewed for security
- [ ] Authorized explicitly
- [ ] Integrity verified (hash/signature check)

**OpenGuard Feature:**
```kotlin
// Third-party SDK integrity verification
OpenGuard.sdkIntegrity.registerTrustedSdk(
    packageName = "com.example.payment",
    expectedSignatureHash = "sha256/..."
)
```

#### Req 6.5: Changes to all system components managed securely

| Requirement | OpenGuard Implementation |
|-------------|--------------------------|
| Change management process | Release checklist template |
| Testing before production | Pre-release security scan |
| Security review | Code review requirement |
| Rollback capability | Version management guidance |

---

### 2.7 Requirement 7 — Restrict Access to System Components

#### Req 7.3: Access to system components and cardholder data is managed via access control system

**Mobile Context:** App-level access control

| OpenGuard Feature | Implementation |
|-------------------|----------------|
| Biometric authentication | LAContext (iOS) / BiometricPrompt (Android) |
| Session management | Secure session token storage and expiry |
| Device binding | Bind tokens to specific device/hardware |
| Certificate-based auth | mTLS support for API authentication |

---

### 2.8 Requirement 8 — Identify Users and Authenticate Access

#### Req 8.2: User identification and authentication

| Requirement | OpenGuard Implementation |
|-------------|--------------------------|
| Unique IDs for users | User ID never reuse guidance |
| MFA for non-console admin | MFA integration guide |
| Passwords/passphrases meet complexity | Password validation utilities |
| Accounts locked after 6 failures | Lockout policy helper |

#### Req 8.3: User authentication factors managed securely

**Software-Based OTP (Req 8.5):**
```kotlin
// OpenGuard TOTP implementation (RFC 6238)
val totp = OpenGuard.auth.generateTOTP(
    secret = encryptedSecret,
    timeStep = 30,
    digits = 6,
    algorithm = TOTPAlgorithm.SHA256
)
```

| Feature | PCI Req |
|---------|---------|
| TOTP generation (RFC 6238) | Req 8.5 |
| Software token binding to device | Req 8.5.1 |
| Replay attack prevention | Req 8.5.1 |
| Biometric as second factor | Req 8.3 |

#### Req 8.6: Application and system accounts managed

| Feature | OpenGuard Implementation |
|---------|--------------------------|
| API key secure storage | Keystore/Keychain storage |
| Automatic session expiry | Configurable timeout (recommended: 15 min) |
| Re-authentication for sensitive ops | Step-up authentication API |

---

### 2.9 Requirement 9 — Restrict Physical Access

#### Req 9.5: Point-of-Interaction (POI) devices protected from tampering

**Mobile Payment Terminals (mPOS):**

| Check | OpenGuard Feature |
|-------|------------------|
| Device integrity verification | Hardware attestation |
| Unauthorized physical access | Accelerometer/tamper sensor monitoring |
| Software tampering | Code signing verification |
| Firmware validation | Secure boot verification (where possible) |

---

### 2.10 Requirement 10 — Log and Monitor All Access to System Components

#### Req 10.2: Audit logs capture all individual user access

**Mobile App Logging Requirements:**

| Event | Must Log | OpenGuard Support |
|-------|----------|-------------------|
| Authentication attempts | Yes | Built-in event |
| Authentication failures | Yes | Built-in event |
| Root/Jailbreak detection | Yes | RASP event |
| Tamper detection | Yes | RASP event |
| Hook detection | Yes | RASP event |
| Cert pinning failures | Yes | Network event |
| Session creation/termination | Yes | Session event |

#### Req 10.3: Audit logs protected from destruction and unauthorized modifications

```kotlin
// OpenGuard tamper-evident log
OpenGuard.auditLog.record(
    event = SecurityEvent.ROOT_DETECTED,
    severity = Severity.CRITICAL,
    metadata = mapOf("method" to "su_binary")
) // Logs are HMAC-signed and optionally encrypted
```

#### Req 10.5: Audit log history retained

- Minimum 12 months retention (with 3 months immediately available)
- Remote log submission to server (not stored only on device)
- Log shipping with mutual TLS authentication

---

### 2.11 Requirement 11 — Test Security of Systems and Networks

#### Req 11.3: External and internal vulnerabilities managed

**OpenGuard provides:**
- [ ] Automated RASP test suite
- [ ] Penetration testing checklist
- [ ] Vulnerability disclosure policy template
- [ ] Third-party security assessment guide

#### Req 11.4: External and internal penetration testing performed

**Penetration Testing Scope for Mobile Apps:**
1. Static analysis (reverse engineering attempt)
2. Dynamic analysis (Frida, debugger attachment)
3. Network analysis (MITM attempt)
4. Authentication bypass attempts
5. Sensitive data exposure analysis
6. Binary analysis (extract secrets)
7. Runtime manipulation attempts

**OpenGuard Pen Test Integration:**
```kotlin
// Test mode — simulates attacks for pen testing validation
OpenGuard.penTest.simulateRootEnvironment()
OpenGuard.penTest.simulateFridaPresence()
OpenGuard.penTest.simulateDebuggerAttach()
```

#### Req 11.6: Unauthorized changes to payment pages detected

**Mobile equivalent:** Detect unauthorized modifications to app binary
- Code signing verification at runtime
- DEX/Mach-O hash verification
- Resource integrity checks

---

### 2.12 Requirement 12 — Support Information Security with Organizational Policies

#### Req 12.3.4: Hardware and software technologies reviewed annually

**OpenGuard Annual Review Checklist:**
- [ ] Review all third-party SDK dependencies
- [ ] Update certificate pins (if needed)
- [ ] Review and update threat detection signatures
- [ ] Update minimum OS/API level requirements
- [ ] Review RASP detection effectiveness
- [ ] Update cryptographic algorithms if deprecated

---

## 3. OWASP MASVS 2.0 Mapping

OWASP MASVS (Mobile Application Verification Standard) aligns closely with PCI DSS for mobile.

### MASVS-STORAGE
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-STORAGE-1 | No sensitive data in app logs | Log sanitization |
| MASVS-STORAGE-2 | No sensitive data outside device | Secure storage API |

### MASVS-CRYPTO
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-CRYPTO-1 | Strong cryptography only | Crypto API |
| MASVS-CRYPTO-2 | Random values are random | SecureRandom enforcement |

### MASVS-AUTH
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-AUTH-1 | Biometric auth when needed | Biometric API |
| MASVS-AUTH-2 | Session management | Session API |
| MASVS-AUTH-3 | Stateless auth tokens | JWT secure handling |

### MASVS-NETWORK
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-NETWORK-1 | Traffic encrypted on network | TLS enforcement |
| MASVS-NETWORK-2 | TLS settings up to date | TLS config |
| MASVS-NETWORK-3 | App verifies identity of remote endpoint | Certificate pinning |

### MASVS-PLATFORM
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-PLATFORM-1 | No sensitive data via IPC | IPC monitoring |
| MASVS-PLATFORM-2 | No sensitive data in UI components | Screenshot prevention |
| MASVS-PLATFORM-3 | No JS injection in WebViews | WebView security |

### MASVS-CODE
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-CODE-1 | No debug features in production | Debug detection |
| MASVS-CODE-2 | Security warnings addressed | Lint checks |
| MASVS-CODE-3 | Up-to-date third-party components | Dependency check |
| MASVS-CODE-4 | Minimum OS version enforced | Min API level check |

### MASVS-RESILIENCE
| ID | Requirement | OpenGuard Feature |
|----|-------------|-------------------|
| MASVS-RESILIENCE-1 | Root/Jailbreak detection | RASP |
| MASVS-RESILIENCE-2 | Anti-tampering | Tamper detection |
| MASVS-RESILIENCE-3 | Anti-static analysis | Obfuscation (Phase 2) |
| MASVS-RESILIENCE-4 | Anti-dynamic analysis | Debugger/Hook detection |

---

## 4. PCI DSS 4.0 Implementation Checklist

Use this checklist to verify your OpenGuard-integrated app is PCI DSS 4.0 compliant.

### 4.1 Network Security
- [ ] Certificate pinning enabled for all payment API endpoints
- [ ] TLS 1.2 minimum enforced (TLS 1.3 preferred)
- [ ] No HTTP allowed (enforce HTTPS-only)
- [ ] Proxy/MITM detection enabled
- [ ] Certificate expiry monitoring configured

### 4.2 Data Protection
- [ ] No PAN stored in plaintext anywhere
- [ ] No CVV/SAD stored (never, even encrypted)
- [ ] Sensitive fields use secure text input
- [ ] Clipboard cleared after sensitive data copied
- [ ] Screen recording/screenshot prevention enabled
- [ ] App backgrounding hides sensitive data

### 4.3 Authentication
- [ ] MFA implemented (biometric + PIN minimum)
- [ ] Session timeout configured (max 15 minutes)
- [ ] Device binding implemented for high-value operations
- [ ] TOTP software token bound to device
- [ ] Account lockout after failed attempts

### 4.4 Secure Storage
- [ ] All secrets stored in Android Keystore / iOS Keychain
- [ ] Biometric access control on sensitive keys
- [ ] Key attestation verified (hardware-backed)
- [ ] No secrets in SharedPreferences (unencrypted)
- [ ] No secrets in UserDefaults (unencrypted)

### 4.5 Runtime Protection (RASP)
- [ ] Root/Jailbreak detection enabled
- [ ] Debugger detection enabled
- [ ] Hook detection (Frida/Xposed/Substrate) enabled
- [ ] Emulator/Simulator detection enabled
- [ ] Tamper detection enabled (signature verification)
- [ ] Anti-repackaging protection enabled

### 4.6 Audit Logging
- [ ] All security events logged
- [ ] Logs transmitted to server (not stored only on device)
- [ ] Log integrity protection enabled (HMAC)
- [ ] Log retention policy implemented (12 months)

### 4.7 Build Configuration
- [ ] Release build is not debuggable (`android:debuggable=false`)
- [ ] No debug logs in release build
- [ ] Code obfuscation enabled (R8/ProGuard minimum)
- [ ] All debug features disabled
- [ ] Backup disabled (`android:allowBackup=false`)

---

## 5. PCI DSS 4.0 New Requirements (Effective March 2025)

These are net-new requirements in v4.0 not present in v3.2.1:

| Req | Description | OpenGuard Relevance |
|-----|-------------|---------------------|
| 6.4.3 | All payment page scripts managed and authorized | Third-party SDK integrity |
| 11.6.1 | Change and tamper detection for payment pages | Binary integrity checks |
| 8.5 | MFA for software-based tokens | TOTP device binding |
| 8.6.3 | Passwords/passphrases for application accounts protected | Secure credential storage |
| 12.3.4 | Annual review of hardware and software | Annual review checklist |

---

## 6. Compliance Evidence Templates

### 6.1 Network Security Evidence
```
Control: Certificate Pinning Implementation
Implemented By: OpenGuard SDK v{version}
Method: SPKI pinning via OpenGuard.network.configurePinning()
Pins Configured: {list of sha256 pins}
Backup Pins: {list of backup pins}
Review Date: {date}
Reviewer: {name}
```

### 6.2 RASP Evidence
```
Control: Runtime Application Self-Protection
Implemented By: OpenGuard SDK v{version}
Detections Enabled:
  - Root/Jailbreak: YES
  - Debugger: YES
  - Hook (Frida/Xposed/Substrate): YES
  - Emulator/Simulator: YES
  - Tamper: YES
Threat Response: {BLOCK/LOG/WARN}
Test Date: {date}
Pen Test Results: {PASS/FAIL}
```

### 6.3 Cryptography Evidence
```
Control: Strong Cryptography for Stored Data
Algorithm: AES-256-GCM
Key Storage: Hardware-backed (Android Keystore / iOS Secure Enclave)
Key Rotation: Annual / On-compromise
Attestation: Verified hardware backing
Review Date: {date}
```
