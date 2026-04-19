# Copilot Cloud Agent Infrastructure — Reusable Guide

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Status:** Reference Architecture  
**Classification:** Public / Reusable

---

## 1. Purpose

This document captures the infrastructure architecture and philosophy for running AI-agent-driven development using **GitHub Copilot's cloud agent** (coding agent). It is designed as a reusable blueprint — any Kotlin Multiplatform, Android, or iOS project can adopt this pattern by copying the relevant configuration files.

The architecture solves three hard problems:
1. **DNS firewall bypass** — The Copilot sandbox blocks `dl.google.com`, breaking Android builds
2. **Cross-platform compilation** — iOS targets require macOS, but the sandbox is Ubuntu
3. **Agent tool access** — MCP servers give the AI agent interactive device testing

---

## 2. Core Constraint: The Copilot Sandbox Firewall

### 2.1 What Happens

When GitHub's Copilot cloud agent starts a session, it runs on an **Ubuntu-based runner** behind a **DNS-level firewall**. This firewall blocks certain domains to prevent data exfiltration. Critically:

| Domain | Status | Impact |
|--------|--------|--------|
| `dl.google.com` | ❌ **BLOCKED** | Hosts all Android dependencies (AGP, AndroidX, Google Play Services) |
| `maven.google.com` | ⚠️ Resolves but 301→dl.google.com | Gradle's `google()` repo shorthand — unusable |
| `repo1.maven.org` | ✅ Allowed | Maven Central (Kotlin, coroutines, OkHttp) |
| `services.gradle.org` | ✅ Allowed | Gradle wrapper distribution |
| `plugins.gradle.org` | ✅ Allowed | Gradle Plugin Portal |

### 2.2 Why This Matters

Gradle's `google()` repository shorthand resolves to `https://dl.google.com/dl/android/maven2/`. Every Android project depends on this for:
- **Android Gradle Plugin** (`com.android.tools.build:gradle`)
- **AndroidX libraries** (`androidx.*`)
- **Google Play Services** (`com.google.android.gms.*`)

Without these, `./gradlew build` fails immediately during plugin resolution.

### 2.3 The Solution: Pre-Cache in Setup Steps

GitHub documents that the firewall **does not apply** to processes started during `copilot-setup-steps.yml`:

> *"The firewall only applies to processes started by the agent via its Bash tool. It does not apply to [...] processes started in configured Copilot setup steps."*

— [GitHub Docs: Customizing the development environment](https://docs.github.com/en/copilot/customizing-copilot/customizing-the-development-environment-for-copilot-coding-agent)

This means: if we resolve ALL Gradle dependencies during setup steps, they're cached in `~/.gradle/caches` and available when the agent runs builds. The agent never needs to hit `dl.google.com`.

### 2.4 Redundancy: Custom Firewall Allowlist

As a backup, add `dl.google.com` to the custom allowlist:

1. Go to **Repo Settings → Copilot → Cloud Agent**
2. Click **Custom allowlist**
3. Add `dl.google.com`
4. Save

This provides redundancy — even if the cache misses a new dependency, the agent can download it directly.

---

## 3. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                    GitHub Repository                                │
│                                                                     │
│  .github/                                                           │
│  ├── workflows/                                                     │
│  │   ├── copilot-setup-steps.yml  ← Pre-caches deps (no firewall) │
│  │   └── ci.yml                   ← Ubuntu + macOS CI pipeline     │
│  ├── agents/                      ← Agent role definitions          │
│  │   ├── orchestrator.agent.md                                      │
│  │   ├── android.agent.md                                           │
│  │   ├── ios.agent.md                                               │
│  │   └── ...                                                        │
│  └── prompts/                     ← Reusable prompt templates       │
│      └── run-pipeline.prompt.md                                     │
│                                                                     │
│  .copilot/                                                          │
│  └── mcp.json                     ← MCP server registration         │
│                                                                     │
│  ORCHESTRATION.md                 ← Version-controlled task board    │
│  AGENTS.md                        ← Project-wide agent instructions  │
│                                                                     │
│  gradle/wrapper/                                                    │
│  ├── gradle-wrapper.jar           ← MUST be committed               │
│  └── gradle-wrapper.properties                                      │
│  gradlew                          ← MUST be committed               │
│  gradlew.bat                      ← MUST be committed               │
└─────────────────────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Copilot Cloud Agent Session (Ubuntu)                   │
│                                                                     │
│  Phase 1: Setup Steps (FULL INTERNET ACCESS)                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ • Install JDK 17, Node.js 20, Android SDK                   │   │
│  │ • Install Appium + MCP servers                               │   │
│  │ • Create AVD (openguard_test)                                │   │
│  │ • ./gradlew buildEnvironment (resolve plugins from          │   │
│  │   dl.google.com → cached in ~/.gradle/caches)               │   │
│  │ • ./gradlew :module:dependencies (resolve all artifacts)     │   │
│  │ • ./gradlew compileKotlinAndroid (warm toolchain)            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                          │                                          │
│                          ▼                                          │
│  Phase 2: Agent Runs (FIREWALL ACTIVE — dl.google.com blocked)     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ • Agent reads ORCHESTRATION.md                               │   │
│  │ • Agent writes code, edits files                             │   │
│  │ • ./gradlew build ← resolves from ~/.gradle/caches ✅       │   │
│  │ • ./gradlew test  ← resolves from ~/.gradle/caches ✅       │   │
│  │ • emulator -avd openguard_test & ← AVD pre-created ✅       │   │
│  │ • appium & ← pre-installed ✅                                │   │
│  │ • MCP tools (wdio-mcp) ← pre-installed, via .copilot/mcp   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│              CI Pipeline (Full Internet Access)                      │
│                                                                     │
│  Job 1: build-and-test (ubuntu-latest)                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ • ./gradlew build  (Android/JVM — iOS targets OS-gated)     │   │
│  │ • ./gradlew test                                             │   │
│  │ • ./gradlew detekt                                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Job 2: ios-build-and-test (macos-15) — needs Job 1                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ • ./gradlew build  (all targets including iOS)               │   │
│  │ • ./gradlew iosSimulatorArm64Test                            │   │
│  │ • ./gradlew linkDebugFrameworkIosSimulatorArm64              │   │
│  │ • xcodebuild test (when sample/iosApp exists)                │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 4. File-by-File Reference

### 4.1 `gradlew` + `gradlew.bat` + `gradle/wrapper/gradle-wrapper.jar`

**CRITICAL:** These files MUST be committed to the repository. Without them, no Gradle command works. Many project templates include them, but they can be missing if `.gitignore` is overly aggressive.

Generate them with:
```bash
gradle wrapper --gradle-version 8.7
```

### 4.2 `.github/workflows/copilot-setup-steps.yml`

This is the most critical infrastructure file. It runs **before** the sandbox firewall activates and must:

1. Install JDK, Node.js, Android SDK
2. Install Appium + MCP servers (if needed)
3. Create Android emulator AVD (if needed)
4. **Resolve ALL Gradle dependencies** — this is the firewall bypass

Key pattern for Gradle pre-caching:
```yaml
- name: Pre-cache ALL Gradle dependencies
  run: |
    chmod +x gradlew
    # 1. Plugin/buildscript dependencies (AGP, Kotlin plugin)
    ./gradlew buildEnvironment --no-daemon
    # 2. Module dependencies (AndroidX, OkHttp, etc.)
    ./gradlew :module1:dependencies :module2:dependencies --no-daemon
    # 3. Warm up compilation toolchains
    ./gradlew :module1:compileKotlinAndroid --no-daemon || true
```

### 4.3 `openguard-core/build.gradle.kts` (OS-Gating Pattern)

iOS targets require Xcode (macOS only). On Linux (Copilot sandbox, Ubuntu CI), we skip them:

```kotlin
val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

kotlin {
    androidTarget { ... }

    if (isMacOs) {
        listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { ... }
    }

    sourceSets {
        commonMain.dependencies { ... }
        androidMain.dependencies { ... }
        if (isMacOs) {
            iosMain.dependencies { ... }
        }
    }
}
```

This ensures `./gradlew build` succeeds on both Ubuntu and macOS, with iOS targets only compiled when Xcode is available.

### 4.4 `.copilot/mcp.json`

Registers MCP servers that Copilot automatically discovers. These give the agent interactive access to running devices:

```json
{
  "mcpServers": {
    "wdio-mcp": {
      "command": "npx",
      "args": ["-y", "@wdio/mcp@latest"]
    }
  }
}
```

### 4.5 `.github/workflows/ci.yml`

Two-job pipeline:
- **Job 1 (ubuntu-latest):** Android/JVM build + test (fast, free)
- **Job 2 (macos-15):** iOS build + test (10x billing multiplier, gated on Job 1)

Concurrency group with `cancel-in-progress: true` prevents wasted macOS minutes.

### 4.6 `ORCHESTRATION.md`

Version-controlled task board. The agent reads this to determine what to work on next. Key properties:
- Wave-based ordering (dependencies are explicit)
- Status symbols (✅, 🔄, ☐, 🔒, ⚠️)
- Delegation map (task ID prefix → agent)
- Persists across sessions (committed to Git)

### 4.7 `.github/agents/*.agent.md`

Agent role definitions. Each file defines:
- Scope (which files the agent can modify)
- Constraints (what it must never do)
- ID prefix (for task routing)
- Tools available

---

## 5. Replication Guide: Adding This to a New Project

### Step 1: Ensure Gradle Wrapper is Committed

```bash
# If gradlew is missing:
gradle wrapper --gradle-version 8.7
git add gradlew gradlew.bat gradle/wrapper/
git commit -m "Add Gradle wrapper"
```

### Step 2: Create `copilot-setup-steps.yml`

Copy the template and customize:
- Replace module names in `./gradlew :module:dependencies`
- Add any project-specific tools (Appium, MCP servers, etc.)
- Add Android SDK/emulator setup if needed

### Step 3: Add OS-Gating for iOS Targets

If your project uses Kotlin Multiplatform with iOS targets, add the `isMacOs` guard in `build.gradle.kts`.

### Step 4: Create CI Pipeline

Two-job pipeline: Ubuntu (Android/JVM) → macOS (iOS). Always gate macOS on Ubuntu passing first.

### Step 5: Add Firewall Allowlist (Redundancy)

In GitHub repo settings → Copilot → Cloud Agent → Custom allowlist:
- Add `dl.google.com`

### Step 6: Create Task Board (Optional)

If using multi-agent orchestration, create `ORCHESTRATION.md` with wave-based task tables.

---

## 6. Sandbox Capabilities Matrix

What the Copilot cloud agent sandbox can and cannot do:

| Capability | Sandbox (Ubuntu) | CI (Ubuntu) | CI (macOS) |
|-----------|-------------------|-------------|------------|
| Java/Kotlin compilation | ✅ | ✅ | ✅ |
| Android library builds | ✅ (with pre-cached deps) | ✅ | ✅ |
| Android APK assembly | ✅ (with pre-cached deps) | ✅ | ✅ |
| iOS framework compilation | ❌ (no Xcode) | ❌ | ✅ |
| iOS simulator tests | ❌ (no Xcode) | ❌ | ✅ |
| Xcode builds | ❌ | ❌ | ✅ |
| Android emulator | ✅ (AVD pre-created, KVM available) | ✅ | ⚠️ |
| Appium automation | ✅ (pre-installed) | ❌ (not configured) | ❌ |
| MCP tools (wdio-mcp) | ✅ (via .copilot/mcp.json) | ❌ | ❌ |
| `./gradlew build` | ✅ (Android/JVM via OS-gating) | ✅ (Android/JVM) | ✅ (all targets) |
| `./gradlew test` | ✅ (JVM + commonTest) | ✅ | ✅ (+ iOS tests) |
| `./gradlew connectedAndroidTest` | ✅ (with running emulator) | ✅ | ⚠️ |
| `./gradlew detekt` | ✅ | ✅ | ✅ |
| Network: dl.google.com | ❌ (DNS blocked) | ✅ | ✅ |
| Network: maven.google.com | ⚠️ (301→dl.google.com) | ✅ | ✅ |
| Network: repo1.maven.org | ✅ | ✅ | ✅ |
| Network: services.gradle.org | ✅ | ✅ | ✅ |
| Network: github.com | ✅ | ✅ | ✅ |
| Git push | ❌ (use report_progress) | ✅ | ✅ |

---

## 7. Common Pitfalls

### 7.1 Missing Gradle Wrapper
**Symptom:** `./gradlew: No such file or directory`  
**Fix:** Commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.

### 7.2 Dependency Not Pre-Cached
**Symptom:** `Could not resolve plugin artifact 'com.android.library...'`  
**Fix:** Ensure `copilot-setup-steps.yml` resolves ALL modules. Add new modules to the dependency resolution step.

### 7.3 iOS Compilation Fails on Ubuntu
**Symptom:** `Xcode command line tools not found` or Kotlin/Native linker errors  
**Fix:** Add OS-gating: `if (System.getProperty("os.name").startsWith("Mac"))` around iOS target configuration.

### 7.4 New Dependency Added But Not Cached
**Symptom:** Build fails after adding a new library  
**Fix:** The agent cannot download from `dl.google.com`. Either:
1. Add `dl.google.com` to the firewall allowlist (one-time repo setting), or
2. Push the change and let CI trigger, which updates the Gradle cache

### 7.5 Emulator Not Booting
**Symptom:** `emulator: command not found` or AVD not created  
**Fix:** Ensure `copilot-setup-steps.yml` installs the system image and creates the AVD.

---

## 8. Design Philosophy

### Infrastructure as Code
Every piece of the agent development environment is defined in version-controlled files. No manual setup is required beyond the one-time firewall allowlist setting.

### Pre-Cache, Don't Proxy
Instead of setting up proxy servers or mirror registries, we pre-cache dependencies during the unrestricted setup phase. This is simpler, more reliable, and doesn't require additional infrastructure.

### OS-Gate, Don't Fail
Cross-platform projects should gracefully skip unsupported targets rather than failing. The CI pipeline validates all targets — the sandbox only needs to validate the targets it can run.

### Layer, Don't Replace
MCP tools (Appium, WebDriverIO) are layered on top of existing infrastructure (emulator, ADB). They add AI-driven testing without replacing the existing unit/integration test pipeline.

### Persist State in Git
The task board (`ORCHESTRATION.md`) is committed to Git, not stored in a database or external service. This ensures agent state survives session boundaries and is auditable.

---

## 9. Files to Copy for a New Project

Minimum files to replicate this infrastructure:

```
.github/
├── workflows/
│   ├── copilot-setup-steps.yml   ← Customize module names
│   └── ci.yml                    ← Customize build commands
└── agents/
    └── (optional — for multi-agent orchestration)

.copilot/
└── mcp.json                      ← Customize MCP servers

gradlew                           ← Generate with `gradle wrapper`
gradlew.bat
gradle/wrapper/
├── gradle-wrapper.jar
└── gradle-wrapper.properties

ORCHESTRATION.md                   ← Optional — for multi-agent workflows
AGENTS.md                          ← Customize for project
```
