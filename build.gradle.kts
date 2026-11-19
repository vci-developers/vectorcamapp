// Top-level build file where you can add configuration options common to all sub-projects/modules.
// AGP 9 ships Kotlin 2.2.10. This classpath selects the catalog Kotlin version instead.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.dagger.hilt.android) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    alias(libs.plugins.detekt)
}

dependencies {
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:${libs.versions.detekt.get()}")
}

subprojects {
    apply(plugin = "dev.detekt")

    dependencies {
        "detektPlugins"("dev.detekt:detekt-rules-ktlint-wrapper:${rootProject.libs.versions.detekt.get()}")
    }

    configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig.set(true)
        allRules.set(false)
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        autoCorrect.set(rootProject.findProperty("detekt.autoCorrect")?.toString()?.toBoolean() ?: false)
        baseline.set(rootProject.layout.projectDirectory.file("config/detekt/baseline.xml"))
    }

    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        jvmTarget.set("17")
        baseline.set(rootProject.layout.projectDirectory.file("config/detekt/baseline.xml"))
    }
}
