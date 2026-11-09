plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies { api(projects.shared); api(projects.bitmaps) }
        jvmMain.dependencies { implementation(libs.pdfbox) }
        androidMain.dependencies { implementation(projects.core); implementation(libs.androidx.core.ktx); implementation(libs.kotlinx.coroutines.android); implementation(libs.androidx.print) }
    }
}
