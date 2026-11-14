plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(libs.kotlinx.atomicfu)
            api(libs.kable.core)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ksensor.core)
        }
    }
}
