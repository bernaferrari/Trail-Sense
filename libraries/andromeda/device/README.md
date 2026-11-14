# Device sensors

`DeviceSensors.location()` reports position and, when available, mean-sea-level altitude. `mslAltitude()` exposes that altitude as a flow in meters; `mslAltimeter(scope)` adapts it to the shared `Altimeter` sensor interface. The flow emits only when a location has an MSL altitude.

On Android, the location API's MSL altitude takes precedence. When it is unavailable, the GPS stream uses a recent NMEA altitude if the device supplies one; fine location permission is required. On iOS, the value comes from Core Location. Availability depends on the device and location provider.

`satelliteStatus()` emits detected GNSS satellite details on Android, and `satelliteStatusSensor(scope)` exposes the same data through the shared sensor interface. Android requires fine location permission and reports status while its GNSS receiver is active. iOS and desktop do not expose per-satellite status through these APIs, so their flows complete without readings.

`geofenceTransitions(fences)` detects entry and exit from circular regions while collecting a location stream. It emits an entry when the first position is already inside, supports optional expiration, and ignores out-of-order locations. This shared monitor runs only while collected; it does not register a background geofence with the operating system. The Android `sense` module still provides its native PendingIntent-based background API.
