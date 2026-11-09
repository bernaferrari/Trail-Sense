plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.sol)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(projects.permissions)
            implementation(libs.androidx.core.ktx)
            implementation(libs.ksensor.core)
        }
        iosMain.dependencies { implementation(libs.pulsar) }
    }
}
