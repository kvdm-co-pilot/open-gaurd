# Appium MCP Integration for OpenGuard

**Document Version:** 1.0  
**Date:** 2026-04-19  
**Status:** Implementation Guide  
**Classification:** Internal / QA Infrastructure

---

## 1. Overview

The orchestrator pipeline already runs an **Android emulator** (`openguard_test` AVD) during QA phases to validate detection implementations on a real device. **Appium + MCP is simply an additional step on top of that existing emulator workflow** — it gives the Copilot coding agent (and human developers in web sessions) direct, conversational access to the running emulator via MCP tools.

Instead of writing separate test scripts, the `@qa` agent can drive the emulator interactively: install the sample APK, tap buttons, read UI state, take screenshots, and validate that security detections fire correctly — all within the same Copilot session that's already running the pipeline.

### What Appium MCP Adds to the Existing Emulator Step

```
Existing pipeline step:          What Appium MCP adds:
─────────────────────────        ─────────────────────────────────
1. Build sample APK              (same)
2. Start emulator                (same)
3. Run unit tests on emulator    (same)
4. ──── NEW ────                 Start Appium server (appium &)
5. ──── NEW ────                 AI agent uses MCP tools to drive the emulator
6. ──── NEW ────                 Conversational E2E validation via screenshots
```

| Without Appium MCP | With Appium MCP (additional step) |
|---|---|
| Write test scripts manually | AI agent drives the device conversationally |
| Separate test framework for E2E | MCP tools available directly in Copilot session |
| Static test cases | Dynamic, exploratory testing by AI |
| Results interpreted manually | Agent reads screenshots and UI state directly |

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────┐
│ GitHub Copilot Coding Agent / Web Session            │
│                                                      │
│  @orchestrator → @qa agent                           │
│       │                                              │
│       ▼                                              │
│  ┌──────────────┐    ┌───────────────────────┐       │
│  │ MCP Client   │───▶│ Appium MCP Server     │       │
│  │ (Copilot)    │    │ (@wdio/mcp or         │       │
│  │              │◀───│  mcp-appium-visual)   │       │
│  └──────────────┘    └───────────┬───────────┘       │
│                                  │                   │
│                      ┌───────────▼───────────┐       │
│                      │ Appium Server         │       │
│                      │ (localhost:4723)       │       │
│                      └───────────┬───────────┘       │
│                                  │                   │
│                      ┌───────────▼───────────┐       │
│                      │ Android Emulator      │       │
│                      │ (openguard_test AVD)   │       │
│                      └───────────────────────┘       │
└─────────────────────────────────────────────────────┘
```

---

## 3. MCP Server Options

### 3.1 @wdio/mcp (WebDriverIO MCP) — Recommended

**Repository:** [webdriverio/mcp](https://github.com/webdriverio/mcp)  
**npm:** `@wdio/mcp`  
**Stars:** 25+ | **Maintained by:** WebDriverIO team

| Feature | Support |
|---|---|
| Android native apps | ✅ via Appium UiAutomator2 |
| iOS native apps | ✅ via Appium XCUITest |
| Browser automation | ✅ Chrome, Firefox, Safari, Edge |
| Touch gestures | ✅ Tap, swipe, long-press, drag |
| App lifecycle | ✅ Launch, background, terminate |
| Screenshots | ✅ |
| Session recording | ✅ Exportable as WebdriverIO JS |
| BrowserStack integration | ✅ Cloud device testing |

**MCP tools provided:**
- `start_session` — Start Android/iOS/browser session
- `navigate` / `get_page_source` — Navigation
- `find_element` / `click_element` / `send_keys` — Interaction
- `take_screenshot` — Visual verification
- `swipe` / `long_press` — Touch gestures
- `app_lifecycle` — Install, launch, terminate apps

**Configuration (`.copilot/mcp.json`):**
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

### 3.2 mcp-appium-visual (Appium-native MCP)

**Repository:** [Rahulec08/appium-mcp](https://github.com/Rahulec08/appium-mcp)  
**npm:** `mcp-appium-visual`  
**Stars:** 62+ | **Unique feature:** Visual element detection with AI

Best for scenarios where the AI agent needs to identify UI elements visually rather than through accessibility IDs/XPath.

### 3.3 mcp-appium-gestures (Gesture-focused)

**Repository:** [AppiumTestDistribution/mcp-appium-gestures](https://github.com/AppiumTestDistribution/mcp-appium-gestures)  
**npm:** `mcp-appium-gestures`  
**Stars:** 31+ | **Unique feature:** Rich gesture vocabulary via Actions API

Best for testing complex touch interactions (multi-finger gestures, custom swipe patterns).

---

## 4. Environment Setup

### 4.1 Copilot Coding Agent (Automated via copilot-setup-steps.yml)

The `copilot-setup-steps.yml` workflow automatically provisions the full environment:

1. **JDK 17** — Required for Android SDK and Gradle
2. **Node.js 20** — Required for Appium and MCP servers
3. **Appium + UiAutomator2 driver** — Mobile automation engine
4. **@wdio/mcp + mcp-appium-visual** — MCP servers (globally installed)
5. **Android SDK + API 34 system image** — Emulator runtime
6. **AVD `openguard_test`** — Pre-created Android Virtual Device
7. **Gradle dependency cache** — Faster builds

The MCP servers are registered in `.copilot/mcp.json` so Copilot automatically discovers them.

### 4.2 Local Development (Manual Setup)

```bash
# Prerequisites: JDK 17+, Node.js 20+, Android SDK

# Install Appium
npm install -g appium
appium driver install uiautomator2

# Install MCP servers
npm install -g @wdio/mcp mcp-appium-visual

# Create AVD (if not exists)
sdkmanager "system-images;android-34;google_apis;x86_64"
echo "no" | avdmanager create avd -n openguard_test \
  -k "system-images;android-34;google_apis;x86_64" --force

# Start emulator
emulator -avd openguard_test -noaudio -no-window &
adb wait-for-device

# Start Appium
appium &

# Now the MCP server can connect to the running Appium instance
```

---

## 5. Usage in the Orchestrator Pipeline

### 5.1 QA Phase Workflow

The pipeline already starts an emulator for unit/instrumented tests. When the `@qa` agent reaches a `QA-E2E-*` task, it simply adds Appium on top of the running emulator:

```
──── Existing pipeline steps (already planned) ────
1. Build sample APK:          ./gradlew :sample:androidApp:assembleDebug
2. Start Android emulator:    emulator -avd openguard_test -noaudio -no-window &
3. Wait for boot:             adb wait-for-device && adb shell getprop sys.boot_completed
4. Run unit/instrumented tests: ./gradlew connectedAndroidTest

──── Additional step: Appium MCP ────
5. Start Appium server:       appium &
6. Use MCP tools to:
   a. start_session({ platform: "android", app: "sample/androidApp/build/..." })
   b. Verify OpenGuard initializes correctly
   c. Trigger detection checks (root, debugger, emulator, tamper)
   d. take_screenshot() to capture results
   e. Validate threat events are dispatched correctly
   f. close_session()
7. Report results to @orchestrator
```

> **Key point:** Steps 1-4 are the same emulator workflow we already planned. Step 5-6 is just `appium &` and then using MCP tools — that's the only addition.

### 5.2 Example MCP Tool Calls (for AI Agent)

The AI agent (Copilot) uses these MCP tools conversationally:

```
// Start a session with the sample app
start_session({
  platform: "android",
  deviceName: "openguard_test",
  app: "./sample/androidApp/build/outputs/apk/debug/androidApp-debug.apk",
  automationName: "UiAutomator2"
})

// Find and tap the "Run Security Check" button
find_element({ strategy: "accessibility id", selector: "run_security_check" })
click_element({ elementId: "..." })

// Take a screenshot to verify results
take_screenshot()

// Check that threat events appear in the UI
find_element({ strategy: "xpath", selector: "//android.widget.TextView[contains(@text, 'Emulator Detected')]" })

// Close the session
close_session()
```

### 5.3 Security Detection Validation Matrix

| Detection | How to Trigger on Emulator | Expected Result |
|---|---|---|
| Emulator detection | Running on AVD (always true) | `ThreatEvent(type=EMULATOR, severity=HIGH)` |
| Root detection | Install Magisk on emulator image | `ThreatEvent(type=ROOT, severity=CRITICAL)` |
| Debugger detection | Attach debugger via ADB | `ThreatEvent(type=DEBUGGER, severity=HIGH)` |
| Tamper detection | Resign APK with different key | `ThreatEvent(type=TAMPER, severity=CRITICAL)` |
| Hook detection | Inject Frida via emulator | `ThreatEvent(type=HOOK, severity=CRITICAL)` |

---

## 6. Integration with Orchestration Tasks

The following tasks are tracked in `ORCHESTRATION.md`:

| ID | Task | Owner | Wave | Depends On |
|---|---|---|---|---|
| QA-E2E-001 | Build sample app APK and run on emulator via Appium MCP | @qa | 2a | QA-002 |
| QA-E2E-002 | Validate all Android detections trigger correctly on emulator | @qa | 2a | QA-E2E-001 |
| QA-E2E-003 | Screenshot-based regression tests for sample app UI | @qa | 6 | QA-E2E-002, DOC-002 |
| IOS-010 | Create sample/iosApp — minimal SwiftUI app importing OpenGuardCore | @ios | 2b | KMP-001 |
| QA-E2E-004 | Build sample iOS app and run on simulator via macOS CI job | @qa | 2b | IOS-010, QA-003 |
| QA-E2E-005 | Validate all iOS detections trigger correctly on simulator | @qa | 2b | QA-E2E-004 |

---

## 7. Limitations & Considerations

### GitHub Copilot Coding Agent Environment
- **Runner:** Ubuntu-based (Linux x86_64) — supports Android emulation with KVM
- **No iOS simulator on Ubuntu:** iOS simulators require macOS. The `ci.yml` macOS job handles iOS simulator testing automatically in CI
- **macOS CI job:** The `ci.yml` workflow includes a `macos-15` job that runs iOS simulator tests automatically — no human needed for Layers 1-4
- **Emulator performance:** KVM acceleration is available on GitHub-hosted runners, but emulator boot takes 60-90 seconds
- **MCP server lifecycle:** MCP servers run as stdio processes managed by Copilot — they start/stop per session

### BrowserStack (Optional — Real-Device Testing Only)
For real-device testing (jailbroken devices, Secure Enclave, App Attest), BrowserStack is available as an optional cloud service. This is **not required** for normal CI — the macOS simulator job covers simulator-based validation. BrowserStack is only needed for testing detections that require real hardware.
For real-device testing (especially iOS), configure BrowserStack credentials:
```json
{
  "mcpServers": {
    "wdio-mcp": {
      "command": "npx",
      "args": ["-y", "@wdio/mcp@latest"],
      "env": {
        "BROWSERSTACK_USERNAME": "${BROWSERSTACK_USERNAME}",
        "BROWSERSTACK_ACCESS_KEY": "${BROWSERSTACK_ACCESS_KEY}"
      }
    }
  }
}
```

### Firewall Considerations
- Appium server runs on `localhost:4723` — no external network access needed
- MCP servers communicate via stdio (no network ports)
- Android emulator uses local ADB — no firewall issues

---

## 8. File Reference

| File | Purpose |
|---|---|
| `.github/workflows/copilot-setup-steps.yml` | Provisions Appium, MCP servers, Android emulator in Copilot agent env |
| `.copilot/mcp.json` | Registers MCP servers for Copilot to discover |
| `sample/androidApp/` | Sample Android app for E2E testing |
| `ORCHESTRATION.md` | Task board — includes QA-E2E-* tasks |
| `docs/appium-mcp-integration.md` | This document |
