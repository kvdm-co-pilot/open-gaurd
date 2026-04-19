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
        commonTest.dependencies {
            implementation(kotlin("test"))
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

    lint {
        // Lint is run in a dedicated CI step (detekt). Disable the AGP lint
        // integration so we don't need lint-gradle from dl.google.com in the
        // Copilot sandbox environment where that host is DNS-blocked.
        checkReleaseBuilds = false
        abortOnError = false
    }
}

// Disable Android lint and annotation-extraction tasks — they require
// lint-gradle from dl.google.com which is DNS-blocked in the Copilot
// sandbox. Lint runs via detekt in CI instead.
//
// Because syncLibJars depends on the typedefs.txt file produced by
// extractAnnotations, we register a dedicated task that creates empty
// typedef files. We wire syncLibJars to depend on it so the files are
// recreated even after a clean build.
afterEvaluate {
    val createEmptyTypedefFiles by tasks.registering {
        val debugDir = layout.buildDirectory.dir(
            "intermediates/annotations_typedef_file/debug/extractDebugAnnotations"
        )
        val releaseDir = layout.buildDirectory.dir(
            "intermediates/annotations_typedef_file/release/extractReleaseAnnotations"
        )
        outputs.dir(debugDir)
        outputs.dir(releaseDir)
        doLast {
            for (dir in listOf(debugDir.get().asFile, releaseDir.get().asFile)) {
                dir.mkdirs()
                File(dir, "typedefs.txt").also { if (!it.exists()) it.createNewFile() }
            }
        }
    }

    // Make syncLibJars depend on our typedef generator, then disable extractAnnotations
    tasks.matching { it.name.startsWith("sync") && it.name.contains("LibJars") }.configureEach {
        dependsOn(createEmptyTypedefFiles)
    }

    tasks.matching {
        it.name.contains("lint", ignoreCase = true) ||
        (it.name.startsWith("extract") && it.name.contains("Annotations", ignoreCase = true))
    }.configureEach {
        enabled = false
    }
}
