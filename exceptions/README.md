# Crash reports

`CrashReportStore` writes the latest uncaught Kotlin exception to an Okio filesystem path. Create a store during application startup, call `readPending()` to review a report from the previous run, and call `clearPending(report)` after handling it. The content check prevents an older reviewed report from deleting a newer one. `install()` adds the uncaught exception hook and returns a handle that restores the previous hook when closed. Keep the handle while reporting is needed.

Compose `StackTraceReport`, `AppDetailsReport`, and `DeviceDetailsReport` with `CombinedBugReport` to include the desired information. Create `DeviceDetailsReport` on the UI thread on iOS. The host decides how to present or share a report. The hook handles Kotlin uncaught exceptions; platform-native crashes and hard process termination are outside its scope.
