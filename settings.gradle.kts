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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
rootProject.name = "Trail Sense"

// Opt in while the app is being migrated to the adjacent multiplatform libraries.
// The catalog pins sol 20.0.0 and andromeda 26.0.0, which exist only as the sibling ../sol and ../andromeda
// checkouts until they are published. Until then, build with -PlocalMultiplatformLibraries=true; the default
// (published-library) path resolves from Maven and needs those versions released first.
if (providers.gradleProperty("localMultiplatformLibraries").orNull == "true") {
    includeBuild("../andromeda") {
        dependencySubstitution {
            listOf(
                "alerts", "background", "battery", "bitmaps", "camera", "canvas", "clipboard",
                "compression", "core", "csv", "exceptions", "files", "forms", "geojson", "gpx",
                "haptics", "ipc", "json", "list", "markdown", "notify", "pdf", "permissions", "print",
                "qr", "sense", "signal", "sound", "torch", "views", "widgets", "xml"
            ).forEach { name ->
                substitute(module("com.kylecorry.andromeda:$name")).using(project(":$name"))
            }
            substitute(module("com.kylecorry.andromeda:preferences")).using(project(":storage"))
        }
    }
    includeBuild("../sol") {
        dependencySubstitution {
            substitute(module("com.kylecorry:sol")).using(project(":"))
        }
    }
}
