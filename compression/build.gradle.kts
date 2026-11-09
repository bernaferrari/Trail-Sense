plugins {
    id("andromeda.library")
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                withAndroidLibrary()
            }
        }
    }
    sourceSets {
        commonMain.dependencies { implementation(libs.korlibs.compression) }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}
