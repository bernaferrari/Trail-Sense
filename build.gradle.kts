import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
}

val versionName = "3.0.0"

group = "com.kylecorry"
version = versionName

kotlin {
    jvmToolchain(17)
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        browser { testTask { enabled = false } }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.atomicfu)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.junit.bom))
            implementation(libs.junit.jupiter)
            implementation(libs.junit.jupiter.params)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates("com.kylecorry", "luna", versionName)

    pom {
        name = "Luna"
        description = "A Kotlin Multiplatform library of useful Kotlin utilities."
        url = "https://github.com/kylecorry31/luna"
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
            connection = "scm:git:git://github.com/kylecorry31/luna.git"
            developerConnection = "scm:git:ssh://github.com:kylecorry31/luna.git"
            url = "https://github.com/kylecorry31/luna"
        }
    }

    publishToMavenCentral()
    // Maven Central requires signatures; local and snapshot publishing works without a key
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
