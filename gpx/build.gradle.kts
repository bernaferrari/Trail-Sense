plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":xml")); api(libs.sol) }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.junit4)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.espresso)
        }
    }
}
