package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.util.Size

/** The tile size as Android's size, for bitmap operations. */
fun TileSize.toSize(): Size = Size(width, height)
