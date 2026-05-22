@file:Suppress("UnstableApiUsage")

import java.util.Properties

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val localProperties = Properties()
val localPropertiesFile = File(rootProject.projectDir, "local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val githubUser: String? = System.getenv("GITHUB_USER") ?: localProperties.getProperty("github.username")
val githubToken: String? = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("github.token")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-uploads-sdk")
            credentials {
                username = githubUser
                password = githubToken
            }
        }
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/fastpix-android-player")
            credentials {
                username = githubUser
                password = githubToken
            }
        }
    }
}

rootProject.name = "StreamGate"
include(":app")
 