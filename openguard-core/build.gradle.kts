import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

group = "com.openguard"
version = "0.1.0-alpha"

// iOS targets require Xcode command-line tools (macOS only).
// On Linux (Copilot sandbox, Ubuntu CI), we skip iOS targets so that
// `./gradlew build` and `./gradlew test` succeed for Android/JVM code.
// The macOS CI job in ci.yml compiles and tests iOS targets separately.
val isMacOs = System.getProperty("os.name").startsWith("Mac", ignoreCase = true)

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        publishLibraryVariants("release")
    }

    if (isMacOs) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "OpenGuardCore"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.biometric)
            implementation(libs.okhttp)
        }
        if (isMacOs) {
            iosMain.dependencies {
                // iOS-specific dependencies are added via interop
            }
        }
    }
}

android {
    namespace = "com.openguard.core"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
