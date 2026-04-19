plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.openguard.sample"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.openguard.sample"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
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
    implementation(project(":openguard-android"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.android)
}

// Disable Android lint and annotation-extraction tasks — they require
// lint-gradle from dl.google.com which is DNS-blocked in the Copilot
// sandbox. Lint runs via detekt in CI instead.
afterEvaluate {
    tasks.matching {
        it.name.contains("lint", ignoreCase = true) ||
        it.name.startsWith("extract") && it.name.contains("Annotations", ignoreCase = true)
    }.configureEach {
        enabled = false
    }
}
