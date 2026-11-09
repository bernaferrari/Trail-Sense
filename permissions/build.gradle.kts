plugins {
    id("andromeda.library")
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("mobile") {
                withAndroidLibrary()
                group("ios")
            }
        }
    }
    sourceSets {
        commonMain.dependencies { api(projects.shared) }
        named("mobileMain") {
            dependencies {
            api(libs.moko.permissions)
            implementation(libs.moko.permissions.camera)
            implementation(libs.moko.permissions.location)
            implementation(libs.moko.permissions.microphone)
            implementation(libs.moko.permissions.bluetooth)
            implementation(libs.moko.permissions.notifications)
            implementation(libs.moko.permissions.motion)
            implementation(libs.moko.permissions.gallery)
            implementation(libs.moko.permissions.contacts)
            implementation(libs.moko.permissions.storage)
            }
        }
        androidMain.dependencies {
            implementation(projects.core)
            implementation(libs.androidx.core.ktx)
            api(libs.androidx.activity)
        }
    }
}
