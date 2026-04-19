# OpenGuard — Agent Instructions

## Project
OpenGuard is an open-source RASP SDK for Kotlin Multiplatform (Android + iOS).
It provides runtime security checks for fintech apps targeting PCI DSS 4.0.

## Tech Stack
- Kotlin 2.1.x (Multiplatform)
- Gradle 8.x with Kotlin DSL
- Android: minSdk 21, targetSdk 35, AGP 8.x
- iOS: deployment target 15.0, Xcode 16+
- Testing: JUnit 5 (Android), XCTest (iOS), kotlin.test (common)

## Commands
- Build: `./gradlew build`
- Test: `./gradlew test`
- Lint: `./gradlew detekt`
- Android tests: `./gradlew connectedAndroidTest`
- Start emulator: `emulator -avd openguard_test -noaudio -no-window &`
- Start Appium: `appium &`

## MCP Servers (AI-Driven Mobile Testing)
- `@wdio/mcp` — WebDriverIO MCP for browser + mobile automation
- `mcp-appium-visual` — Appium-native MCP with visual element detection
- Config: `.copilot/mcp.json`
- Guide: `docs/appium-mcp-integration.md`

## Code Style
- Kotlin coding conventions
- No wildcard imports
- Use `suspend` functions for async operations
- Use `expect`/`actual` for platform code
- All public APIs must have KDoc documentation
- DSL builder pattern for configuration classes

## Boundaries

### Always
- Run tests before reporting completion
- Follow existing code patterns in the codebase
- Use `expect`/`actual` for platform differences
- Route security implementations through `@security-review`

### Ask First
- Adding new dependencies
- Changing public API signatures
- Modifying build configuration

### Never
- Commit secrets, API keys, or certificates
- Modify `.github/agents/` files without human approval
- Bypass security review for detection implementations
- Use deprecated Android APIs without fallback

## Agent Workflow
This project uses multi-agent orchestration. Task state is tracked in `ORCHESTRATION.md`.
Agent definitions are in `.github/agents/`. Start with `@orchestrator` or the `run-pipeline` prompt.
