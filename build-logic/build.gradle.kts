plugins {
    `kotlin-dsl`
}

dependencies {
    // Provided by the plugins block of the root build, so every module shares one copy of each plugin
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.mavenPublish.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("library") {
            id = "andromeda.library"
            implementationClass = "AndromedaLibraryPlugin"
        }
        register("compose") {
            id = "andromeda.compose"
            implementationClass = "AndromedaComposePlugin"
        }
    }
}
