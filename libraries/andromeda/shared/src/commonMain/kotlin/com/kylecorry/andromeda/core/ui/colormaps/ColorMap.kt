package com.kylecorry.andromeda.core.ui.colormaps


public interface ColorMap {
    /**
     * Get the color for a given percent [0, 1]
     */
    public fun getColor(percent: Float): Int
}