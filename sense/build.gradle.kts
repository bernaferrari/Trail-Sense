plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(project(":device"))
            api(project(":storage"))
            implementation(libs.kotlinx.atomicfu)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(project(":core"))
            implementation(project(":permissions"))
            api(libs.luna)
        }
    }
}
