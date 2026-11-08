plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(17)
    sourceSets.commonMain.dependencies {
        api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        api("com.kylecorry:sol:19.4.0")
        implementation("io.github.pdvrieze.xmlutil:core:1.0.2")
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}

group = "com.kylecorry.andromeda"
version = "25.1.1"
mavenPublishing {
    coordinates("com.kylecorry.andromeda", "shared", "25.1.1")
}
