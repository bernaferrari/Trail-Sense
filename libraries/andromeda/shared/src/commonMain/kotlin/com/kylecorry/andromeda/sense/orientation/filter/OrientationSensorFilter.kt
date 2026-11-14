package com.kylecorry.andromeda.sense.orientation.filter

public interface OrientationSensorFilter {
    public fun filter(quaternion: FloatArray, out: FloatArray)
    public fun reset(value: FloatArray)
}