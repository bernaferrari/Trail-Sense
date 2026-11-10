plugins {
    id("andromeda.library")
}

kotlin {
    sharedSourceSets(SharedSourceSet.JvmShared)
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(libs.qrcodeKotlin)
        }
        named("jvmSharedMain") {
            dependencies { implementation(libs.zxing) }
        }
        androidMain.dependencies { implementation(libs.androidx.core.ktx) }
        iosMain.dependencies { implementation(libs.kiteqr) }
    }
}
