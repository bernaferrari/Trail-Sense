plugins {
    id("andromeda.library")
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("jvmShared") {
                withJvm()
                withAndroidLibrary()
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
            implementation(libs.qrcodeKotlin)
        }
        named("jvmSharedMain") {
            dependencies { implementation(libs.zxing) }
        }
        androidMain.dependencies { implementation(libs.androidx.core.ktx) }
        iosMain.dependencies { implementation(libs.kiteqr) }
    }
}
