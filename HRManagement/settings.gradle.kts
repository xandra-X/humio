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

    // Centralized plugin versions
    plugins {
        id("com.android.application") version "8.2.2" apply false
        id("com.android.library") version "8.2.2" apply false

        id("org.jetbrains.kotlin.android") version "1.9.10" apply false
        id("org.jetbrains.kotlin.jvm") version "1.9.10" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10" apply false

        // 🔥 REQUIRED FOR FIREBASE
        id("com.google.gms.google-services") version "4.4.0" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HRManagement"
include(":app")
include(":server")