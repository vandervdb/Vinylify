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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Temporaire : SDK 0.3.0-SNAPSHOT publié par `publishToMavenLocal`, à retirer une fois
        // la 0.3.0 sur Maven Central.
        mavenLocal {
            content { includeGroup("io.github.vandervdb") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "VilylTech"

include(":app")
