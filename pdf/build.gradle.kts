plugins {
    id("andromeda.library")
}

kotlin {
    sharedSourceSets(SharedSourceSet.JvmShared)
    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(project(":shared"))
            api(project(":bitmaps"))
            implementation(project(":wkt"))
        }
        named("jvmSharedMain") {
            dependencies {
            api(libs.luna)
            implementation(project(":compression"))
            }
        }
        androidMain.dependencies {
            implementation(libs.ksensor.core)
            implementation(project(":core"))
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
