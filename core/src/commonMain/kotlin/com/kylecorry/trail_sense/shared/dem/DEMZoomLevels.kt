package com.kylecorry.trail_sense.shared.dem

/** Which map zoom levels draw DEM images, and the grid resolution (degrees) each one samples. */
object DEMZoomLevels {
    private const val BASE_RESOLUTION = 1 / 240.0
    const val IMAGE_MIN_ZOOM_LEVEL = 10
    const val IMAGE_MAX_ZOOM_LEVEL = 19

    val HIGH_RESOLUTION_ZOOM_TO_RESOLUTION = mapOf(
        10 to BASE_RESOLUTION * 2,
        11 to BASE_RESOLUTION,
        12 to BASE_RESOLUTION,
        13 to BASE_RESOLUTION / 2,
        14 to BASE_RESOLUTION / 2,
        15 to BASE_RESOLUTION / 4,
        16 to BASE_RESOLUTION / 4,
        17 to BASE_RESOLUTION / 4,
        18 to BASE_RESOLUTION / 4,
        19 to BASE_RESOLUTION / 4
    )

    val LOW_RESOLUTION_ZOOM_TO_RESOLUTION = mapOf(
        10 to BASE_RESOLUTION * 8,
        11 to BASE_RESOLUTION * 4,
        12 to BASE_RESOLUTION * 2,
        13 to BASE_RESOLUTION,
        14 to BASE_RESOLUTION / 2,
        15 to BASE_RESOLUTION / 4,
        16 to BASE_RESOLUTION / 4,
        17 to BASE_RESOLUTION / 4,
        18 to BASE_RESOLUTION / 4,
        19 to BASE_RESOLUTION / 4
    )
}
