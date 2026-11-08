# Camera

`CameraSurface` displays a Compose preview and provides a `PhotoCamera` for capture, zoom, flash and torch control. Obtain camera permission before composing it. Platform configuration and camera hardware determine which operations are available.

`PhotoCamera.calibration` exposes a `CameraCalibrationSource` when the backend provides sensor metadata. Supply it to `CalibratedCameraAnglePixelMapper` for shared calibrated projection. Android supplies Camera2 calibration through this API; the current iOS and desktop CameraK backend returns `null`, so callers can use the shared perspective or linear mapper instead. A caller with its own calibration can implement `CameraCalibrationSource` on any target. The mapper refreshes cached data when `calibrationId` changes; call `invalidateCalibration()` after other calibration changes.
