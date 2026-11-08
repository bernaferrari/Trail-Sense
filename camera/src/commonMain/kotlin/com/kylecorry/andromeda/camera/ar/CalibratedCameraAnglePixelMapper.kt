package com.kylecorry.andromeda.camera.ar

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.geometry.Size
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A camera angle pixel mapper that uses the intrinsic calibration of the camera to map angles to pixels.
 * @param camera The camera to use
 * @param fallback The fallback mapper to use if the camera does not have intrinsic calibration
 * @param applyDistortionCorrection Whether to apply distortion correction. Defaults to false.
 */
class CalibratedCameraAnglePixelMapper(
    private val camera: CameraCalibrationSource,
    private val fallback: CameraAnglePixelMapper = LinearCameraAnglePixelMapper(),
    private val applyDistortionCorrection: Boolean = false,
    private val useManufacturerCalibration: Boolean = false
) : CameraAnglePixelMapper {

    private var calibration: FloatArray? = null
    private var preActiveArray: ImageBounds? = null
    private var activeArray: ImageBounds? = null
    private var distortion: FloatArray? = null
    private val linear = LinearCameraAnglePixelMapper()
    private var calibrationId = camera.calibrationId

    /** Clears cached metadata after a camera or calibration change. */
    fun invalidateCalibration() {
        calibration = null
        preActiveArray = null
        activeArray = null
        distortion = null
        calibrationId = camera.calibrationId
    }

    override fun getAngle(
        x: Float,
        y: Float,
        imageRect: ImageBounds,
        fieldOfView: Size
    ): Vector2 {
        val projection = projection() ?: return fallback.getAngle(x, y, imageRect, fieldOfView)
        val zoom = getZoom()
        if (!zoom.isFinite() || imageRect.width <= 0f || imageRect.height <= 0f) {
            return fallback.getAngle(x, y, imageRect, fieldOfView)
        }
        val intrinsic = projection.intrinsic
        val active = projection.active
        val left = imageRect.centerX - zoom * imageRect.width / 2f
        val top = imageRect.centerY - zoom * imageRect.height / 2f
        val correctedX = active.left + (x - left) / (zoom * imageRect.width) * active.width
        val correctedY = active.bottom - (y - top) / (zoom * imageRect.height) * active.height
        val corrected = Vector2(correctedX, correctedY)
        val point = if (applyDistortionCorrection && projection.distortion != null) {
            invertDistortion(corrected, projection.preActive, intrinsic[2], intrinsic[3], projection.distortion)
                ?: return fallback.getAngle(x, y, imageRect, fieldOfView)
        } else corrected
        return Vector2(
            atan((point.x - intrinsic[2]) / intrinsic[0]).toDegrees(),
            atan((point.y - intrinsic[3]) / intrinsic[1]).toDegrees(),
        )
    }

    private data class Projection(
        val intrinsic: FloatArray,
        val preActive: ImageBounds,
        val active: ImageBounds,
        val distortion: FloatArray?,
    )

    private fun projection(): Projection? {
        if (camera.calibrationId != calibrationId) invalidateCalibration()
        val intrinsic = getCalibration() ?: return null
        val active = getActiveArraySize() ?: return null
        val preActive = getPreActiveArraySize() ?: active
        if (intrinsic.size < 5 || (0..3).any { !intrinsic[it].isFinite() } ||
            intrinsic[0] <= 0f || intrinsic[1] <= 0f ||
            active.width <= 0f || active.height <= 0f ||
            !active.width.isFinite() || !active.height.isFinite()) return null
        val correction = if (applyDistortionCorrection) getDistortion()?.takeIf {
            it.size >= 5 && (0..4).all { index -> it[index].isFinite() } &&
                max(intrinsic[2] - preActive.left, preActive.right - intrinsic[2]) > 0f &&
                max(intrinsic[3] - preActive.top, preActive.bottom - intrinsic[3]) > 0f
        } else null
        return Projection(intrinsic, preActive, active, correction)
    }

    private fun getCalibration(): FloatArray? {
        if (calibration == null) {
            val calibration =
                camera.intrinsicCalibration(onlyUseEstimated = !useManufacturerCalibration)
                    ?: return null
            if (calibration.size < 5) return null
            val rotation = camera.sensorRotationDegrees
            this.calibration = if (rotation == 90 || rotation == 270) {
                floatArrayOf(
                    calibration[1],
                    calibration[0],
                    calibration[3],
                    calibration[2],
                    calibration[4]
                )
            } else {
                calibration
            }
        }
        return calibration
    }

    private fun getPreActiveArraySize(): ImageBounds? {
        if (preActiveArray == null) {
            val activeArray = camera.activeArray(preCorrection = true) ?: return null
            val rotation = camera.sensorRotationDegrees
            this.preActiveArray = if (rotation == 90 || rotation == 270) {
                ImageBounds(activeArray.top, activeArray.left, activeArray.bottom, activeArray.right)
            } else {
                activeArray
            }
        }
        return preActiveArray
    }

    private fun getActiveArraySize(): ImageBounds? {
        if (activeArray == null) {
            val activeArray = camera.activeArray(preCorrection = false) ?: return null
            val rotation = camera.sensorRotationDegrees
            this.activeArray = if (rotation == 90 || rotation == 270) {
                ImageBounds(activeArray.top, activeArray.left, activeArray.bottom, activeArray.right)
            } else {
                activeArray
            }
        }
        return activeArray
    }

    private fun getDistortion(): FloatArray? {
        if (distortion == null) {
            distortion = camera.distortionCorrection()
        }
        return distortion
    }

    private fun getZoom(): Float = camera.zoomRatio?.coerceAtLeast(0.05f) ?: 1f

    override fun getPixel(
        angleX: Float,
        angleY: Float,
        imageRect: ImageBounds,
        fieldOfView: Size,
        distance: Float?
    ): PixelCoordinate {
        // TODO: Factor in pose translation (just add it to the world position?)
        val world = toCartesian(angleX, angleY, distance ?: 1f)

        // Point is behind the camera, so calculate the linear projection
        if (world.z < 0) {
            return linear.getPixel(angleX, angleY, imageRect, fieldOfView, distance)
        }

        val projection = projection() ?: return fallback.getPixel(angleX, angleY, imageRect, fieldOfView, distance)
        val calibration = projection.intrinsic
        val preActiveArray = projection.preActive
        val activeArray = projection.active
        val distortion = projection.distortion

        val fx = calibration[0]
        val fy = calibration[1]
        val cx = calibration[2]
        val cy = calibration[3]

        // Get the pixel in the pre-active array
        val preX = fx * (world.x / world.z) + cx
        val preY = fy * (world.y / world.z) + cy

        // Correct for distortion
        val corrected = if (applyDistortionCorrection && distortion != null) {
            undistort(preX, preY, preActiveArray, cx, cy, distortion)
        } else {
            Vector2(preX, preY)
        }

        // Translate to the active array
        val activeX = corrected.x - activeArray.left
        val activeY = corrected.y - activeArray.top

        // The y axis is inverted (TODO: Why?)
        val invertedY = activeArray.height - activeY

        // Unzoom the output image
        val zoom = getZoom()
        if (!zoom.isFinite()) return fallback.getPixel(angleX, angleY, imageRect, fieldOfView, distance)
        val rectLeft = imageRect.centerX - zoom * imageRect.width / 2f
        val rectWidth = zoom * imageRect.width
        val rectTop = imageRect.centerY - zoom * imageRect.height / 2f
        val rectHeight = zoom * imageRect.height

        // Scale to the output image dimensions
        val pctX = activeX / activeArray.width
        val pctY = invertedY / activeArray.height
        val pixelX = pctX * rectWidth + rectLeft
        val pixelY = pctY * rectHeight + rectTop

        return PixelCoordinate(pixelX, pixelY)
    }

    private fun undistort(
        x: Float,
        y: Float,
        activeArray: ImageBounds,
        cx: Float,
        cy: Float,
        distortion: FloatArray
    ): Vector2 {
        val sizeX = max(cx - activeArray.left, activeArray.right - cx)
        val sizeY = max(cy - activeArray.top, activeArray.bottom - cy)

        val normalizedX = (x - cx) / sizeX
        val normalizedY = (y - cy) / sizeY

        val rSquared = normalizedX * normalizedX + normalizedY * normalizedY

        val radialDistortion =
            1 + distortion[0] * rSquared + distortion[1] * rSquared * rSquared + distortion[2] * rSquared * rSquared * rSquared

        val xc =
            normalizedX * radialDistortion + distortion[3] * (2 * normalizedX * normalizedY) + distortion[4] * (rSquared + 2 * normalizedX * normalizedX)
        val yc =
            normalizedY * radialDistortion + distortion[3] * (rSquared + 2 * normalizedX * normalizedY) + distortion[4] * (2 * normalizedY * normalizedY)

        return Vector2(xc * sizeX + cx, yc * sizeY + cy)
    }

    /** Inverts the distortion polynomial used by the forward projection. */
    private fun invertDistortion(
        target: Vector2,
        activeArray: ImageBounds,
        cx: Float,
        cy: Float,
        distortion: FloatArray,
    ): Vector2? {
        var x = target.x
        var y = target.y
        val tolerance = max(0.001f, max(abs(x), abs(y)) * 0.000001f)
        repeat(15) {
            val value = undistort(x, y, activeArray, cx, cy, distortion)
            val errorX = value.x - target.x
            val errorY = value.y - target.y
            if (!errorX.isFinite() || !errorY.isFinite()) return null
            if (abs(errorX) < tolerance && abs(errorY) < tolerance) return Vector2(x, y)
            val stepX = max(0.05f, abs(x) * 0.0001f)
            val stepY = max(0.05f, abs(y) * 0.0001f)
            val dx = undistort(x + stepX, y, activeArray, cx, cy, distortion)
            val dy = undistort(x, y + stepY, activeArray, cx, cy, distortion)
            val a = (dx.x - value.x) / stepX
            val b = (dy.x - value.x) / stepY
            val c = (dx.y - value.y) / stepX
            val d = (dy.y - value.y) / stepY
            val determinant = a * d - b * c
            if (!determinant.isFinite() || abs(determinant) < 1e-8f) return null
            x -= (d * errorX - b * errorY) / determinant
            y -= (a * errorY - c * errorX) / determinant
        }
        return null
    }

    private fun toCartesian(
        bearing: Float,
        altitude: Float,
        radius: Float
    ): Vector3 {
        val altitudeRad = altitude.toRadians()
        val bearingRad = bearing.toRadians()
        val cosAltitude = cos(altitudeRad)
        val sinAltitude = sin(altitudeRad)
        val cosBearing = cos(bearingRad)
        val sinBearing = sin(bearingRad)

        // X and Y are flipped
        val x = sinBearing * cosAltitude * radius
        val y = cosBearing * sinAltitude * radius
        val z = cosBearing * cosAltitude * radius
        return Vector3(x, y, z)
    }
}