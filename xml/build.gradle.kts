plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { implementation(libs.xmlutil.core) }
        getByName("androidDeviceTest").dependencies {
            implementation(libs.junit4)
            implementation(libs.androidx.test.junit)
            implementation(libs.androidx.test.espresso)
        }
    }
}
