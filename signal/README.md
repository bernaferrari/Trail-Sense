# Cellular signals

`cellSignals()` observes cellular signal details while collected on Android. Configure cache updates, inclusion of unregistered cells, and the timing-distance correction with `CellSignalOptions`. Check `cellularSignalSupported` and request fine location permission before collecting; availability also depends on the device and carrier. iOS and desktop do not expose these cell details through this API.

Android uses KSensor's application context, normally initialized by AndroidX Startup. The KSensor dependency contributes its boot receiver and permission to the app manifest.
