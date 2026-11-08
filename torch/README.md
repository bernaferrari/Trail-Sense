# Torch

`deviceTorch()` returns a camera torch for shared Kotlin code. Check `isAvailable()` and `isDimmable()` before use; desktop currently reports no camera torch. Android uses the same KSensor context as the device sensor APIs, normally initialized through AndroidX Startup. If startup is disabled, initialize `KSensorContext` with the application context before use. The KSensor Android dependency also contributes its boot receiver and permission to the app manifest. Camera and torch availability depend on hardware and platform permissions.

`screenBrightness()` supplies screen brightness control when available. Pass the result to `ScreenTorch` to save and restore the previous brightness. On Android an active activity is required; desktop currently returns null. iOS brightness changes must be performed on the main thread.

`torchStates()` emits `On`, `Off`, or `Unavailable` for the selected camera torch. Android uses the camera torch callback; iOS checks the device state every half second while collected. Desktop emits `Unavailable` and completes. Collection does not turn the torch on or off.
