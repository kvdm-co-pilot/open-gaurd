# OpenGuard SDK Distribution Strategy

**Document Version:** 1.0  
**Date:** 2026-04-19

---

## 1. Distribution Overview

OpenGuard is distributed as a multi-platform SDK targeting:
1. **Android** — via Maven Central (AAR/JAR)
2. **iOS** — via Swift Package Manager (XCFramework) and CocoaPods
3. **Kotlin Multiplatform** — via Maven Central (KMP metadata + platform artifacts)

---

## 2. Android Distribution

### 2.1 Maven Central Publication

**Group ID:** `com.openguard`

| Artifact | Description | Platform |
|----------|-------------|----------|
| `openguard-core` | KMP shared module | Android + iOS |
| `openguard-android` | Android-specific extras | Android |

**Gradle Dependency:**
```kotlin
// settings.gradle.kts — ensure Maven Central is added
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

// build.gradle.kts (app module)
dependencies {
    // For pure Android apps
    implementation("com.openguard:openguard-core:1.0.0")
    
    // For Android apps with extra native hardening
    implementation("com.openguard:openguard-android:1.0.0")
}
```

### 2.2 Android Build Requirements
- **Minimum SDK:** API 21 (Android 5.0 Lollipop)
- **Target SDK:** Latest stable
- **Compile SDK:** Latest stable
- **ProGuard/R8:** Rules bundled in AAR (consumer rules)
- **ABI support:** `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

### 2.3 ProGuard Consumer Rules

The SDK AAR bundles consumer ProGuard rules that prevent R8 from stripping SDK classes:
```proguard
# openguard-consumer-rules.pro (bundled in AAR)
-keep class com.openguard.** { *; }
-keepclassmembers class com.openguard.** { *; }
-dontwarn com.openguard.**
```

---

## 3. iOS Distribution

### 3.1 Swift Package Manager (Primary)

**Repository:** `https://github.com/openguard/openguard-spm`

```swift
// Package.swift (consumer app)
dependencies: [
    .package(
        url: "https://github.com/openguard/openguard-spm.git",
        from: "1.0.0"
    )
],
targets: [
    .target(
        name: "YourApp",
        dependencies: [
            .product(name: "OpenGuard", package: "openguard-spm")
        ]
    )
]
```

**Xcode Integration:**
1. File → Add Package Dependencies
2. Enter: `https://github.com/openguard/openguard-spm`
3. Select version rule: `Up to Next Major Version` from `1.0.0`

### 3.2 CocoaPods (Legacy Support)

```ruby
# Podfile
platform :ios, '14.0'

target 'YourApp' do
  use_frameworks!
  pod 'OpenGuard', '~> 1.0'
end
```

### 3.3 XCFramework Structure

The iOS distribution is a pre-built XCFramework supporting:
```
OpenGuard.xcframework/
├── ios-arm64/                    # Device (64-bit ARM)
│   └── OpenGuard.framework/
├── ios-arm64-simulator/          # Apple Silicon simulator
│   └── OpenGuard.framework/
└── ios-x86_64-simulator/         # Intel simulator (legacy)
    └── OpenGuard.framework/
```

### 3.4 iOS Build Requirements
- **Minimum iOS:** 14.0
- **Swift Version:** 5.9+
- **Xcode:** 15.0+
- **Privacy Manifest:** `PrivacyInfo.xcprivacy` bundled (required for App Store submission)

---

## 4. Kotlin Multiplatform Distribution

### 4.1 KMP Module Dependencies

```kotlin
// shared/build.gradle.kts (KMP module in consumer app)
kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation("com.openguard:openguard-core:1.0.0")
        }
        androidMain.dependencies {
            // Android-specific extras if needed
        }
        iosMain.dependencies {
            // iOS-specific extras if needed  
        }
    }
}
```

### 4.2 Published Artifacts

Maven Central publications per KMP target:
```
com.openguard:openguard-core:1.0.0                           # BOM/metadata
com.openguard:openguard-core-android:1.0.0                   # Android target
com.openguard:openguard-core-iosarm64:1.0.0                  # iOS ARM64
com.openguard:openguard-core-iosx64:1.0.0                    # iOS x86_64
com.openguard:openguard-core-iossimulatorarm64:1.0.0         # iOS Sim ARM64
```

---

## 5. SDK Initialization per Platform

### 5.1 Android

```kotlin
// MyApplication.kt
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        OpenGuard.initialize(
            context = this,
            config = OpenGuardConfig {
                detection {
                    rootJailbreakDetection { enabled = true }
                    debuggerDetection { enabled = true }
                    hookDetection { enabled = true }
                    tamperDetection {
                        enabled = true
                        expectedSignatureHash = BuildConfig.EXPECTED_SIGNING_CERT_HASH
                    }
                }
                network {
                    certificatePinning {
                        pin("api.yourapp.com") {
                            addSha256Pin(BuildConfig.CERT_PIN_PRIMARY)
                            addSha256Pin(BuildConfig.CERT_PIN_BACKUP)
                        }
                    }
                }
            }
        )
    }
}
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:name=".MyApplication"
    android:allowBackup="false"
    android:debuggable="false"
    ... />
```

### 5.2 iOS (Swift)

```swift
// AppDelegate.swift (or App.swift for SwiftUI)
import OpenGuard

@main
struct YourApp: App {
    init() {
        OpenGuard.initialize(
            config: OpenGuardConfig.Builder()
                .detection { detection in
                    detection.jailbreakDetection(enabled: true, reaction: .blockAndReport)
                    detection.debuggerDetection(enabled: true, reaction: .blockAndReport)
                    detection.hookDetection(enabled: true, reaction: .blockAndReport)
                }
                .network { network in
                    network.certificatePinning { pinning in
                        pinning.pin(host: "api.yourapp.com") { pin in
                            pin.addSha256Pin("AAAA...AAAA=")
                            pin.addSha256Pin("BBBB...BBBB=") // backup
                        }
                    }
                }
                .build()
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### 5.3 Kotlin Multiplatform (Shared Code)

```kotlin
// shared/src/commonMain/kotlin/Platform.kt
expect fun initializeOpenGuard()

// shared/src/androidMain/kotlin/Platform.kt
actual fun initializeOpenGuard() {
    // Android-specific init (context comes from AndroidMain)
}

// shared/src/iosMain/kotlin/Platform.kt
actual fun initializeOpenGuard() {
    OpenGuard.initialize(
        config = OpenGuardConfig {
            detection {
                rootJailbreakDetection { enabled = true }
            }
        }
    )
}
```

---

## 6. CI/CD Pipeline for SDK Release

```yaml
# .github/workflows/release.yml (planned)
name: Release OpenGuard SDK

on:
  push:
    tags:
      - 'v*'

jobs:
  release-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Android artifacts
        run: ./gradlew :openguard-core:publishToMavenCentral
      
  release-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build XCFramework
        run: ./gradlew :openguard-core:assembleXCFramework
      - name: Create Swift Package
        run: ./scripts/create-spm-package.sh
      - name: Publish to SPM repo
        run: git push openguard-spm --tags
```

---

## 7. Versioning Policy

| Version Component | Meaning | Example |
|-------------------|---------|---------|
| MAJOR | Breaking API changes | 2.0.0 |
| MINOR | New features, backward compatible | 1.1.0 |
| PATCH | Bug fixes, security patches | 1.0.1 |

**Security Patch SLA:**
- Critical CVE: Patch released within 72 hours
- High CVE: Patch released within 7 days
- Medium CVE: Patch released within 30 days

**Pin Rotation Support:**
- SDK supports runtime pin update via remote config
- Migration guide published with each major version
- 6-month deprecation notice for old pins

---

## 8. Integration Guide Summary

### Minimum Integration Checklist

**Android:**
- [ ] Add Maven Central dependency
- [ ] Initialize in `Application.onCreate()`
- [ ] Set `android:allowBackup="false"`
- [ ] Set `android:debuggable="false"` (or use build type)
- [ ] Configure certificate pins for API endpoints
- [ ] Enable ProGuard/R8 in release build
- [ ] Set `FLAG_SECURE` on payment screens

**iOS:**
- [ ] Add Swift Package dependency
- [ ] Initialize in `AppDelegate`/App entry point
- [ ] Configure `NSAppTransportSecurity` in Info.plist
- [ ] Enable certificate pinning for API endpoints
- [ ] Use `UITextField.isSecureTextEntry` for sensitive fields
- [ ] Handle `UIScreen.isCaptured` for screen recording
- [ ] Enable bitcode (if required by target iOS version)

**Both Platforms:**
- [ ] Configure threat response callbacks
- [ ] Set up remote audit log endpoint
- [ ] Test RASP detections in pre-production
- [ ] Run pen test suite before production release
- [ ] Document all certificate pins and their rotation schedule
