plugins {
    id("andromeda.library")
}

kotlin {
    sharedSourceSets(SharedSourceSet.JvmShared)
    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.atomicfu)
        }
    }
}
