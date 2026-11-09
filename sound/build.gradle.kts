plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(libs.kotlinx.atomicfu)
            api(libs.gadulka)
        }
        iosMain.dependencies { implementation(libs.audioCapture) }
        androidMain.dependencies {
            implementation(libs.ksensor.core)
            implementation(libs.kotlinx.coroutines.android)
            implementation(project(":core"))
            implementation(libs.androidx.core.ktx)
            implementation(libs.luna)
        }
    }
}
