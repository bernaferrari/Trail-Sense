package com.kylecorry.trail_sense.tools.augmented_reality.domain.mapper

import com.kylecorry.andromeda.camera.Camera
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences

class CameraAnglePixelMapperFactory {
    fun getMapper(
        type: CameraPreferences.ProjectionType,
        camera: Camera
    ): CameraAnglePixelMapper {
        val perspective = SimplePerspectiveCameraAnglePixelMapper()

        return when (type) {
            CameraPreferences.ProjectionType.EstimatedIntrinsics -> CalibratedCameraAnglePixelMapper(
                camera,
                perspective,
                useManufacturerCalibration = false
            )

            CameraPreferences.ProjectionType.ManufacturerIntrinsics -> CalibratedCameraAnglePixelMapper(
                camera,
                perspective,
                useManufacturerCalibration = true
            )

            CameraPreferences.ProjectionType.Perspective -> perspective
            CameraPreferences.ProjectionType.Linear -> LinearCameraAnglePixelMapper()
        }
    }
}
