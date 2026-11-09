plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(projects.core)
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            api(libs.sol)
            api(projects.shared)
            api(projects.device)
        }

    }
}
