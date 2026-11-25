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
        google()
        mavenCentral()
        // spotify-sdk n'est pas publié : on le consomme depuis `./gradlew publishToMavenLocal`
        // lancé dans /Users/vander/Dev/Code/spotify-sdk.
        mavenLocal {
            content { includeGroup("io.github.vandervdb") }
        }
    }
}

rootProject.name = "VilylTech"

include(":app")
