import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Load secrets from local.properties (NOT committed). See local.properties.example.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
val missingSecrets = mutableListOf<String>()
fun secret(key: String, default: String = "", required: Boolean = true): String {
    val raw = localProps.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        ?: default
    if (required && raw.isBlank()) missingSecrets += key
    // Escape for safe embedding inside a Kotlin/Java string literal.
    return raw.replace("\\", "\\\\").replace("\"", "\\\"")
}

android {
    namespace = "com.voice0.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.voice0.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // ⚠ These are baked into the APK and extractable. See README "Security".
        buildConfigField("String", "GROQ_API_KEY", "\"${secret("GROQ_API_KEY")}\"")
        buildConfigField("String", "ELEVENLABS_API_KEY", "\"${secret("ELEVENLABS_API_KEY")}\"")
        buildConfigField("String", "HELIUS_RPC_URL", "\"${secret("HELIUS_RPC_URL")}\"")
        buildConfigField("String", "HELIUS_DEVNET_RPC_URL", "\"${secret("HELIUS_DEVNET_RPC_URL")}\"")
        buildConfigField("String", "GROQ_MODEL", "\"${secret("GROQ_MODEL", "llama-3.3-70b-versatile", required = false)}\"")

        if (missingSecrets.isNotEmpty()) {
            logger.warn("⚠ voice0: missing keys in local.properties: ${missingSecrets.joinToString(", ")}")
            logger.warn("  → fill them in, then run:  ./gradlew --rerun-tasks assembleDebug")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Core / lifecycle / activity
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coroutines / serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Persistence
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Networking
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Solana Mobile Stack
    implementation(libs.solana.mwa)
    implementation(libs.solana.web3)
    implementation(libs.solana.rpc.core)
    implementation(libs.multimult)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
