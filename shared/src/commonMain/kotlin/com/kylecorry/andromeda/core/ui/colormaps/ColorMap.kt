package com.kylecorry.andromeda.core.ui.colormaps


interface ColorMap {
    /**
     * Get the color for a given percent [0, 1]
     */
    fun getColor(percent: Float): Int
}