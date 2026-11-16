pluginManagement {
    repositories {
//        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
//        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://jitpack.io")
    }
}

include(":app")
include(":shared")
include(":core")
rootProject.name = "trail-sense"

// Build the shared libraries from this repository so every target uses the same revisions.
includeBuild("libraries/andromeda") {
    dependencySubstitution {
        listOf(
            "alerts", "background", "battery", "bitmaps", "camera", "canvas", "clipboard",
            "compression", "core", "csv", "device", "exceptions", "files", "forms", "geojson", "gpx",
            "haptics", "ipc", "json", "list", "markdown", "notify", "pdf", "permissions", "print",
            "qr", "sense", "signal", "sound", "torch", "views", "widgets", "xml"
        ).forEach { name ->
            substitute(module("com.kylecorry.andromeda:$name")).using(project(":$name"))
        }
        substitute(module("com.kylecorry.andromeda:preferences")).using(project(":storage"))
    }
}
includeBuild("libraries/sol") {
    dependencySubstitution { substitute(module("com.kylecorry:sol")).using(project(":")) }
}
includeBuild("libraries/luna") {
    dependencySubstitution { substitute(module("com.kylecorry:luna")).using(project(":")) }
}
