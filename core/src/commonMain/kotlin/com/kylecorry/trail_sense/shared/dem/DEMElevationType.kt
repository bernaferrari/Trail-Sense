package com.kylecorry.trail_sense.shared.dem

enum class DEMElevationType {
    Land,
    InlandWater,
    Ocean;

    companion object {
        /** The water mask channel of a DEM tile: dark is land, mid gray is inland water, light is ocean. */
        fun fromWaterMask(maskValue: Float): DEMElevationType {
            return when {
                maskValue < 64f -> Land
                maskValue < 192f -> InlandWater
                else -> Ocean
            }
        }
    }
}
