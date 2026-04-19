pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Read local.properties to determine if sandbox stubs should be enabled.
// local.properties is gitignored, so this is inert in CI and local dev.
// In the Copilot sandbox, the DevOps agent creates local.properties with
// useSandboxStubs=true to activate the fallback localMaven repository.
val localProps = java.util.Properties()
val localPropsFile = rootDir.resolve("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}
val sandboxStubsEnabled = localProps.getProperty("useSandboxStubs") == "true"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Sandbox stub repository — placed FIRST so it serves AndroidX stubs when
        // dl.google.com is DNS-blocked (Copilot sandbox). In CI and local dev, the
        // real JARs are in the Gradle cache (pre-warmed by setup steps) and Gradle
        // serves those from cache without making network requests, so these stubs
        // are never used in practice for normal CI runs.
        //
        // Gated on useSandboxStubs=true in local.properties (gitignored) so that
        // developers without local.properties don't accidentally use stubs.
        //
        // In CI (copilot-setup-steps.yml pre-warms cache from google() with internet
        // access), the real artifacts are cached and take priority over network calls.
        if (sandboxStubsEnabled) {
            maven {
                name = "localStubs"
                url = uri(rootDir.resolve("localMaven"))
                content {
                    includeGroupByRegex("androidx\\..*")
                }
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "open-guard"

include(":openguard-core")
include(":openguard-android")

// The sample app requires appcompat, lifecycle-runtime-ktx, and other AndroidX
// libraries that are only downloadable from dl.google.com. On Ubuntu CI and the
// Copilot sandbox (where dl.google.com is DNS-blocked), these cannot be resolved.
// The sample app is included in CI builds on macOS (see ci.yml job 2) where the
// full dependency set is available after the Gradle cache is pre-warmed.
//
// To build the sample app locally: ./gradlew -PincludeSampleApp=true :sample:androidApp:build
val includeSampleApp = settings.extra.has("includeSampleApp") && settings.extra["includeSampleApp"] == "true" ||
    System.getProperty("os.name").startsWith("Mac", ignoreCase = true)
if (includeSampleApp) {
    include(":sample:androidApp")
}
