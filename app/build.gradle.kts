import java.time.LocalDate

plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    id("com.google.devtools.ksp")
    id("dev.detekt")
    id("kotlin-parcelize")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.kylecorry.trail_sense"
    compileSdk = 37

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.kylecorry.trail_sense"
        minSdk = 24
        targetSdk = 37
        versionCode = 149
        versionName = "8.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }
    signingConfigs {
        create("nightly") {
            if (System.getProperty("nightly_store_file") != null) {
                storeFile = file(System.getProperty("nightly_store_file"))
                storePassword = System.getProperty("nightly_store_password")
                keyAlias = System.getProperty("nightly_key_alias")
                keyPassword = System.getProperty("nightly_key_password")
            }
        }
        create("github") {
            if (System.getProperty("github_store_file") != null) {
                storeFile = file(System.getProperty("github_store_file"))
                storePassword = System.getProperty("github_store_password")
                keyAlias = System.getProperty("github_key_alias")
                keyPassword = System.getProperty("github_key_password")
            }
        }
    }
    androidResources {
        // Support for auto-generated locales for per-app language settings
        generateLocaleConfig = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    buildTypes {
        // ------- RELEASES -------
        // F-Droid
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Play Store
        create("playStore") {
            initWith(getByName("release"))
        }
        // GitHub
        create("github") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("github")
        }

        // ------- DEVELOPMENT -------
        // Local debug
        getByName("debug") {
            testProguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Staging
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            signingConfig = signingConfigs.getByName("debug")
        }

        // ------- NIGHTLY -------

        // Debug
        create("nightly") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-nightly-${LocalDate.now()}"
        }
        // Release
        create("nightlyRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly_release"
            versionNameSuffix = "-nightly-release-${LocalDate.now()}"
        }
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources.merges += "META-INF/LICENSE.md"
        resources.merges += "META-INF/LICENSE-notice.md"
        jniLibs {
            useLegacyPackaging = true
        }
    }


    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$projectDir/detekt-baseline.xml")
    source.setFrom("src/main/java", "src/main/kotlin")
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    jvmTarget = JavaVersion.VERSION_11.toString()
}

dependencies {
    // Internal modules
    implementation(project(":shared"))

    // Kotlin
    implementation(libs.kotlinx.coroutines.android)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX
    implementation(libs.constraintlayout)
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.documentfile)
    implementation(libs.gridlayout)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.lifecycle.runtime.compose)
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.process)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.exifinterface)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:${libs.versions.material3Compose.get()}")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    constraints {
        // Instrumented tests run against the debug app's classpath, and androidx.test:core needs the newer futures.
        debugImplementation("androidx.concurrent:concurrent-futures:1.2.0")
        debugImplementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    }
    androidTestImplementation(libs.compose.ui.test.junit4)

    // Material
    implementation(libs.material)
    implementation(libs.flexbox)

    // Andromeda
    implementation(libs.andromeda.core)
    implementation(libs.andromeda.forms)
    implementation(libs.andromeda.csv)
    implementation(libs.andromeda.background)
    implementation(libs.andromeda.camera)
    implementation(libs.andromeda.gpx)
    implementation(libs.andromeda.sound)
    implementation(libs.andromeda.sense)
    implementation(libs.andromeda.signal)
    implementation(libs.andromeda.preferences)
    implementation(libs.andromeda.permissions)
    implementation(libs.andromeda.canvas)
    implementation(libs.andromeda.files)
    implementation(libs.andromeda.notify)
    implementation(libs.andromeda.alerts)
    implementation(libs.andromeda.pickers)
    implementation(libs.andromeda.list)
    implementation(libs.andromeda.qr)
    implementation(libs.andromeda.markdown)
    implementation(libs.andromeda.clipboard)
    implementation(libs.andromeda.haptics)
    implementation(libs.andromeda.bitmaps)
    implementation(libs.andromeda.torch)
    implementation(libs.andromeda.battery)
    implementation(libs.andromeda.compression)
    implementation(libs.andromeda.pdf)
    implementation(libs.andromeda.exceptions)
    implementation(libs.andromeda.print)
    implementation(libs.andromeda.views)
    implementation(libs.andromeda.xml)
    implementation(libs.andromeda.json)
    implementation(libs.andromeda.widgets)
    implementation(libs.andromeda.ipc)
    implementation(libs.andromeda.geojson)

    // Misc
    implementation(libs.sol)
    implementation(libs.luna)

    // Mapsforge
    implementation(libs.mapsforge.map.android)
    implementation(libs.mapsforge.map.reader)

    // Linting
    detektPlugins(libs.orion)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestUtil(libs.androidx.test.orchestrator)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.arch.core)
    androidTestImplementation(libs.work.testing)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.kotlin)
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
