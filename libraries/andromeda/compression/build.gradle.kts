plugins {
    id("andromeda.library")
}

kotlin {
    sharedSourceSets(SharedSourceSet.JvmShared)
    sourceSets {
        commonMain.dependencies { implementation(libs.korlibs.compression) }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
