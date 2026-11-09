plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            implementation(libs.kotlinx.atomicfu)
            api(libs.gadulka)
        }
        iosMain.dependencies { implementation(libs.audioCapture) }
        androidMain.dependencies {
            implementation(libs.ksensor.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(projects.core)
            implementation(libs.androidx.core.ktx)
            implementation(libs.luna)
        }
    }
}
