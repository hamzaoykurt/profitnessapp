import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.androidx.baselineprofile)
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
    localProperties.load(it)
}

fun secret(name: String, defaultValue: String = ""): String =
    localProperties.getProperty(name)
        ?: System.getenv(name)
        ?: defaultValue

// Debug keystore: local development only.
val ksBase64: String = secret("KEYSTORE_BASE64")
val ksPassword: String = secret("KEYSTORE_PASSWORD", "android")
val ksAlias: String = secret("KEY_ALIAS", "androiddebugkey")
val ksKeyPassword: String = secret("KEY_PASSWORD", "android")

val resolvedKeystore: File = if (ksBase64.isNotEmpty()) {
    val bytes = Base64.getDecoder().decode(ksBase64.trim())
    val f = rootProject.file("build/ci_signing.keystore")
    f.parentFile?.mkdirs()
    f.writeBytes(bytes)
    f
} else {
    File(System.getProperty("user.home"), ".android/debug.keystore")
}

val releaseKsBase64: String = secret("RELEASE_KEYSTORE_BASE64")
val releaseKsPassword: String = secret("RELEASE_KEYSTORE_PASSWORD")
val releaseKsAlias: String = secret("RELEASE_KEY_ALIAS")
val releaseKsKeyPassword: String = secret("RELEASE_KEY_PASSWORD")
val releaseKeystore: File? = releaseKsBase64.takeIf { it.isNotBlank() }?.let { encoded ->
    val bytes = Base64.getDecoder().decode(encoded.trim())
    val f = rootProject.file("build/release_signing.keystore")
    f.parentFile?.mkdirs()
    f.writeBytes(bytes)
    f
}
val hasReleaseSigning = releaseKeystore != null &&
    releaseKsPassword.isNotBlank() &&
    releaseKsAlias.isNotBlank() &&
    releaseKsKeyPassword.isNotBlank()

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

        buildConfigField("String", "SUPABASE_URL",      "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "MAPS_API_KEY",      "\"${secret("MAPS_API_KEY")}\"")
        buildConfigField("String", "RESET_PASSWORD_LINK_HOST", "\"${secret("RESET_PASSWORD_LINK_HOST", "cosmibit.com")}\"")
        buildConfigField("String", "RESET_PASSWORD_REDIRECT_URL", "\"${secret("RESET_PASSWORD_REDIRECT_URL", "profitness://reset-password")}\"")
        manifestPlaceholders["resetPasswordLinkHost"] = secret("RESET_PASSWORD_LINK_HOST", "cosmibit.com")
    }

    signingConfigs {
        getByName("debug") {
            storeFile     = resolvedKeystore
            storePassword = ksPassword
            keyAlias      = ksAlias
            keyPassword   = ksKeyPassword
        }
        create("release") {
            if (releaseKeystore != null) {
                storeFile = releaseKeystore
            }
            storePassword = releaseKsPassword
            keyAlias = releaseKsAlias
            keyPassword = releaseKsKeyPassword
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("profile") {
            initWith(buildTypes.getByName("release"))
            isDebuggable = false
            isProfileable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    baselineProfile {
        automaticGenerationDuringBuild = false
        saveInSrc = true
        dexLayoutOptimization = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir}/compose_metrics",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir}/compose_metrics"
        )
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

gradle.taskGraph.whenReady {
    val releaseRequested = allTasks.any { task ->
        task.path in setOf(":app:assembleRelease", ":app:bundleRelease") ||
            task.name in setOf("assembleRelease", "bundleRelease")
    }
    if (releaseRequested && !hasReleaseSigning) {
        throw GradleException(
            "Release signing requires RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD."
        )
    }
}

dependencies {
    // Core Android/Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)

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
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
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

    // Google Places autocomplete
    implementation(libs.google.places)
    implementation(libs.google.material)

    // DataStore (theme persistence)
    implementation(libs.androidx.datastore.preferences)

    // Room (local database — single source of truth)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.gotrue)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)

    // Ktor (Supabase HTTP engine for Android + Gemini JSON content negotiation)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    baselineProfile(project(":baselineprofile"))
}
