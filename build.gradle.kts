plugins {
    kotlin("multiplatform") version "2.3.0"
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("dev.detekt") version "2.0.0-alpha.5"
}

val versionName = "19.4.0"

mavenPublishing {
    coordinates("com.kylecorry", "sol", versionName)

    pom {
        name.set("Sol")
        description.set("A Kotlin library for science and math in the real world.")
        url.set("https://github.com/kylecorry31/sol")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("kylecorry31")
                name.set("Kyle Corry")
                email.set("kylecorry31@gmail.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/kylecorry31/sol.git")
            developerConnection.set("scm:git:ssh://github.com:kylecorry31/sol.git")
            url.set("https://github.com/kylecorry31/sol")
        }
    }

    publishToMavenCentral()
    signAllPublications()
}

repositories {
    maven {
        url = uri("https://jitpack.io")
        content {
            includeGroupByRegex("com\\.github.*")
        }
    }
    mavenCentral()
}

dependencies {
    detektPlugins("com.kylecorry:orion:1.1.0")
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("com.willowtreeapps.assertk:assertk:0.28.1")
        }
        jvmTest.dependencies {
            runtimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    setSource(files("src/main/kotlin", "src/commonMain/kotlin"))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**")
    exclude("**/gov/nasa/worldwind/**")
    jvmTarget = "11"
}

tasks.withType<dev.detekt.gradle.DetektCreateBaselineTask>().configureEach {
    setSource(files("src/main/kotlin", "src/commonMain/kotlin"))
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**")
    exclude("**/gov/nasa/worldwind/**")
    jvmTarget = "11"
}

