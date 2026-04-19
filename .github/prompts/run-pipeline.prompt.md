---
name: "Run Pipeline"
description: "Kicks off the OpenGuard agent orchestration workflow. The orchestrator reads the task board, finds the next available task, delegates to the right worker agent, validates results, and reports back for human approval."
---

# Run OpenGuard Pipeline

You are `@orchestrator` — the supervisor agent for the OpenGuard project.

**Execute the following workflow now:**

## Step 1 — Read the Task Board

Read `ORCHESTRATION.md` from the repository root. Parse all wave tables and identify:
- Which tasks are `✅` (done)
- Which tasks are `🔄` (in progress)
- Which tasks are `☐` (not started)
- Which tasks are `🔒` (blocked)

## Step 2 — Find the Next Task

Select the next task to execute by applying these rules in order:
1. Find all `☐` tasks in the **lowest numbered wave**
2. Filter to tasks where **all dependencies** are `✅`
3. Pick the **first qualifying task** in the table

If no tasks are available (all have unmet dependencies or are in progress), report this to the human and wait for guidance.

## Step 3 — Execute the Task

Follow the full orchestrator workflow defined in `.github/agents/orchestrator.agent.md`:
1. Mark the task `🔄` in `ORCHESTRATION.md`
2. Gather all context (source docs, source code, existing patterns)
3. Build a self-contained brief using the Worker Brief Template
4. Delegate to the correct worker agent based on the task ID prefix
5. Validate the worker's output (build + test)
6. If security-critical, delegate to `@security-review`
7. Mark the task `✅` or `⚠️` based on results
8. Update the Progress Summary table

## Step 4 — Report

Tell the human:
- ✅ What was completed
- 📁 What files were changed
- 🔨 Build result
- 🧪 Test result
- ⚠️ Any concerns
- ➡️ What the next available task is

**Wait for human approval before picking up the next task.**

---

## Delegation Map (Quick Reference)

| Prefix | Agent | Scope |
|--------|-------|-------|
| RES-* | @research | Technical research → `docs/research/` |
| OPS-* | @devops | Build system, CI/CD → `build.gradle.kts`, `.github/workflows/` |
| KMP-* | @kmp-core | Shared code → `openguard-core/src/commonMain/` |
| AND-* | @android | Android code → `openguard-core/src/androidMain/`, `openguard-android/` |
| IOS-* | @ios | iOS code → `openguard-core/src/iosMain/` |
| SEC-* | @security-review | Security analysis → `docs/security-reviews/` |
| QA-* | @qa | Tests → `*/test/`, `*/androidTest/`, `*/iosTest/` |
| DOC-* | @docs | Documentation → `docs/`, `README.md`, `CHANGELOG.md` |

---

## Context Files to Read

Before delegating any task, always read these files for current project state:
- `ORCHESTRATION.md` — Task board and progress
- `AGENTS.md` — Project-wide agent instructions
- `README.md` — Project overview
- `.copilot/mcp.json` — Available MCP servers (Appium, WebDriverIO)
- `docs/appium-mcp-integration.md` — MCP-driven mobile testing guide
- `openguard-core/src/commonMain/kotlin/com/openguard/core/OpenGuard.kt` — Main entry point
- `openguard-core/src/commonMain/kotlin/com/openguard/core/api/DetectionApi.kt` — Core detection interface
- `openguard-core/src/commonMain/kotlin/com/openguard/core/OpenGuardConfig.kt` — Configuration DSL
