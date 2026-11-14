plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(project(":core"))
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            api(libs.sol)
            api(project(":shared"))
            api(project(":device"))
        }

    }
}
