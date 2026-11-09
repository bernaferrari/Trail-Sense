plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(projects.device)
            api(projects.storage)
            implementation(libs.kotlinx.atomicfu)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(projects.core)
            implementation(projects.permissions)
            api(libs.luna)
        }
    }
}
