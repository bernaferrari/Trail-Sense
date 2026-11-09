plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared")) }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx); implementation(libs.ksensor.core) }
    }
}
