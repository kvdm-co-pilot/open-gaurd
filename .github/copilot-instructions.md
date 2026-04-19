# OpenGuard — Copilot Instructions

## Project Overview

OpenGuard is an open-source Runtime Application Self-Protection (RASP) SDK for Kotlin Multiplatform (Android + iOS), targeting PCI DSS 4.0 compliance for fintech applications. It is a free alternative to DexGuard and iXGuard.

## Tech Stack

- **Language:** Kotlin 2.1.x (Multiplatform)
- **Build:** Gradle 8.x with Kotlin DSL
- **Android:** minSdk 21, targetSdk 35, AGP 8.x
- **iOS:** deployment target 15.0, Xcode 16+
- **Testing:** JUnit 5 (Android/JVM), XCTest (iOS), kotlin.test (common)
- **Dependencies:** kotlinx.coroutines, AndroidX Security, OkHttp

## Commands

| Action | Command |
|--------|---------|
| Build | `./gradlew build` |
| Test | `./gradlew test` |
| Lint | `./gradlew detekt` |
| Android instrumented tests | `./gradlew connectedAndroidTest` |

## Code Style

- Kotlin coding conventions
- No wildcard imports
- Use `suspend` functions for all async operations
- Use `expect`/`actual` for platform-specific code
- All public APIs must have KDoc documentation
- DSL builder pattern for configuration classes
- `@Throws` annotations on public APIs for iOS interop

## Project Structure

```
openguard-core/src/commonMain/  → Shared KMP interfaces and logic
openguard-core/src/androidMain/ → Android actual implementations
openguard-core/src/iosMain/     → iOS actual implementations (Kotlin/Native)
openguard-android/              → Android-specific extensions
sample/androidApp/              → Android sample application
docs/                           → Documentation and research
```

## Agent Workflow

This project uses a multi-agent orchestration workflow. See `ORCHESTRATION.md` for the task board and `.github/agents/` for agent definitions.

- **@orchestrator** coordinates all work — invoke with `@orchestrator` or use the `run-pipeline` prompt
- Worker agents (`@kmp-core`, `@android`, `@ios`, `@qa`, `@security-review`, `@devops`, `@research`, `@docs`) are delegated to by the orchestrator
- All task state is tracked in `ORCHESTRATION.md`
- Security implementations require `@security-review` sign-off

## Boundaries

### Always
- Run tests before reporting completion
- Follow existing code patterns in the codebase
- Use `expect`/`actual` for platform differences
- Route security-critical changes through `@security-review`

### Ask First
- Adding new dependencies
- Changing public API signatures
- Modifying build configuration

### Never
- Commit secrets, API keys, or certificates
- Bypass security review for detection implementations
- Use deprecated Android APIs without providing fallback
- Modify agent definition files without human approval
