package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/** Android bitmap ownership for the shared tile loading policy. */
class ImageTile(
    key: String,
    tile: Tile,
    image: Bitmap? = null,
    state: TileState = TileState.Idle,
    shouldFadeIn: Boolean = true,
    owner: String = "",
    errorRetryDelayMillis: Long = 10_000L,
    loadFunction: (suspend () -> Bitmap?)?
) : SharedImageTile<Bitmap>(
    key, tile, BitmapTileImageStore(image), state, shouldFadeIn, owner, errorRetryDelayMillis, loadFunction
)

private class BitmapTileImageStore(initial: Bitmap?) : TileImageStore<Bitmap> {
    private val lock = ReentrantReadWriteLock()
    @Volatile private var image = initial

    override fun hasImage(): Boolean = image != null

    override fun replace(image: Bitmap?) = lock.write {
        this.image?.recycle()
        this.image = image
    }

    override fun withImage(block: (Bitmap?) -> Unit) {
        val readLock = lock.readLock()
        if (!readLock.tryLock()) {
            block(null)
            return
        }
        try {
            block(image)
        } finally {
            readLock.unlock()
        }
    }

    override fun clear(afterClear: () -> Unit) = lock.write {
        image?.recycle()
        image = null
        afterClear()
    }
}
