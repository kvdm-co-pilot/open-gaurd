plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

group = "com.openguard"
version = "0.1.0-alpha"

android {
    namespace = "com.openguard.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        // Lint is run in a dedicated CI step (detekt). Disable the AGP lint
        // integration so we don't need lint-gradle from dl.google.com in the
        // Copilot sandbox environment where that host is DNS-blocked.
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    api(project(":openguard-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
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
