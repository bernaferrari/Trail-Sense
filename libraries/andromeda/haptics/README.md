# Haptics

`deviceHaptics()` provides a platform haptic motor to shared Kotlin code. Check `isAvailable()` before use. Android uses its vibrator service, iOS uses Pulsar and Core Haptics, and desktop reports unavailable. Android apps must declare the vibration permission; iOS haptic calls should run on the main thread.

Android uses the KSensor application context, normally initialized by AndroidX Startup. If startup is disabled, initialize `KSensorContext` with the application context before use. The KSensor Android dependency contributes its boot receiver and permission to the app manifest.
