plugins {
    id("andromeda.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.datetime)
        api(libs.sol)
        api(project(":shared"))
        api(libs.multiplatformSettings.noArg)
        api(libs.okio)
    }
    sourceSets.jvmTest.dependencies {
        implementation(libs.multiplatformSettings.test)
    }
}
