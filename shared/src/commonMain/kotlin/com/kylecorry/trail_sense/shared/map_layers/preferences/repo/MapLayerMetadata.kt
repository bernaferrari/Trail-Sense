package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

enum class MapLayerType {
    Overlay,
    Feature,
    Tile
}

data class MapLayerAttribution(
    /**
     * The attribution text shown on the map / preference page. Should be short - ideally just the name of the copyright holder.
     * Markdown supported.
     */
    val attribution: String,
    /**
     * The long attribution text shown on preferences page.
     * Markdown supported.
     */
    val longAttribution: String? = null,
    /**
     * If true it will show up on the map.
     */
    val alwaysShow: Boolean = false
)
