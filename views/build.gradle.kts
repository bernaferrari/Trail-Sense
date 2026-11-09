plugins {
    id("andromeda.compose")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.views.shared"
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(project(":shared"))
            api(project(":list"))
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.coil.compose)
            api(libs.zoomimage.compose.coil3)
            implementation(libs.coil.network.ktor3)
            implementation(libs.ktor.client.cio)
        }
    }
}
