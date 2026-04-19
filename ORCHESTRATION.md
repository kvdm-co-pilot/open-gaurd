# OpenGuard — Task Orchestration

> **Agent instruction:** Read this file and pick up the next available task. Find the first `☐` task in the lowest wave where all dependencies are `✅`.

---

## Status Key

| Symbol | Meaning |
|--------|---------|
| ✅ | Done |
| 🔄 | In progress |
| ☐ | Not started |
| 🔒 | Blocked |
| ⚠️ | Needs human review |

## Rules (every task)

1. Build must pass (`./gradlew build`)
2. Tests must pass (`./gradlew test`)
3. Follow existing Kotlin code patterns
4. All public APIs need KDoc documentation
5. Security implementations need `@security-review` sign-off
6. Human approval required before moving to next task

## Delegation Map

| ID Prefix | Agent | Scope |
|-----------|-------|-------|
| KMP-* | @kmp-core | Shared KMP code (commonMain) |
| AND-* | @android | Android platform code |
| IOS-* | @ios | iOS platform code |
| SEC-* | @security-review | Security analysis |
| QA-* | @qa | Tests and validation |
| OPS-* | @devops | CI/CD and build system |
| RES-* | @research | Technical research |
| DOC-* | @docs | Documentation |

---

## Wave 0 — Research & Foundation

_No code dependencies. Research and infrastructure can start immediately._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | RES-001 | Root detection bypass resistance research | @research | — |
| ☐ | RES-002 | iOS jailbreak detection research (palera1n, rootless) | @research | — |
| ☐ | RES-003 | Frida detection techniques research | @research | — |
| ☐ | RES-004 | Certificate pinning best practices research | @research | — |
| ☐ | OPS-001 | Configure Gradle KMP build system (verify all targets compile) | @devops | — |
| ☐ | OPS-002 | Set up GitHub Actions CI pipeline (build + test) | @devops | OPS-001 |

---

## Wave 1 — Core API Design

_Shared interfaces that platform agents will implement._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | KMP-001 | Define DetectionApi expect interface (all detection methods) | @kmp-core | OPS-001 |
| ☐ | KMP-002 | Define CryptoApi expect interface (AES, SHA, HMAC, TOTP) | @kmp-core | OPS-001 |
| ☐ | KMP-003 | Define StorageApi expect interface (put, get, delete, list) | @kmp-core | OPS-001 |
| ☐ | KMP-004 | Implement OpenGuardConfig DSL (builder pattern, validation) | @kmp-core | KMP-001 |
| ☐ | KMP-005 | Implement DetectionEngine (periodic checks, threat dispatch) | @kmp-core | KMP-001 |
| ☐ | QA-001 | Set up test framework (JUnit 5, kotlin.test, test fixtures) | @qa | OPS-001 |

---

## Wave 2a — Android Detection

_Android `actual` implementations. Can run in parallel with Wave 2b (iOS)._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | AND-001 | Implement root detection (su, Magisk, SuperSU, properties) | @android | KMP-001, RES-001 |
| ☐ | AND-002 | Implement debugger detection (TracerPid, timing, flags) | @android | KMP-001 |
| ☐ | AND-003 | Implement Frida/hook detection (maps, ports, classloader) | @android | KMP-001, RES-003 |
| ☐ | AND-004 | Implement emulator detection (build props, sensors, telephony) | @android | KMP-001 |
| ☐ | AND-005 | Implement tamper detection (signature, installer, checksum) | @android | KMP-001 |
| ☐ | SEC-001 | Security review: all Android detection implementations | @security-review | AND-001, AND-002, AND-003, AND-004, AND-005 |
| ☐ | QA-002 | Android detection unit tests (all detection types) | @qa | AND-001, AND-002, AND-003, AND-004, AND-005 |

---

## Wave 2b — iOS Detection

_iOS `actual` implementations. Runs in parallel with Wave 2a (Android)._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | IOS-001 | Implement jailbreak detection (files, sandbox, dyld, URL schemes) | @ios | KMP-001, RES-002 |
| ☐ | IOS-002 | Implement debugger detection (sysctl, ptrace, getppid) | @ios | KMP-001 |
| ☐ | IOS-003 | Implement Frida/hook detection (dylibs, ports, Substrate) | @ios | KMP-001, RES-003 |
| ☐ | IOS-004 | Implement simulator detection (env, architecture, model) | @ios | KMP-001 |
| ☐ | IOS-005 | Implement tamper detection (code signature, provisioning, hash) | @ios | KMP-001 |
| ☐ | SEC-002 | Security review: all iOS detection implementations | @security-review | IOS-001, IOS-002, IOS-003, IOS-004, IOS-005 |
| ☐ | QA-003 | iOS detection unit tests (all detection types) | @qa | IOS-001, IOS-002, IOS-003, IOS-004, IOS-005 |

---

## Wave 3 — Network Security

_Certificate pinning and TLS enforcement for both platforms._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | KMP-006 | Define NetworkApi expect interface (pinning, TLS config) | @kmp-core | KMP-002 |
| ☐ | AND-006 | Implement OkHttp certificate pinning (SPKI, pin rotation) | @android | KMP-006, RES-004 |
| ☐ | IOS-006 | Implement URLSession certificate pinning (delegate, SPKI) | @ios | KMP-006, RES-004 |
| ☐ | SEC-003 | Security review: certificate pinning implementations | @security-review | AND-006, IOS-006 |
| ☐ | QA-004 | Network security tests (pinning, TLS validation) | @qa | AND-006, IOS-006 |

---

## Wave 4 — Secure Storage & Cryptography

_Hardware-backed key storage and encryption for both platforms._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | AND-007 | Implement Android Keystore integration (key generation, storage) | @android | KMP-003 |
| ☐ | IOS-007 | Implement iOS Keychain + Secure Enclave integration | @ios | KMP-003 |
| ☐ | AND-008 | Implement AES-256-GCM encryption (Android Keystore-backed) | @android | KMP-002, AND-007 |
| ☐ | IOS-008 | Implement AES-256-GCM encryption (iOS Keychain-backed) | @ios | KMP-002, IOS-007 |
| ☐ | SEC-004 | Security review: crypto + storage implementations | @security-review | AND-007, AND-008, IOS-007, IOS-008 |
| ☐ | QA-005 | Crypto + storage tests (encrypt/decrypt, key lifecycle) | @qa | AND-007, AND-008, IOS-007, IOS-008 |

---

## Wave 5 — Platform Attestation

_Play Integrity (Android) and App Attest (iOS) integration._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | RES-005 | Play Integrity + App Attest API research | @research | — |
| ☐ | AND-009 | Implement Play Integrity API integration | @android | RES-005 |
| ☐ | IOS-009 | Implement App Attest API integration | @ios | RES-005 |
| ☐ | SEC-005 | Security review: attestation implementations | @security-review | AND-009, IOS-009 |

---

## Wave 6 — Documentation & Distribution

_Final documentation, publishing, and release automation._

| Status | ID | Task | Owner | Depends On |
|--------|----|------|-------|------------|
| ☐ | DOC-001 | API documentation (all public interfaces with examples) | @docs | Wave 4 complete |
| ☐ | DOC-002 | Integration guide (Android + iOS setup, configuration) | @docs | Wave 4 complete |
| ☐ | DOC-003 | PCI DSS 4.0 compliance mapping document | @docs | SEC-001, SEC-002, SEC-003, SEC-004, SEC-005 |
| ☐ | OPS-003 | Maven Central publishing setup (POM, signing, Sonatype) | @devops | Wave 4 complete |
| ☐ | OPS-004 | Release automation (versioning, CHANGELOG, tags) | @devops | OPS-003 |

---

## Progress Summary

| Wave | Total | Done | In Progress | Not Started | Blocked |
|------|-------|------|-------------|-------------|---------|
| 0 | 6 | 0 | 0 | 6 | 0 |
| 1 | 6 | 0 | 0 | 6 | 0 |
| 2a | 7 | 0 | 0 | 7 | 0 |
| 2b | 7 | 0 | 0 | 7 | 0 |
| 3 | 5 | 0 | 0 | 5 | 0 |
| 4 | 6 | 0 | 0 | 6 | 0 |
| 5 | 4 | 0 | 0 | 4 | 0 |
| 6 | 5 | 0 | 0 | 5 | 0 |
| **Total** | **46** | **0** | **0** | **46** | **0** |
