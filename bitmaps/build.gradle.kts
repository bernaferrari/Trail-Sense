plugins {
    id("andromeda.library")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.bitmaps.shared"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(projects.core)
            implementation(libs.androidx.core.ktx)
            implementation(libs.luna)
        }
        commonMain.dependencies {
            api(libs.sol)
            api(projects.shared)
        }
    }
}
