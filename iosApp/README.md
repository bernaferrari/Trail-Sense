# iOS app

The iOS host currently exposes the shared notes feature. Notes are stored locally in the app container using Room and bundled SQLite.

Open `TrailSense.xcodeproj` in Xcode and run the `TrailSense` scheme on an iOS simulator or device. The build invokes Gradle to compile and embed the shared Compose framework; CocoaPods is not required. `project.yml` is the XcodeGen source for the checked-in project. After changing it, run `xcodegen generate --spec iosApp/project.yml` from the repository root.
