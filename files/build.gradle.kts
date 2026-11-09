plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.storage)
            api(libs.filekit.core)
            implementation(libs.ktor.http)
            api(libs.kmpZip.okio)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.documentfile)
            implementation(projects.core)
            api(libs.luna)
        }
    }
}
