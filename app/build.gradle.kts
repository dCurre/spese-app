import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Versione letta dal file VERSION nella root del progetto (es. "0.0.1").
// versionCode numerico derivato da major*10000 + minor*100 + patch.
val appVersionName: String = File(rootDir, "VERSION").readText().trim().ifBlank { "0.0.1" }
val appVersionCode: Int = run {
    val parts = appVersionName.split(".").map { it.toIntOrNull() ?: 0 }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 1 }
    major * 10000 + minor * 100 + patch
}

android {
    namespace = "com.dcapps.spese"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dcapps.spese"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Config backend / Supabase esposta come BuildConfig
        buildConfigField("String", "API_BASE_URL", "\"https://spese-ms.onrender.com/\"")
        buildConfigField("String", "SUPABASE_URL", "\"https://ysucbvilxwaduksccakz.supabase.co\"")
        // Chiave anon pubblica Supabase (non sensibile per definizione).
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlzdWNidmlseHdhZHVrc2NjYWt6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1NDg1MTksImV4cCI6MjA5NTEyNDUxOX0.PbkCHHRUJkEMHT6-qnfbCOHg7eCzBl7BZlyuNdNPeU8\""
        )
        // Web client ID OAuth per Google Sign-In (progetto Firebase spese-dc).
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"587215754394-qkpq6j17tog605f0tthbfmol52ad9fjv.apps.googleusercontent.com\"",
        )
    }

    // Firma release: usata solo se i secret sono presenti nell'ambiente (CI).
    // In locale, senza secret, la release ricade sulla firma debug (vedi sotto).
    val releaseStoreFile = System.getenv("RELEASE_STORE_FILE")
    val hasReleaseSigning = releaseStoreFile != null && File(releaseStoreFile).exists()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = File(releaseStoreFile!!)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
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
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // Fallback locale: firma debug, così `assembleRelease` non fallisce senza keystore.
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // DI - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Immagini
    implementation(libs.coil.compose)

    // Firebase Auth + Google Sign-In
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // Supabase Realtime
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.okhttp)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
