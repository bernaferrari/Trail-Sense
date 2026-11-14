plugins {
    id("andromeda.library")
}

kotlin {
    sharedSourceSets(SharedSourceSet.JvmShared)
    sourceSets {
        commonMain.dependencies {
            api(project(":storage"))
            api(libs.filekit.core)
            implementation(libs.ktor.http)
            api(libs.kmpZip.okio)
        }
        named("jvmSharedMain") { dependencies { api(libs.luna) } }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            runtimeOnly(libs.junit.jupiter.engine)
            runtimeOnly(libs.junit.platform.launcher)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.documentfile)
            implementation(project(":core"))
            api(libs.luna)
        }
    }
}
