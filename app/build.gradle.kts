import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
    localProperties.load(it)
}

// Keystore: CI'da KEYSTORE_BASE64'ten decode et, lokalde varsayılan debug keystore'u kullan
val ksBase64: String = localProperties.getProperty("KEYSTORE_BASE64", "")
val ksPassword: String = localProperties.getProperty("KEYSTORE_PASSWORD", "android")
val ksAlias: String = localProperties.getProperty("KEY_ALIAS", "androiddebugkey")
val ksKeyPassword: String = localProperties.getProperty("KEY_PASSWORD", "android")

val resolvedKeystore: File = if (ksBase64.isNotEmpty()) {
    val bytes = Base64.getDecoder().decode(ksBase64.trim())
    val f = rootProject.file("build/ci_signing.keystore")
    f.parentFile?.mkdirs()
    f.writeBytes(bytes)
    f
} else {
    File(System.getProperty("user.home"), ".android/debug.keystore")
}

android {
    namespace = "com.avonix.profitness"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.avonix.profitness"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL",      "\"${localProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile     = resolvedKeystore
            storePassword = ksPassword
            keyAlias      = ksAlias
            keyPassword   = ksKeyPassword
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("debug") // debug key ile imzala (şimdilik)
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    // Core Android/Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose UX/UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    // Google Fonts (Space Grotesk)
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // Dependency Injection (Hilt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image loading
    implementation(libs.coil.compose)

    // DataStore (theme persistence)
    implementation(libs.androidx.datastore.preferences)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)

    // Ktor (Supabase HTTP engine for Android)
    implementation(libs.ktor.client.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
