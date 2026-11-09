package com.kylecorry.andromeda.sense.orientation

import kotlin.math.*
import com.kylecorry.sol.math.MathExtensions.toDegrees

object OrientationUtils {

    /**
     * Computes the orientation of the device in compass space.
     * @param orientationSensor The orientation sensor
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param surfaceRotation The surface rotation of the device (default 0)
     * @param declination The declination to use (default null)
     */
    fun getCompassOrientation(
        orientationSensor: OrientationSensor,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        surfaceRotation: Int = 0,
        declination: Float? = null
    ) {
        val x = when (surfaceRotation) {
            0 -> 1
            1 -> 2
            2 -> 129
            3 -> 130
            else -> 1
        }

        val y = when (surfaceRotation) {
            0 -> 2
            1 -> 129
            2 -> 130
            3 -> 1
            else -> 2
        }

        getOrientation(
            orientationSensor,
            x,
            y,
            rotationMatrix,
            orientation,
            declination
        )
    }

    /**
     * Computes the orientation of the device in AR space.
     * @param orientationSensor The orientation sensor
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param surfaceRotation The surface rotation of the device (default 0)
     * @param declination The declination to use (default null)
     */
    fun getAROrientation(
        orientationSensor: OrientationSensor,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        surfaceRotation: Int = 0,
        declination: Float? = null
    ) {
        val x = when (surfaceRotation) {
            0 -> 1
            1 -> 3
            2 -> 129
            3 -> 131
            else -> 1
        }

        val y = when (surfaceRotation) {
            0 -> 3
            1 -> 129
            2 -> 131
            3 -> 1
            else -> 3
        }

        OrientationUtils.getOrientation(
            orientationSensor,
            x,
            y,
            rotationMatrix,
            orientation,
            declination
        )

        orientation[1] = -orientation[1]
        orientation[2] = -orientation[2]
    }

    /**
     * Computes the orientation of the device in the desired coordinate system.
     * @param orientationSensor The orientation sensor
     * @param xAxis The x axis of the coordinate system
     * @param yAxis The y axis of the coordinate system
     * @param rotationMatrix the array to store the rotation matrix in
     * @param orientation The array to store the orientation in (azimuth, pitch, roll in degrees)
     * @param declination The declination to use (default null)
     */
    fun getOrientation(
        orientationSensor: OrientationSensor,
        xAxis: Int,
        yAxis: Int,
        rotationMatrix: FloatArray,
        orientation: FloatArray,
        declination: Float? = null
    ) {
        require(rotationMatrix.size == 9 || rotationMatrix.size == 16)
        require(orientation.size >= 3)
        require(xAxis and 0x7c == 0 && yAxis and 0x7c == 0 &&
            xAxis and 3 in 1..3 && yAxis and 3 in 1..3 && xAxis and 3 != yAxis and 3)
        val q = orientationSensor.rawOrientation
        require(q.size >= 3)
        val x = q[0]; val y = q[1]; val z = q[2]
        val w = if (q.size >= 4) q[3] else sqrt((1f - x*x - y*y - z*z).coerceAtLeast(0f))
        val stride = if (rotationMatrix.size == 16) 4 else 3
        rotationMatrix.fill(0f)
        rotationMatrix[0] = 1 - 2*y*y - 2*z*z
        rotationMatrix[1] = 2*x*y - 2*z*w
        rotationMatrix[2] = 2*x*z + 2*y*w
        rotationMatrix[stride] = 2*x*y + 2*z*w
        rotationMatrix[stride+1] = 1 - 2*x*x - 2*z*z
        rotationMatrix[stride+2] = 2*y*z - 2*x*w
        rotationMatrix[2*stride] = 2*x*z - 2*y*w
        rotationMatrix[2*stride+1] = 2*y*z + 2*x*w
        rotationMatrix[2*stride+2] = 1 - 2*x*x - 2*y*y
        if (stride == 4) rotationMatrix[15] = 1f

        if (xAxis != 1 || yAxis != 2) {
            val source = rotationMatrix.copyOf()
            var zAxis = xAxis xor yAxis
            val xi = (xAxis and 3) - 1
            val yi = (yAxis and 3) - 1
            val zi = (zAxis and 3) - 1
            if (xi != (zi + 1) % 3 || yi != (zi + 2) % 3) zAxis = zAxis xor 0x80
            for (row in 0..2) {
                val offset = row * stride
                rotationMatrix[offset + xi] = source[offset] * if (xAxis and 0x80 != 0) -1 else 1
                rotationMatrix[offset + yi] = source[offset+1] * if (yAxis and 0x80 != 0) -1 else 1
                rotationMatrix[offset + zi] = source[offset+2] * if (zAxis and 0x80 != 0) -1 else 1
            }
        }

        if (declination != null && declination != 0f) {
            val radians = declination * PI.toFloat() / 180f
            val c = cos(radians); val s = sin(radians)
            // Match Android's column-major Matrix.rotateM operation.
            for (column in 0 until stride) {
                val first = rotationMatrix[column]
                val second = rotationMatrix[stride + column]
                rotationMatrix[column] = first * c + second * s
                rotationMatrix[stride + column] = -first * s + second * c
            }
        }
        orientation[0] = atan2(rotationMatrix[1], rotationMatrix[stride+1]).toDegrees()
        orientation[1] = asin(-rotationMatrix[2*stride+1].coerceIn(-1f, 1f)).toDegrees()
        orientation[2] = atan2(-rotationMatrix[2*stride], rotationMatrix[2*stride+2]).toDegrees()
    }
}
