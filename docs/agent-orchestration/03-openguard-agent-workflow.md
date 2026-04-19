# OpenGuard — Ideal Agent Orchestration Workflow

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Status:** Recommended Architecture  
**Classification:** Internal / Architecture

---

## 1. Overview

This document defines the **ideal multi-agent orchestration workflow** for the OpenGuard project — an open-source RASP SDK for Kotlin Multiplatform targeting PCI DSS 4.0 compliance. It synthesizes April 2026 best practices from GitHub Copilot (Mission Control, custom agents), industry frameworks (Microsoft Agent Framework 1.0), and analysis of 2,500+ AGENTS.md repositories.

> **⚠️ Runtime constraint:** This workflow is designed to run entirely within **GitHub Copilot** (the only model available in Copilot coding agent / cloud agent / web sessions). Claude Opus 4.6 and other external models are **not available** within a Copilot session. Features like Agent HQ (multi-model) and `/fleet` (parallel CLI) are separate tools that require their own environments and are documented as optional enhancements where noted.

The design optimizes for:
- **Security correctness** (RASP SDK must be hardened)
- **PCI DSS auditability** (full task trail for compliance)
- **Kotlin Multiplatform complexity** (Android + iOS platform code)
- **Parallel throughput** (independent modules worked on simultaneously)
- **Human-in-the-loop** (security-critical decisions require human approval)

---

## 2. Agent Team

### 2.1 Team Structure

```
Human Architect (approve/reject all changes)
  ↕
@orchestrator (supervisor — reads task board, delegates, validates)
  ├── @kmp-core       → Kotlin Multiplatform shared code (commonMain)
  ├── @android         → Android-specific implementations (androidMain)
  ├── @ios             → iOS-specific implementations (iosMain)
  ├── @security-review → Cross-platform security analysis & hardening
  ├── @qa              → Tests, validation, compliance verification
  ├── @devops          → CI/CD, Gradle, build system, distribution
  ├── @research        → Technical research, PoC, feasibility studies
  └── @docs            → Documentation, API specs, compliance docs
```

### 2.2 Agent Definitions

#### @orchestrator — Supervisor Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/orchestrator.agent.md` |
| **Role** | Read ORCHESTRATION.md, delegate tasks, validate results |
| **Tools** | `read`, `edit`, `search`, `agent`, `todo`, `execute` |
| **Sub-agents** | `kmp-core`, `android`, `ios`, `security-review`, `qa`, `devops`, `research`, `docs` |
| **Model preference** | Copilot (default model in coding agent session) |

**Responsibilities:**
1. Read ORCHESTRATION.md → find next available task (lowest wave, dependencies met)
2. Mark task 🔄 (in progress)
3. Read source docs + source code for the task
4. Build a self-contained brief with all context
5. Delegate to the correct worker agent
6. Validate worker output (build + test + security check)
7. If validation passes, mark task ✅
8. Report to human for approval

**Why separate from workers:** The orchestrator never writes code. It only reads, delegates, and validates. This prevents scope creep and ensures every code change goes through a specialized agent.

---

#### @kmp-core — Kotlin Multiplatform Shared Code Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/kmp-core.agent.md` |
| **Scope** | `openguard-core/src/commonMain/` |
| **Role** | Shared API interfaces, expect declarations, detection engine, config DSL |
| **Tools** | `read`, `edit`, `search`, `execute` |
| **ID prefix** | `KMP-*` |

**Scope details:**
- `com/openguard/core/OpenGuard.kt` — Main entry point
- `com/openguard/core/OpenGuardConfig.kt` — Configuration DSL
- `com/openguard/core/api/*` — expect interfaces (DetectionApi, CryptoApi, NetworkApi, StorageApi)
- `com/openguard/core/detection/*` — Detection engine, ThreatEvent, ThreatSeverity, etc.
- `com/openguard/core/network/*` — Network config
- `com/openguard/core/storage/*` — Audit log config

**Constraints:**
- NEVER write platform-specific code (no `androidMain` or `iosMain`)
- ALWAYS use `expect`/`actual` for platform-dependent functionality
- Follow existing KMP patterns in the codebase
- All public APIs must be `@Throws`-annotated for iOS interop

---

#### @android — Android Platform Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/android.agent.md` |
| **Scope** | `openguard-core/src/androidMain/`, `openguard-android/` |
| **Role** | Android `actual` implementations, Android-specific extensions |
| **Tools** | `read`, `edit`, `search`, `execute` |
| **ID prefix** | `AND-*` |

**Scope details:**
- Root detection (Magisk, SuperSU, file checks)
- ART hook detection (Frida, Xposed, Substrate)
- Emulator detection
- Debugger detection (ptrace, debugger flags)
- Tamper/repackaging detection
- Android Keystore integration
- OkHttp certificate pinning
- Play Integrity API integration

**Constraints:**
- NEVER modify `commonMain` code
- NEVER modify iOS code
- Must target API 21+ (minSdk)
- Use Android Keystore for all cryptographic operations
- JNI/NDK code must be in a separate `jni/` directory

---

#### @ios — iOS Platform Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/ios.agent.md` |
| **Scope** | `openguard-core/src/iosMain/` |
| **Role** | iOS `actual` implementations via Kotlin/Native |
| **Tools** | `read`, `edit`, `search`, `execute` |
| **ID prefix** | `IOS-*` |

**Scope details:**
- Jailbreak detection (palera1n, rootless, tethered)
- Substrate/Frida hook detection
- Simulator detection
- Debugger detection (sysctl, ptrace)
- Tamper detection (code signature verification)
- iOS Keychain + Secure Enclave integration
- URLSession certificate pinning
- App Attest API integration

**Constraints:**
- NEVER modify `commonMain` code
- NEVER modify Android code
- Use Kotlin/Native interop for all iOS framework calls
- All `cinterop` definitions must be in `.def` files
- Memory management: explicit `autoreleasepool` for Obj-C interop

---

#### @security-review — Security Specialist Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/security-review.agent.md` |
| **Scope** | All source code (read-only for production code) |
| **Role** | Security analysis, bypass resistance review, hardening recommendations |
| **Tools** | `read`, `search` |
| **Model preference** | Copilot (default model — Claude Opus 4.6 not available in coding agent sessions) |
| **ID prefix** | `SEC-*` |

**Responsibilities:**
1. Review all security-critical implementations for bypass vulnerabilities
2. Analyze detection algorithms against known bypass techniques
3. Verify cryptographic implementations follow best practices
4. Check for timing attacks, side channels, and information leakage
5. Validate OWASP MASVS 2.0 Level 2 compliance
6. Generate security assessment reports

**Constraints:**
- READ-ONLY access to production code (can only modify its own reports)
- Can write to `docs/security-reviews/` only
- Must reference OWASP MSTG test cases
- Must evaluate against Magisk DenyList, palera1n, Frida latest

**Why a dedicated security agent:** OpenGuard is a security SDK. Every implementation must be reviewed by an agent whose sole purpose is finding security weaknesses. Since Claude Opus 4.6 is not available within Copilot coding sessions, this agent compensates by having comprehensive security-focused prompting, OWASP checklists, and mandatory human sign-off.

---

#### @qa — Quality Assurance Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/qa.agent.md` |
| **Scope** | `*/test/`, `*/androidTest/`, `*/iosTest/`, `e2e/` |
| **Role** | Write and maintain tests, run test suites, validate coverage |
| **Tools** | `read`, `edit`, `search`, `execute` |
| **ID prefix** | `QA-*` |

**Responsibilities:**
1. Write unit tests for all new implementations
2. Write integration tests for cross-module interactions
3. Create platform-specific instrumented tests
4. Run test suites and report results
5. Verify code coverage meets minimum thresholds
6. Validate PCI DSS 4.0 compliance test cases

**Constraints:**
- READ-ONLY on production code (can only modify test files)
- Must achieve >80% code coverage for new code
- Tests must cover both success and bypass/failure scenarios
- Platform tests must run on both Android and iOS

---

#### @devops — DevOps & Build Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/devops.agent.md` |
| **Scope** | `build.gradle.kts`, `settings.gradle.kts`, `.github/workflows/`, `gradle/` |
| **Role** | CI/CD, Gradle build system, distribution pipeline |
| **Tools** | `read`, `edit`, `search`, `execute` |
| **ID prefix** | `OPS-*` |

**Responsibilities:**
1. Configure Gradle build for KMP (Android + iOS targets)
2. Set up GitHub Actions CI/CD pipeline
3. Configure Maven Central publishing
4. Set up code signing and artifact verification
5. Configure R8/ProGuard rules for SDK consumers
6. Set up automated security scanning in CI

**Constraints:**
- NEVER modify application source code
- ONLY modify build files, CI config, and infrastructure
- Must support Gradle 8.x and AGP 8.x
- iOS builds must work with Xcode 16+

---

#### @research — Research Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/research.agent.md` |
| **Scope** | `docs/research/` |
| **Role** | Technical research, feasibility studies, PoC implementations |
| **Tools** | `read`, `edit`, `search`, `execute` |
| **ID prefix** | `RES-*` |

**Responsibilities:**
1. Research security detection techniques and bypass methods
2. Evaluate open-source libraries and tools
3. Create feasibility studies for new features
4. Write technical research reports
5. Prototype detection algorithms (in `/tmp` or `docs/research/poc/`)

**Constraints:**
- Research output goes to `docs/research/` only
- PoC code goes to `docs/research/poc/` (never production code)
- Must cite sources and provide reproducible findings
- Must align research with ORCHESTRATION.md task requirements

---

#### @docs — Documentation Agent

| Property | Value |
|----------|-------|
| **File** | `.github/agents/docs.agent.md` |
| **Scope** | `docs/`, `README.md`, `CHANGELOG.md`, KDoc comments |
| **Role** | API documentation, compliance docs, guides |
| **Tools** | `read`, `edit`, `search` |
| **ID prefix** | `DOC-*` |

**Responsibilities:**
1. Write and maintain API documentation
2. Update README.md with new features
3. Write integration guides for SDK consumers
4. Maintain PCI DSS compliance documentation
5. Generate CHANGELOG entries
6. Add KDoc comments to public APIs

**Constraints:**
- NO code changes (only documentation files and comments)
- Must follow existing documentation style
- API docs must include code examples
- Compliance docs must reference specific PCI DSS requirements

---

## 3. Task Board Design

### 3.1 ORCHESTRATION.md Structure

```markdown
# OpenGuard — Task Orchestration

> Agent instruction: "Read ORCHESTRATION.md and pick up the next available task."

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
5. Security implementations need @security-review sign-off

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
```

### 3.2 Wave Design for OpenGuard

**Wave 0 — Research & Foundation (no code dependencies)**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| RES-001 | Root detection bypass resistance research | @research | — |
| RES-002 | iOS jailbreak detection research | @research | — |
| RES-003 | Frida detection research | @research | — |
| RES-004 | Certificate pinning best practices | @research | — |
| OPS-001 | Configure Gradle KMP build system | @devops | — |
| OPS-002 | Set up GitHub Actions CI pipeline | @devops | OPS-001 |

**Wave 1 — Core API (shared interfaces)**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| KMP-001 | Define DetectionApi expect interface | @kmp-core | OPS-001 |
| KMP-002 | Define CryptoApi expect interface | @kmp-core | OPS-001 |
| KMP-003 | Define StorageApi expect interface | @kmp-core | OPS-001 |
| KMP-004 | Implement OpenGuardConfig DSL | @kmp-core | KMP-001 |
| KMP-005 | Implement DetectionEngine | @kmp-core | KMP-001 |
| QA-001 | Set up test framework (JUnit + XCTest) | @qa | OPS-001 |

**Wave 2 — Android Detection (parallel with iOS)**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| AND-001 | Implement root detection | @android | KMP-001, RES-001 |
| AND-002 | Implement debugger detection | @android | KMP-001 |
| AND-003 | Implement Frida/hook detection | @android | KMP-001, RES-003 |
| AND-004 | Implement emulator detection | @android | KMP-001 |
| AND-005 | Implement tamper detection | @android | KMP-001 |
| SEC-001 | Security review: Android detections | @security-review | AND-001..005 |
| QA-002 | Android detection unit tests | @qa | AND-001..005 |

**Wave 2 (parallel) — iOS Detection**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| IOS-001 | Implement jailbreak detection | @ios | KMP-001, RES-002 |
| IOS-002 | Implement debugger detection | @ios | KMP-001 |
| IOS-003 | Implement Frida/hook detection | @ios | KMP-001, RES-003 |
| IOS-004 | Implement simulator detection | @ios | KMP-001 |
| IOS-005 | Implement tamper detection | @ios | KMP-001 |
| SEC-002 | Security review: iOS detections | @security-review | IOS-001..005 |
| QA-003 | iOS detection unit tests | @qa | IOS-001..005 |

**Wave 3 — Network Security**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| KMP-006 | Define NetworkApi expect interface | @kmp-core | KMP-002 |
| AND-006 | OkHttp certificate pinning | @android | KMP-006, RES-004 |
| IOS-006 | URLSession certificate pinning | @ios | KMP-006, RES-004 |
| SEC-003 | Security review: certificate pinning | @security-review | AND-006, IOS-006 |
| QA-004 | Network security tests | @qa | AND-006, IOS-006 |

**Wave 4 — Secure Storage & Crypto**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| AND-007 | Android Keystore integration | @android | KMP-003 |
| IOS-007 | iOS Keychain + Secure Enclave | @ios | KMP-003 |
| AND-008 | AES-256-GCM (Android) | @android | KMP-002, AND-007 |
| IOS-008 | AES-256-GCM (iOS) | @ios | KMP-002, IOS-007 |
| SEC-004 | Security review: crypto + storage | @security-review | AND-007..008, IOS-007..008 |
| QA-005 | Crypto + storage tests | @qa | AND-007..008, IOS-007..008 |

**Wave 5 — Platform Attestation**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| RES-005 | Play Integrity + App Attest research | @research | — |
| AND-009 | Play Integrity API integration | @android | RES-005 |
| IOS-009 | App Attest API integration | @ios | RES-005 |
| SEC-005 | Security review: attestation | @security-review | AND-009, IOS-009 |

**Wave 6 — Documentation & Distribution**
| ID | Task | Owner | Depends On |
|----|------|-------|------------|
| DOC-001 | API documentation | @docs | Wave 4 |
| DOC-002 | Integration guide | @docs | Wave 4 |
| DOC-003 | PCI DSS compliance mapping | @docs | SEC-001..005 |
| OPS-003 | Maven Central publishing setup | @devops | Wave 4 |
| OPS-004 | Release automation | @devops | OPS-003 |

---

## 4. Workflow Execution

### 4.1 Standard Task Flow

```
1. Human opens Copilot Chat
2. Types: /run-pipeline (or @orchestrator)
3. Orchestrator reads ORCHESTRATION.md
4. Finds next ☐ task where dependencies are ✅
5. Marks task 🔄
6. Reads source docs relevant to the task
7. Reads source files the worker will modify
8. Builds self-contained brief (see template below)
9. Delegates to worker agent: runSubagent(@worker, brief)
10. Worker:
    a. Reads files listed in brief
    b. Implements changes
    c. Runs tests
    d. Reports back: files changed, what was added, test results
11. Orchestrator:
    a. Reads modified files to verify
    b. Runs: ./gradlew build
    c. Runs: ./gradlew test
    d. If security task → delegates to @security-review
12. If all pass → marks ✅
13. Reports to human for approval
14. Human approves → orchestrator picks up next task
```

### 4.2 Worker Brief Template

Every delegation from the orchestrator MUST include:

```markdown
## Task
{ID}: {Title}

## Context
- Wave: {N}
- What's already built: {list of completed tasks}
- How this fits in: {relationship to other components}

## Files to Read First
- {path} — {why this file matters}
- {path} — {what patterns to follow}

## Files to Create/Modify
- {path} — {what to add/change}

## Requirements (verbatim from spec)
{Paste exact acceptance criteria}

## Current Code State
{Paste relevant types, function signatures, existing patterns}
Example:
```kotlin
// From DetectionApi.kt
expect interface DetectionApi {
    suspend fun checkRoot(): DetectionResult
    suspend fun checkDebugger(): DetectionResult
    suspend fun checkAll(): List<DetectionResult>
}
```

## Technical Constraints
- Kotlin version: {X}
- Min Android SDK: 21
- iOS deployment target: 15.0
- {Any other constraints}

## When Done
1. Run: ./gradlew build
2. Run: ./gradlew test
3. Report: files changed, functions added, test results, any concerns
```

### 4.3 Security Review Flow

For all security-critical tasks (detection algorithms, crypto, network):

```
Worker completes implementation
  ↓
Orchestrator delegates to @security-review:
  ↓
@security-review reads:
  - Implementation code
  - Known bypass techniques (from research docs)
  - OWASP MASVS requirements
  ↓
Produces security assessment:
  - Bypass resistance rating (1-5)
  - Known vulnerabilities
  - Hardening recommendations
  - MASVS compliance status
  ↓
If critical issues found → task marked ⚠️, sent back to worker
If no issues → task proceeds to ✅
```

### 4.4 Parallel Execution with `/fleet` (CLI Only — Not Available in Web Session)

> **⚠️ Note:** The `/fleet` command requires the Copilot CLI installed locally. It is **not available within the Copilot coding agent (cloud agent) web session**. Within a web session, tasks are executed sequentially by the orchestrator. Use `/fleet` only when working from a local terminal.

For independent tasks within the same wave, use `/fleet` from Copilot CLI:

```
/fleet Implement the following OpenGuard detections in parallel:
- AND-001: Root detection in openguard-core/src/androidMain/
- AND-002: Debugger detection in openguard-core/src/androidMain/
- AND-003: Frida hook detection in openguard-core/src/androidMain/
- AND-004: Emulator detection in openguard-core/src/androidMain/
Each agent should follow the DetectionApi expect interface from commonMain.
```

### 4.5 Cross-Model Validation (Optional — Requires Agent HQ Outside Coding Session)

> **⚠️ Note:** Cross-model validation requires Agent HQ, which is a **separate workflow from the Copilot coding agent session**. Within a coding session, rely on the `@security-review` agent + human review instead.

For the most security-critical implementations, when Agent HQ is available (from GitHub.com UI):

```
1. Assign task to both Copilot and Claude Opus 4.6 via Agent HQ
2. Both agents produce independent implementations
3. Compare outputs:
   a. If substantial agreement → proceed with best version
   b. If disagreement → flag for human review with both versions
4. Security-review agent analyzes both approaches
5. Human makes final decision
```

---

## 5. Configuration Files

### 5.1 File Structure

```
open-guard/
├── .github/
│   ├── agents/
│   │   ├── orchestrator.agent.md
│   │   ├── kmp-core.agent.md
│   │   ├── android.agent.md
│   │   ├── ios.agent.md
│   │   ├── security-review.agent.md
│   │   ├── qa.agent.md
│   │   ├── devops.agent.md
│   │   ├── research.agent.md
│   │   └── docs.agent.md
│   ├── prompts/
│   │   ├── run-pipeline.prompt.md
│   │   ├── security-review.prompt.md
│   │   └── parallel-detections.prompt.md
│   └── copilot-instructions.md
├── ORCHESTRATION.md
├── AGENTS.md
└── docs/
    ├── agent-orchestration/
    │   ├── 01-multi-agent-orchestration-landscape-2026.md
    │   ├── 02-claude-vs-copilot-comparison.md
    │   └── 03-openguard-agent-workflow.md  (this document)
    └── security-reviews/
        └── (generated by @security-review agent)
```

### 5.2 AGENTS.md (Root-Level Project Instructions)

```markdown
# OpenGuard — Agent Instructions

## Project
OpenGuard is an open-source RASP SDK for Kotlin Multiplatform (Android + iOS).
It provides runtime security checks for fintech apps targeting PCI DSS 4.0.

## Tech Stack
- Kotlin 2.1.x (Multiplatform)
- Gradle 8.x with Kotlin DSL
- Android: minSdk 21, targetSdk 35, AGP 8.x
- iOS: deployment target 15.0, Xcode 16+
- Testing: JUnit 5 (Android), XCTest (iOS)

## Commands
- Build: `./gradlew build`
- Test: `./gradlew test`
- Lint: `./gradlew detekt`
- Android tests: `./gradlew connectedAndroidTest`

## Code Style
- Kotlin coding conventions
- No wildcard imports
- Use `suspend` functions for async operations
- Use `expect`/`actual` for platform code
- All public APIs must have KDoc documentation

## Boundaries
### Always
- Run tests before reporting completion
- Follow existing code patterns in the codebase
- Use `expect`/`actual` for platform differences

### Ask First
- Adding new dependencies
- Changing public API signatures
- Modifying build configuration

### Never
- Commit secrets, API keys, or certificates
- Modify `.github/agents/` files
- Bypass security review for detection implementations
- Use deprecated Android APIs without fallback
```

### 5.3 VS Code Settings (Auto-Approve)

```json
{
  "chat.tools.autoApprove": true,
  "chat.agent.maxRequests": 50
}
```

---

## 6. Design Principles

| # | Principle | Implementation |
|---|-----------|---------------|
| 1 | **Single responsibility** | Each agent owns one domain; orchestrator only coordinates |
| 2 | **Stateless workers** | No shared memory; orchestrator front-loads all context in briefs |
| 3 | **Version-controlled state** | ORCHESTRATION.md tracks all progress in Git |
| 4 | **Human-in-the-loop** | Orchestrator reports; human approves or rejects every change |
| 5 | **Dependency enforcement** | Workers can't start until dependencies are ✅ |
| 6 | **Validation gates** | Build + test + security review must pass before marking done |
| 7 | **Minimal tool access** | Workers get only the tools they need (no `agent` tool) |
| 8 | **Security-first** | Every detection implementation gets dedicated security review |
| 9 | **Platform isolation** | Android agent cannot modify iOS code and vice versa |
| 10 | **Human-driven security validation** | Human review required for all security-critical code (Claude Opus 4.6 not available in session) |
| 11 | **Auditable trail** | Every task tracked in ORCHESTRATION.md for PCI DSS compliance |
| 12 | **Parallel when safe** | Independent modules executed in parallel; dependent work sequential |

---

## 7. Mapping to OpenGuard SDLC

| SDLC Phase | Wave | Agents Involved | Execution Mode |
|------------|------|-----------------|----------------|
| Research | Wave 0 | @research | Sequential in web session (parallelizable with `/fleet` CLI) |
| Infrastructure | Wave 0 | @devops | Sequential (build → CI) |
| API Design | Wave 1 | @kmp-core | Sequential (interfaces → config → engine) |
| Android Implementation | Wave 2 | @android, @security-review, @qa | Sequential per task in web session |
| iOS Implementation | Wave 2 | @ios, @security-review, @qa | Sequential per task in web session |
| Network Security | Wave 3 | @kmp-core, @android, @ios, @security-review | Sequential (API → impl → review) |
| Crypto & Storage | Wave 4 | @android, @ios, @security-review, @qa | Sequential (storage → crypto → review) |
| Platform Attestation | Wave 5 | @research, @android, @ios, @security-review | Research → implementation → review |
| Documentation | Wave 6 | @docs | Sequential in web session |
| Distribution | Wave 6 | @devops | Sequential (publishing → automation) |

---

## 8. Estimated Execution

| Wave | Tasks | Can Parallelize | Bottleneck |
|------|-------|----------------|------------|
| Wave 0 | 6 | 5 (all research + OPS-001 parallel) | OPS-002 depends on OPS-001 |
| Wave 1 | 6 | 4 (KMP-001..003 parallel) | KMP-004, KMP-005 depend on KMP-001 |
| Wave 2 | 14 | 10 (AND-001..005 ∥ IOS-001..005) | Security reviews after implementation |
| Wave 3 | 5 | 2 (AND-006 ∥ IOS-006) | KMP-006 must come first |
| Wave 4 | 6 | 2 (AND-007 ∥ IOS-007 parallel start) | Crypto depends on storage |
| Wave 5 | 4 | 2 (AND-009 ∥ IOS-009) | Research must complete first |
| Wave 6 | 5 | 3 (all docs parallel) | OPS-004 depends on OPS-003 |

**Total tasks:** ~46  
**Maximum parallelism per wave:** 10 (Wave 2 — all detection implementations)  
**Critical path:** Wave 0 → Wave 1 → Wave 2 → Wave 3 → Wave 4 → Wave 6

---

## 9. Quick Reference

| Action | How |
|--------|-----|
| Start pipeline | `/run-pipeline` or `@orchestrator` in Copilot Chat |
| Run parallel tasks | `/fleet <task list>` in Copilot CLI (not available in web session) |
| Add a task | Edit `ORCHESTRATION.md` |
| Add a new agent role | Create `.agent.md` + update orchestrator's `agents:` list |
| Check progress | Read `ORCHESTRATION.md` |
| Security review | `@security-review` or auto-delegated by orchestrator |
| Cross-model validation | Agent HQ on GitHub.com (not available in coding agent session) |
| Monitor parallel agents | `/tasks` in Copilot CLI (not available in web session) |
| Override auto-approve | Set `chat.tools.autoApprove: false` in VS Code settings |

---

## 10. Limitations & Mitigations

| Limitation | Mitigation |
|-----------|------------|
| **Claude Opus 4.6 not available in Copilot session** | All workflows designed Copilot-only; Claude Opus 4.6 is optional via Agent HQ (separate workflow) |
| No true parallelism in web/cloud session | Sequential execution within session; use `/fleet` CLI locally for parallel work |
| Context window limits | Break tasks into small units; keep briefs under 4K tokens |
| No persistent inter-agent memory | Orchestrator relays context; ORCHESTRATION.md is shared state |
| Single session scope | Progress persists in ORCHESTRATION.md; re-invoke orchestrator |
| Agent hallucinations in security code | Dedicated `@security-review` agent + mandatory human review + comprehensive tests |
| KMP compile errors from context gaps | Include full `expect` interface in every brief |
| iOS build verification requires macOS | CI runs on macOS runner; local verification on Linux limited |

---

## 11. Next Steps

1. **Immediate:** Create all `.github/agents/*.agent.md` files per this specification
2. **Immediate:** Create `ORCHESTRATION.md` with Wave 0-6 task board
3. **Immediate:** Create `AGENTS.md` with project-wide instructions
4. **Immediate:** Create `.github/prompts/run-pipeline.prompt.md`
5. **Week 1:** Execute Wave 0 (research + build system)
6. **Week 2:** Execute Wave 1 (core API design)
7. **Week 3-4:** Execute Wave 2 (all detection implementations in parallel)
8. **Week 5:** Execute Wave 3-4 (network + crypto)
9. **Week 6:** Execute Wave 5-6 (attestation + docs + distribution)
