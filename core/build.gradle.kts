import java.util.Properties

val secretsProperties = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use { secretsProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.androidx.room)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.vci.vectorcamapp.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://test.api.vectorcam.org/\"")
            buildConfigField("String", "VECTORCAM_API_KEY", "\"${secretsProperties["DEBUG_VECTORCAM_API_KEY"]}\"")
        }

        release {
            buildConfigField("String", "BASE_URL", "\"https://api.vectorcam.org/\"")
            buildConfigField("String", "VECTORCAM_API_KEY", "\"${secretsProperties["RELEASE_VECTORCAM_API_KEY"]}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text.google.fonts)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Ktor (networking)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // DataStore
    implementation(libs.androidx.datastore)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // TUS upload
    implementation(libs.tus.android.client)
    implementation(libs.tus.java.client)

    // Location
    implementation(libs.play.services.location)

    // Coil (image loading used by core presentation)
    implementation(libs.coil.compose)

    // Window
    implementation(libs.androidx.window)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
}
