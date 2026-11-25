import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    id("kotlin-kapt")
}

// CLIENT_ID : variable d'environnement d'abord, local.properties sinon. Jamais commité.
// Il n'y a pas de CLIENT_SECRET : le SDK utilise PKCE.
val spotifyClientId: String =
    System.getenv("SPOTIFY_CLIENT_ID")
        ?: Properties()
            .apply {
                rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
            }.getProperty("CLIENT_ID")
        ?: ""

android {
    namespace = "org.vander.android.vinylify"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.vander.android.vinylify"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$spotifyClientId\"")
        buildConfigField("String", "SPOTIFY_REDIRECT_URI", "\"org-vander-androidapp://callback\"")

        // Doivent correspondre à SPOTIFY_REDIRECT_URI et à une URI déclarée sur le dashboard
        // Spotify : un intent-filter est figé à la compilation.
        manifestPlaceholders["spotifyRedirectScheme"] = "org-vander-androidapp"
        manifestPlaceholders["spotifyRedirectHost"] = "callback"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging { resources.excludes += "META-INF/**" }

    testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.spotify.android)
    implementation(libs.spotify.android.hilt)

    // `compileOnly` dans la lib : c'est l'application qui embarque l'AAR de l'App Remote.
    // Le SDK d'autorisation n'est plus nécessaire, le flot PKCE est celui de la lib.
    implementation(files("libs/spotify-app-remote-release-0.8.0.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tink.android)
    implementation(libs.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.text.google.fonts)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.ext)
    implementation(libs.material)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    kapt(libs.androidx.hilt.compiler)
    kapt(libs.kotlin.metadata.jvm)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.spotify.testing)
    testImplementation(libs.spotify.android.testing)
}
