# Camera

`CameraSurface` displays a Compose preview and provides a `PhotoCamera` for capture, zoom, flash and torch control. Obtain camera permission before composing it. Platform configuration and camera hardware determine which operations are available.

Set `frameAnalysis = true` on `CameraSurface` to enable the `PhotoCamera.frames` flow. Frames contain row-major ARGB pixels and are buffered as latest-only; they may be dropped if processing is slower than capture. Android supplies CameraX rotation degrees, while iOS and desktop currently report rotation as unavailable. Collect frames only while needed; frame copying stops when collection ends, and releasing the preview stops analysis. Copying each frame has a performance cost.

`PhotoCamera.calibration` exposes a `CameraCalibrationSource` when the backend provides sensor metadata. Supply it to `CalibratedCameraAnglePixelMapper` for shared calibrated projection. Android supplies Camera2 calibration through this API. On iOS, CameraK estimates centered pinhole intrinsics from the active video format’s field of view and dimensions; it does not provide manufacturer calibration or distortion coefficients. The estimate is unavailable if the active lens cannot be identified or the format does not report a field of view. Desktop currently returns `null`, so callers can use the shared perspective or linear mapper instead. Use `fieldOfView()` on the source to derive a zoom-adjusted view angle from its intrinsics. A caller with its own calibration can implement `CameraCalibrationSource` on any target. The mapper refreshes cached data when `calibrationId` changes; call `invalidateCalibration()` after other calibration changes.

The calibrated mapper supports both angle-to-pixel and pixel-to-angle conversion. When distortion correction is enabled, inverse conversion solves the distortion polynomial numerically; invalid calibration or a non-converging solution uses the configured fallback mapper.

`PhotoCamera.exposure` reports exposure compensation and its supported EV range when available. `setExposure` applies an EV value; Android rounds to the nearest supported step, iOS uses the native continuous bias, and desktop reports the control as unavailable.

`manualFocusSupported` indicates whether the camera can set its lens position. `setFocus(position)` selects a position from near (0) to far (1); these positions do not represent fixed physical distances. Pass null to resume autofocus. Desktop reports manual focus as unavailable.

`manualExposure` reports supported shutter-duration and ISO ranges when the camera supports manual exposure. `setManualExposure(duration, iso)` sets both together; pass null for both to return to automatic exposure. Desktop reports manual exposure as unavailable.

When `meteringSupported` is true, `focusAt(x, y)` targets a point in the displayed preview using normalized coordinates from its top-left corner. `resetFocusAndMetering()` clears the target; Android restores its previous behavior and iOS selects continuous automatic focus and exposure where supported. Android also clears the target automatically after its platform timeout. Desktop does not expose this control.

`stabilizationSupport` reports whether optical and preview video stabilization can be controlled. `setStabilization` configures available controls. On iOS, video stabilization applies to the preview connection; optical stabilization is managed by the platform and cannot be configured independently. The controls are unavailable on desktop.
