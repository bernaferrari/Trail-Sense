plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kmpnotifier.local)
    }
    sourceSets.androidMain.dependencies {
        api(libs.androidx.core.ktx)
        implementation(projects.core)
    }
}
