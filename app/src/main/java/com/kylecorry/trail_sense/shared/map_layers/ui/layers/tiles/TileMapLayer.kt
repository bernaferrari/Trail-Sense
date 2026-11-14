package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.content.Context
import android.os.Build
import androidx.compose.ui.graphics.asImageBitmap
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.errors.SafeMode
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.trail_sense.shared.concurrency.CustomDispatchers
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.MapViewState
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.tiles.ImageTile
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileLoader
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileMath
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileQueue
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

open class TileMapLayer<T : TileSource>(
    protected val source: T,
    override val layerId: String,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    private var minZoomLevel: Int? = null,
    private val shouldMultiply: Boolean = false,
    override val isTimeDependent: Boolean = false,
    private val refreshInterval: Duration? = null,
    private val refreshBroadcasts: List<String> = emptyList(),
    private val cacheKeys: List<String>? = null
) : IAsyncLayer {

    private val context: Context
        get() = getAppService()
    private val runtimeId = UUID.randomUUID().toString()
    private var _timeOverride: Instant? = null
    private var _renderTime: Instant = Clock.System.now()
    override var isLoaded: Boolean = false

    override fun setTime(time: Instant?) {
        _timeOverride = time
        refresh()
    }

    private var loader: TileLoader? = null
    private val queue = TileQueue()
    var multiplyAlpha: Int = 255
    private var updateListener: (() -> Unit)? = null
    private var zoomOffset: Int = 0
    private val tileRenderer = TileRenderer(TILE_BORDER_PIXELS, MAX_TILES)
    protected var layerPreferences: MapLayerValues = MapLayerValues.Empty
    private var featureId: String? = null
    private var wasOverTileLimit = false
    private val visibleTiles = VisibleTileCache(MAX_TILES)
    private var lastDesiredTiles: List<Tile>? = null

    private val loadTimer = CoroutineTimer {
        queue.load(maxOf(4, Runtime.getRuntime().availableProcessors()))
        isLoaded = queue.isEmpty()
    }
    private val refreshTimer = refreshInterval?.let { CoroutineTimer { refresh() } }

    private val sourceCleanupTask = BackgroundTask {
        source.cleanup()
    }

    private fun onRefreshBroadcastReceived(data: EventData) {
        refresh()
    }

    fun setZoomOffset(offset: Int) {
        zoomOffset = offset
    }

    fun setMinZoomLevel(level: Int) {
        minZoomLevel = level
    }

    init {
        taskRunner.addTask { _: Rectangle, _: CoordinateBounds, projection: IMapViewProjection ->
            queue.setMapProjection(projection)
        }
    }

    fun getCacheKey(): String? {
        if (cacheKeys == null) {
            return null
        }

        val keys = mutableListOf(layerId)
        for (key in cacheKeys) {
            @Suppress("DEPRECATION")
            val preference = layerPreferences.get(key)?.toString() ?: "null"
            keys.add(preference)
        }
        featureId?.let { keys.add(it) }
        return keys.joinToString("-")
    }

    override fun setFeatureFilter(id: String?) {
        featureId = id
        refresh()
    }

    override fun draw(drawer: MapDrawer, map: IMapView) {
        // Avoid drawing while in safe mode
        if (SafeMode.isEnabled()) {
            return
        }

        // Render loaded tiles
        if (shouldMultiply) {
            // Before Android 10 there is no separable multiply blend, so the layer is only made translucent
            drawer.withLayer(multiplyAlpha, multiply = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                renderTiles(drawer, map)
            }
        } else {
            renderTiles(drawer, map)
        }
    }

    override fun onViewChanged(state: MapViewState) {
        if (SafeMode.isEnabled()) {
            return
        }

        taskRunner.scheduleUpdate(
            state.viewBounds,
            state.bounds,
            state.projection
        )
    }

    override fun drawOverlay(drawer: MapDrawer, map: IMapView) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing, invalidation is handled separately
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override fun onClick(drawer: MapDrawer, map: IMapView, pixel: PixelCoordinate): Boolean {
        return false
    }

    private fun renderTiles(drawer: MapDrawer, map: IMapView) {
        val bounds = map.mapBounds
        val projection = map.mapProjection
        val desiredTiles = visibleTiles.get(bounds, projection.zoom.roundToInt(), zoomOffset)
        tileRenderer.prepare(projection)
        if (desiredTiles !== lastDesiredTiles) {
            queue.setDesiredTiles(desiredTiles)
            lastDesiredTiles = desiredTiles
        }
        if (desiredTiles.size <= MAX_TILES &&
            (desiredTiles.firstOrNull()?.z ?: 0) >= (minZoomLevel ?: 0)
        ) {
            loader?.loadTiles(
                desiredTiles,
                _renderTime,
                layerPreferences,
                featureId,
                map.isWidget,
                isHighDetailMode = map.isHighDetailMode,
                context = context
            )
        }

        // This runs every frame, so only log when the limit is first exceeded
        val isOverTileLimit = desiredTiles.size > MAX_TILES
        if (isOverTileLimit && !wasOverTileLimit) {
            getAppService<Logger>().debug(
                TAG,
                "Too many tiles to load for $layerId: ${desiredTiles.size} > $MAX_TILES (z${desiredTiles.firstOrNull()?.z})"
            )
        }
        wasOverTileLimit = isOverTileLimit

        // Tiles are drawn hard-edged so neighbors meet without seams, untinted, each with its own fade
        drawer.noTint()
        drawer.noSmooth()
        selectTilesForRendering(
            desiredTiles,
            get = { loader?.tileCache?.get(it) },
            peek = { loader?.tileCache?.peek(it) }
        ).forEach { renderTile ->
            renderTile.imageTile.withImage { bitmap ->
                bitmap ?: return@withImage
                tryOrLog {
                    tileRenderer.render(
                        renderTile.imageTile, bitmap.asImageBitmap(), drawer, projection, renderTile.clipTo
                    )
                }
            }
        }
        drawer.opacity(255)
        drawer.smooth()
        if (tileRenderer.hasFadingTiles) {
            notifyListeners()
        }
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
    }

    override fun start() {
        refreshTime()
        loader = TileLoader(
            source,
            queue,
            TILE_BORDER_PIXELS,
            tag = "$layerId-$runtimeId",
            key = getCacheKey()
        ) {
            notifyListeners()
        }
        loadTimer.interval(100)
        refreshBroadcasts.forEach {
            ToolBroadcasts.subscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshInterval?.let { refreshTimer?.interval(it, it) }
    }

    override fun stop() {
        loadTimer.stop()
        refreshBroadcasts.forEach {
            ToolBroadcasts.unsubscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshTimer?.stop()
        taskRunner.stop()
        loader?.clearCache()
        loader = null
        tileRenderer.clear()
        queue.clear()
        // TODO: This isn't the ideal place to do cleanup since garbage can build up. Likely need some sort of LRU cache for all intermediates.
        sourceCleanupTask.start()
    }

    fun refresh() {
        refreshTime()
        source.invalidate()
        loadTimer.stop()
        queue.clear()
        loader?.invalidateCache()
        isLoaded = false
        loadTimer.interval(100)
        invalidate()
        notifyListeners()
    }

    fun improveResolution(
        bounds: CoordinateBounds,
        zoom: Int,
        minimumTileCount: Int
    ) {
        var tileCount: Int
        var zoomOffset = -1
        do {
            zoomOffset++
            tileCount = TileMath.getTiles(bounds, zoom + zoomOffset).size
        } while (tileCount < minimumTileCount && zoomOffset < 10)

        setZoomOffset(zoomOffset)
        notifyListeners()
    }

    private fun refreshTime() {
        _renderTime = _timeOverride ?: Clock.System.now()
    }

    override fun setPreferences(preferences: MapLayerValues) {
        layerPreferences = preferences
        if (shouldMultiply) {
            multiplyAlpha = Interpolation.map(
                preferences.getInt(
                    DefaultMapLayerDefinitions.OPACITY,
                    DefaultMapLayerDefinitions.DEFAULT_OPACITY
                ) / 100f,
                0f,
                1f,
                0f,
                255f,
                shouldClamp = true
            ).toInt()
        } else {
            percentOpacity = preferences.getInt(
                DefaultMapLayerDefinitions.OPACITY,
                DefaultMapLayerDefinitions.DEFAULT_OPACITY
            ) / 100f
        }
    }

    override var percentOpacity: Float = 1f

    companion object {
        const val MAX_TILES = 150
        private const val TAG = "TileMapLayer"
        private const val TILE_BORDER_PIXELS = 2
        private val maxConcurrentLoads = minOf(4, Runtime.getRuntime().availableProcessors())
        private val tileLoadDispatcher = CustomDispatchers.newFixedThreadDispatcher(
            threads = maxConcurrentLoads,
            name = "TileMapLayer"
        )
    }
}
