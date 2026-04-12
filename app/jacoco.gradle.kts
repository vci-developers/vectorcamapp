/**
 * JaCoCo coverage report configuration.
 *
 * Usage:
 *   # Report for the default (Uganda) flavor:
 *   ./gradlew jacocoUgandaDebugUnitTestReport
 *
 *   # Report for any other flavor:
 *   ./gradlew jacocoGhanaDebugUnitTestReport
 *   ./gradlew jacocoNigeriaDebugUnitTestReport
 *   ./gradlew jacocoKenyaDebugUnitTestReport
 *   ./gradlew jacocoColombiaDebugUnitTestReport
 *
 *   # Aggregate report across ALL flavors (runs tests for every flavor):
 *   ./gradlew jacocoAllFlavorsUnitTestReport
 *
 *   # Check minimum thresholds (fails build if coverage drops below limits):
 *   ./gradlew jacocoUgandaDebugUnitTestCoverageVerification
 *
 * HTML reports land in:  app/build/reports/jacoco/<taskName>/html/index.html
 * XML  reports land in:  app/build/reports/jacoco/<taskName>/<taskName>.xml
 */

apply(plugin = "jacoco")

configure<JacocoPluginExtension> {
    toolVersion = "0.8.12"
}

// ---------------------------------------------------------------------------
// Classes to exclude from coverage measurement
// ---------------------------------------------------------------------------
val coverageExclusions = listOf(
    // Android framework boilerplate
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",

    // Hilt / Dagger generated
    "**/*Hilt*.*",
    "**/Dagger*.*",
    "**/*_Factory.*",
    "**/*_Factory\$*.*",
    "**/*_MembersInjector.*",
    "**/*_HiltModules.*",
    "**/*_HiltModules\$*.*",
    "**/hilt_aggregated_deps/**",

    // Room generated
    "**/*Dao_Impl.*",
    "**/*Dao_Impl\$*.*",
    "**/*Database_Impl.*",

    // KSP / KAPT general output
    "**/generated/**",

    // Sealed-class boilerplate (companion objects, etc.)
    "**/*\$DefaultImpls.*",
    "**/*\$WhenMappings.*",

    // Test classes themselves
    "**/*Test.*",
    "**/*Test\$*.*",
)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
fun String.capitalizeFirst() = replaceFirstChar { it.uppercase() }

fun execFileFor(variantName: String): File {
    val cap = variantName.capitalizeFirst()
    return layout.buildDirectory
        .file("outputs/unit_test_code_coverage/${variantName}UnitTest/test${cap}UnitTest.exec")
        .get().asFile
}

fun classTreeFor(variantName: String) =
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/$variantName")) {
        exclude(coverageExclusions)
    }

val sourceDirs = files("${project.projectDir}/src/main/java")

// ---------------------------------------------------------------------------
// Per-flavor report tasks
// ---------------------------------------------------------------------------
val flavors = listOf("uganda", "ghana", "nigeria", "kenya", "colombia")

flavors.forEach { flavor ->
    val variantName  = "${flavor}Debug"
    val variantCap   = variantName.capitalizeFirst()
    val testTaskName = "test${variantCap}UnitTest"
    val reportTaskName = "jacoco${variantCap}UnitTestReport"
    val verifyTaskName = "jacoco${variantCap}UnitTestCoverageVerification"

    // -- Report ---------------------------------------------------------------
    tasks.register<JacocoReport>(reportTaskName) {
        group       = "Reporting"
        description = "Generate JaCoCo HTML + XML coverage report for $variantName unit tests."

        dependsOn(testTaskName)

        reports {
            html.required.set(true)
            xml.required.set(true)
            csv.required.set(false)
        }

        sourceDirectories.setFrom(sourceDirs)
        classDirectories.setFrom(classTreeFor(variantName))
        executionData.setFrom(files(execFileFor(variantName)))
    }

    // -- Verification (threshold check) ---------------------------------------
    tasks.register<JacocoCoverageVerification>(verifyTaskName) {
        group       = "Verification"
        description = "Verify minimum coverage thresholds for $variantName unit tests."

        dependsOn(reportTaskName)

        sourceDirectories.setFrom(sourceDirs)
        classDirectories.setFrom(classTreeFor(variantName))
        executionData.setFrom(files(execFileFor(variantName)))

        violationRules {
            rule {
                limit {
                    // Minimum LINE coverage across the whole app module
                    counter = "LINE"
                    value   = "COVEREDRATIO"
                    minimum = "0.50".toBigDecimal() // 50% — raise as coverage improves
                }
            }
            rule {
                limit {
                    // Minimum BRANCH coverage across the whole app module
                    counter = "BRANCH"
                    value   = "COVEREDRATIO"
                    minimum = "0.40".toBigDecimal() // 40%
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Aggregate report — merges exec files from every flavor
// ---------------------------------------------------------------------------
tasks.register<JacocoReport>("jacocoAllFlavorsUnitTestReport") {
    group       = "Reporting"
    description = "Generate a merged JaCoCo coverage report for ALL flavor unit tests."

    val allTestTasks = flavors.map { flavor -> "test${(flavor + "Debug").capitalizeFirst()}UnitTest" }
    dependsOn(allTestTasks)

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    sourceDirectories.setFrom(sourceDirs)

    classDirectories.setFrom(
        // Use uganda's classes as the canonical set (all flavors compile the same sources)
        classTreeFor("ugandaDebug")
    )

    // Merge exec data from whichever flavors have already been run
    executionData.setFrom(
        files(flavors.map { execFileFor("${it}Debug") }.filter { it.exists() })
    )
}
