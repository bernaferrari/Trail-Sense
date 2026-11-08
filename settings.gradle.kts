pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Andromeda"
include(":haptics")
include(":notify")
include(":sense")
include(":torch")
include(":qr")
include(":core")
include(":sms")
include(":canvas")
include(":xml")
include(":sound")
include(":gpx")
include(":preferences")
include(":files")
include(":clipboard")
include(":permissions")
include(":background")
include(":camera")
include(":connection")
include(":speech")
include(":battery")
include(":compression")
include(":json")
include(":markdown")
include(":signal")
include(":fragments")
include(":list")
include(":alerts")
include(":pickers")
include(":forms")
include(":csv")
include(":pdf")
include(":wkt")
include(":exceptions")
include(":exceptions-android")
include(":print")
include(":tensorflow")
include(":views")
include(":widgets")
include(":bitmaps")
include(":bitmaps-android")
include(":ipc")
include(":net")
include(":geojson")

include(":shared")
include(":device")
include(":storage")

// Prefer the adjacent multiplatform Sol checkout when developing both libraries together.
if (file("../sol/settings.gradle.kts").exists()) {
    includeBuild("../sol") {
        dependencySubstitution {
            substitute(module("com.kylecorry:sol")).using(project(":"))
        }
    }
}
