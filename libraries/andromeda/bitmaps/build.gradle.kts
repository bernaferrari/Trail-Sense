plugins {
    id("andromeda.library")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.bitmaps.shared"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(project(":core"))
            implementation(libs.androidx.core.ktx)
            implementation(libs.luna)
        }
        commonMain.dependencies {
            api(libs.sol)
            api(project(":shared"))
        }
    }
}
