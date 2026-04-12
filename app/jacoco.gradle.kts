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
// Only measure coverage for classes we actually write tests for.
// Everything else (UI, DI, data binding, generated code, etc.) is excluded.
// ---------------------------------------------------------------------------

/** Patterns that MUST match for a class to be included in the report. */
val coverageIncludes = listOf(
    // ViewModels
    "**/*ViewModel.class",
    "**/*ViewModel\$*.class",       // lambdas / anonymous classes inside ViewModels

    // Use cases  (package name: use_cases)
    "**/use_cases/**/*.class",

    // Repositories (domain interfaces + data implementations)
    "**/repository/**/*.class",

    // Utility classes (util / utils packages)
    "**/util/**/*.class",
    "**/utils/**/*.class",
)

/** Generated files that might accidentally match an include pattern above. */
val generatedExcludes = listOf(
    "**/*_Factory.class",
    "**/*_Factory\$*.class",
    "**/*_HiltModules.class",
    "**/*_HiltModules\$*.class",
    "**/*_MembersInjector.class",
    "**/*Dao_Impl.class",
    "**/*Dao_Impl\$*.class",
    "**/*Database_Impl.class",
    "**/hilt_aggregated_deps/**",
    "**/generated/**",
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
        include(coverageIncludes)
        exclude(generatedExcludes)
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
                    // ViewModel + UseCase + Repository + Util classes are specifically
                    // tested, so the bar is intentionally higher than the old whole-app threshold.
                    counter = "LINE"
                    value   = "COVEREDRATIO"
                    minimum = "0.70".toBigDecimal() // 70%
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value   = "COVEREDRATIO"
                    minimum = "0.60".toBigDecimal() // 60%
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
