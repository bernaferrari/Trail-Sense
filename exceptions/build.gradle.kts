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
            api(project(":shared"))
            api(project(":core"))
            api(libs.okio)
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
