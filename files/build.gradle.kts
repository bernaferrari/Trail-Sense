plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":storage"))
            api(libs.filekit.core)
            implementation(libs.ktor.http)
            api(libs.kmpZip.okio)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.documentfile)
            implementation(project(":core"))
            api(libs.luna)
        }
    }
}
