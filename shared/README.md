# Shared Trail Sense app

`:core` holds domain logic, Room entities, DAOs, and repositories. `:shared` holds
Compose screens, navigation, and the design system. The Android app consumes
both modules and supplies its own sensors, services, and system integrations.
iOS, desktop, and web use the shared app shell and register their platform
dependencies in their launchers.

| Target | Local entry point |
| --- | --- |
| Android | `scripts/build.sh` |
| iOS | Open `iosApp/TrailSense.xcodeproj` in Xcode and run the TrailSense scheme |
| Desktop | `./gradlew :shared:run` |
| Web development | `./gradlew :shared:wasmJsBrowserDevelopmentRun` |
| Web distribution | `./gradlew :shared:wasmJsBrowserDistribution` then `python3 scripts/package-web.py shared/build/dist/wasmJs/productionExecutable` |

Set `ANDROID_HOME` to the Android SDK for Gradle's included builds. Serve the
web distribution over HTTPS or localhost so the browser can use its service
worker and SQLite storage. Browser data stays in that origin. Close the other
Trail Sense tab if the browser reports that storage is busy.

Start a desktop or browser smoke check with Notes, Convert, and Survival
Guide. Tool capabilities depend on the host. Background Backtrack, scheduled
alerts, and offline vector maps are currently supplied by Android.

Shared strings are generated from the Android translation resources listed in
`shared-strings.txt`. The checked-in English guides live in `guides/en-US`.
