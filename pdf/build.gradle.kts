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
        commonMain.dependencies {
            api(libs.sol)
            api(projects.shared)
            api(projects.bitmaps)
            implementation(projects.wkt)
        }
        named("jvmSharedMain") {
            dependencies {
            api(libs.luna)
            implementation(projects.compression)
            }
        }
        androidMain.dependencies {
            implementation(libs.ksensor.core)
            implementation(projects.core)
            implementation(libs.androidx.core.ktx)
        }
        jvmMain.dependencies { implementation(libs.pdfbox) }
        iosMain.dependencies { implementation(libs.kitepdf) }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit4)
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.params)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.junit4)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.espresso)
        }
    }
}
