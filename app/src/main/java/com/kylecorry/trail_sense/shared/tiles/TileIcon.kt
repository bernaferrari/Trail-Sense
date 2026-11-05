package com.kylecorry.trail_sense.shared.tiles

import android.graphics.Color
import android.graphics.drawable.Icon
import android.service.quicksettings.TileService
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.icons.toBitmap

/**
 * Draws [icon] onto this tile. Tiles have no icon resource in the manifest (the edit panel falls back to the app
 * icon), so each tile sets its Compose vector, white on transparent as the system expects, whenever it is shown.
 */
internal fun TileService.showIcon(icon: ImageVector) {
    tryOrLog {
        val tile = qsTile ?: return@tryOrLog
        tile.icon = Icon.createWithBitmap(icon.toBitmap(this, 24f, Color.WHITE))
        tile.updateTile()
    }
}
