import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.detekt)
}

val versionName = "20.0.0"

// Some astronomy tests take several seconds on Wasm. Kotlin/JS is not a target because it stores Float as a
// 64-bit double, which changes the results of sol's single precision math.
val MOCHA_TIMEOUT = "120s"

kotlin {
    explicitApi()

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()

    jvmToolchain(17)
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs { testTask { useMocha { timeout = MOCHA_TIMEOUT } } }
        browser { testTask { enabled = false } }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
        }
        webTest.dependencies {
            implementation(npm("@js-joda/timezone", libs.versions.jsJoda.timezone.get()))
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.junit.bom))
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates("com.kylecorry", "sol", versionName)

    pom {
        name = "Sol"
        description = "A Kotlin Multiplatform library for science and math in the real world."
        url = "https://github.com/kylecorry31/sol"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "kylecorry31"
                name = "Kyle Corry"
                email = "kylecorry31@gmail.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/kylecorry31/sol.git"
            developerConnection = "scm:git:ssh://github.com:kylecorry31/sol.git"
            url = "https://github.com/kylecorry31/sol"
        }
    }

    publishToMavenCentral()
    // Maven Central requires signatures; local and snapshot publishing works without a key
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}

dependencies {
    detektPlugins(libs.detekt.orion)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

// Analyze the production code of every target in one pass
val productionSources = files(provider {
    kotlin.sourceSets.filter { it.name.endsWith("Main") }.flatMap { it.kotlin.srcDirs }.filter { it.exists() }
})

tasks.withType<Detekt>().configureEach {
    setSource(productionSources)
    exclude("**/gov/nasa/worldwind/**")
    jvmTarget = "11"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    setSource(productionSources)
    exclude("**/gov/nasa/worldwind/**")
    jvmTarget = "11"
}
