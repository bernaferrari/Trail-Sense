plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(project(":shared")); api(project(":bitmaps")) }
        jvmMain.dependencies { implementation(libs.pdfbox) }
        androidMain.dependencies { implementation(project(":core")); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android); implementation(libs.androidx.print) }
    }
}
