plugins {
    id("andromeda.library")
}

kotlin {
    android {
        namespace = "com.kylecorry.andromeda.exceptions.shared"
    }
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
            api(projects.core)
            api(libs.okio)
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
