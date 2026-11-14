plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets.androidMain.dependencies {
        implementation(libs.ksensor.core)
    }
}
