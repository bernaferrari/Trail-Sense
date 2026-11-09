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
            api(libs.sol)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
