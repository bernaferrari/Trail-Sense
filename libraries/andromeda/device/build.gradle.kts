plugins {
    id("andromeda.library")
}

kotlin {
    sharedSourceSets(SharedSourceSet.Mobile)
    sourceSets {
        commonMain.dependencies {
            api(libs.sol)
            api(project(":shared"))
            implementation(libs.kotlinx.atomicfu)
            api(libs.kotlinx.coroutines.core)
        }
        named("mobileMain") {
            dependencies {
            api(libs.ksensor.sensors.motion)
            api(libs.ksensor.sensors.environment)
            api(libs.ksensor.sensors.positioning)
            api(libs.ksensor.states.system)
            }
        }
        androidMain.dependencies { implementation(libs.androidx.core.ktx) }
        jvmMain.dependencies { implementation(libs.oshi.core) }
    }
}
