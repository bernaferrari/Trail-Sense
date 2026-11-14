package com.kylecorry.andromeda.core.math

import com.kylecorry.sol.math.geometry.Size

/** Scales this size to fit within [bounds], preserving its aspect ratio. */
public fun Size.scaleToFit(bounds: Size): Size {
    require(width > 0f && height > 0f && bounds.width >= 0f && bounds.height >= 0f)
    val scale = minOf(bounds.width / width, bounds.height / height)
    return Size(width * scale, height * scale)
}
