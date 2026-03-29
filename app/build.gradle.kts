import java.util.Properties

val secretsProperties = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use { secretsProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.sentry.android.gradle)
    alias(libs.plugins.google.firebase.crashlytics)
    kotlin("plugin.serialization") version "2.0.21"
}

// Apply Google Services plugin only when google-services.json exists (e.g. local dev).
// CI can build without the file when it is not committed (e.g. in .gitignore).
if (file("${project.projectDir}/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.vci.vectorcamapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vci.vectorcamapp"
        minSdk = 29
        targetSdk = 36

        // Base version - will be overridden by flavors
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.vci.vectorcamapp.HiltTestRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    // Define flavor dimension
    flavorDimensions += "region"

    // Product flavors for different regions
    // NOTE: First flavor listed becomes the default in Android Studio
    productFlavors {
        // Uganda is listed first to be the default flavor
        create("uganda") {
            dimension = "region"
            // applicationIdSuffix = ".uganda"
            versionCode = 2007
            versionName = "1.0.7"
            
            buildConfigField("String", "REGION", "\"uganda\"")
            buildConfigField("String", "REGION_CODE", "\"UG\"")
            buildConfigField("String", "REGION_DISPLAY_NAME", "\"Uganda\"")
            
            resValue("string", "app_name_region", "VectorCam Uganda")
            
            // Set as default flavor (helps with IDE)
            isDefault = true
        }

        create("colombia") {
            dimension = "region"
            applicationIdSuffix = ".colombia"
            versionCode = 1007
            versionName = "1.0.7"

            // Region-specific build config fields
            buildConfigField("String", "REGION", "\"colombia\"")
            buildConfigField("String", "REGION_CODE", "\"CO\"")
            buildConfigField("String", "REGION_DISPLAY_NAME", "\"Colombia\"")
            
            // Custom app name for this region
            resValue("string", "app_name_region", "VectorCam Colombia")
        }

        create("nigeria") {
            dimension = "region"
            applicationIdSuffix = ".nigeria"
            versionCode = 3001
            versionName = "1.0.1"
            
            buildConfigField("String", "REGION", "\"nigeria\"")
            buildConfigField("String", "REGION_CODE", "\"NG\"")
            buildConfigField("String", "REGION_DISPLAY_NAME", "\"Nigeria\"")
            
            resValue("string", "app_name_region", "VectorCam Nigeria")
        }

        create("kenya") {
            dimension = "region"
            applicationIdSuffix = ".kenya"
            versionCode = 4001
            versionName = "1.0.1"
            
            buildConfigField("String", "REGION", "\"kenya\"")
            buildConfigField("String", "REGION_CODE", "\"KE\"")
            buildConfigField("String", "REGION_DISPLAY_NAME", "\"Kenya\"")
            
            resValue("string", "app_name_region", "VectorCam Kenya")
        }

        create("ghana") {
            dimension = "region"
            applicationIdSuffix = ".ghana"
            versionCode = 5001
            versionName = "1.0.1"
            
            buildConfigField("String", "REGION", "\"ghana\"")
            buildConfigField("String", "REGION_CODE", "\"GH\"")
            buildConfigField("String", "REGION_DISPLAY_NAME", "\"Ghana\"")
            
            resValue("string", "app_name_region", "VectorCam Ghana")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            buildConfigField("String", "BASE_URL", "\"https://test.api.vectorcam.org/\"")
            buildConfigField("String", "VECTORCAM_API_KEY", "\"${secretsProperties["DEBUG_VECTORCAM_API_KEY"]}\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )

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
        buildConfig = true
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
    }

}

dependencies {
    // Feature modules — transitively expose :core via api()
    implementation(project(":core"))
    implementation(project(":feature:imaging"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.text.google.fonts)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // OpenCV
    implementation(libs.opencv)

    // Activity + Compose entry point (app-level only)
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.dynamic.features.fragment)
    androidTestImplementation(libs.androidx.navigation.testing)

    // Hilt (app-level wiring + test)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.android.compiler)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // Location
    implementation(libs.play.services.location)

    // Firebase (app-level analytics + crash reporting)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Window testing
    implementation(libs.androidx.window.testing)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

sentry {
    org.set("vectorcam")
    projectName.set("android")
    includeSourceContext.set(true)
    includeProguardMapping.set(true)
    autoUploadProguardMapping.set(true)

    ignoredBuildTypes.set(setOf("debug"))
}
