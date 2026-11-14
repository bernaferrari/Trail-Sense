package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.extensions.isClickable
import com.kylecorry.trail_sense.shared.map_layers.MapLayerBackgroundTask
import com.kylecorry.trail_sense.shared.map_layers.MapViewState
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceKeys
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IAsyncLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IFeatureClickableLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.OnGeoJsonFeatureClickListener
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.map.MapBroadcasts
import com.kylecorry.trail_sense.tools.paths.ui.PathBackgroundColor
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException

open class GeoJsonLayer<T : GeoJsonSource>(
    protected val source: T,
    override val layerId: String,
    useFastPathRendering: Boolean,
    private val minZoomLevel: Int? = null,
    private val taskRunner: MapLayerBackgroundTask = MapLayerBackgroundTask(),
    override val isTimeDependent: Boolean = false,
    private val refreshInterval: Duration? = null,
    private val refreshBroadcasts: List<String> = emptyList()
) : IAsyncLayer, IFeatureClickableLayer {
    val renderer = GeoJsonRenderer()
    @Volatile private var isStopped = false
    private var isInvalid = true

    @Volatile
    private var viewState: MapViewState? = null
    private var updateListener: (() -> Unit)? = null
    private val sourceCleanupTask = BackgroundTask {
        source.cleanup()
    }
    private var onFeatureClick: OnGeoJsonFeatureClickListener? = null
    protected var layerPreferences: MapLayerValues = MapLayerValues.Empty
    private var featureId: String? = null
    private var isWidget = false
    private val refreshTimer = refreshInterval?.let { CoroutineTimer { refresh() } }

    private var _timeOverride: Instant? = null
    private var _renderTime: Instant = Clock.System.now()
    override var isLoaded: Boolean = false

    override fun setTime(time: Instant?) {
        _timeOverride = time
        refresh()
    }

    protected fun refresh() {
        _renderTime = _timeOverride ?: Clock.System.now()
        isLoaded = false
        invalidate()
        notifyListeners()
    }

    override fun setFeatureFilter(id: String?) {
        featureId = id
        refresh()
    }

    private fun onRefreshBroadcastReceived(data: EventData): Boolean {
        refresh()
        return true
    }

    init {
        renderer.configureLineStringRenderer(shouldRenderWithDrawLines = useFastPathRendering)
        renderer.setOnClickListener(this::onClick)
        taskRunner.addTask { viewBounds, bounds, projection ->
            isInvalid = false
            val zoomLevel = projection.zoom.roundToInt()
            try {
                isLoaded = false
                if (minZoomLevel != null) {
                    if (zoomLevel < minZoomLevel) {
                        isLoaded = true
                        renderer.setGeoJsonObject(GeoJsonFeatureCollection(emptyList()))
                        return@addTask
                    }
                }

                val params = MapLayerValues.of(
                    MapLayerParams.PARAM_TIME to _renderTime.toEpochMilliseconds(),
                    MapLayerParams.PARAM_IS_WIDGET to isWidget,
                    MapLayerParams.PARAM_PREFERENCES to layerPreferences,
                    MapLayerParams.PARAM_FEATURE_ID to featureId
                )
                val obj =
                    source.load(bounds, zoomLevel, params) ?: GeoJsonFeatureCollection(
                        emptyList()
                    )
                if (isStopped) return@addTask
                renderer.setGeoJsonObject(obj)
                isLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                isInvalid = true
            }
        }
    }

    override fun setHasUpdateListener(listener: (() -> Unit)?) {
        updateListener = listener
        renderer.setHasUpdateListener(listener)
    }

    override fun setPreferences(preferences: MapLayerValues) {
        layerPreferences = preferences
        percentOpacity = preferences.getInt(
            MapLayerPreferenceKeys.OPACITY,
            MapLayerPreferenceKeys.DEFAULT_OPACITY
        ) / 100f
        // TODO: Eventually make this a geojson feature property instead of a global setting
        if (preferences.containsKey(MapLayerPreferenceKeys.BACKGROUND_COLOR)) {
            val backgroundColorId =
                preferences.getString(MapLayerPreferenceKeys.BACKGROUND_COLOR)?.toLongOrNull()
            renderer.configureLineStringRenderer(
                backgroundColor = PathBackgroundColor.entries.withId(backgroundColorId ?: 0)
                    ?: PathBackgroundColor.None
            )
        }
        if (preferences.containsKey(MapLayerPreferenceKeys.SHOW_LABELS)) {
            renderer.configureLineStringRenderer(
                shouldRenderLabels = preferences.getBoolean(
                    MapLayerPreferenceKeys.SHOW_LABELS,
                    true
                )
            )
        }
    }

    override fun draw(
        drawer: MapDrawer,
        map: IMapView
    ) {
        if (isStopped) return
        isWidget = map.isWidget
        renderer.draw(drawer, map)
    }

    override fun onViewChanged(state: MapViewState) {
        viewState = state
        renderer.onViewChanged(state)
        requestUpdate(state)
    }

    private fun requestUpdate(state: MapViewState) {
        if (isStopped) return
        taskRunner.scheduleUpdate(
            state.viewBounds,
            state.bounds,
            state.projection,
            isInvalid
        )
    }

    override fun drawOverlay(
        drawer: MapDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        isInvalid = true
        renderer.invalidate()
        viewState?.let { requestUpdate(it) }
    }

    override fun onClick(
        drawer: MapDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return renderer.onClick(drawer, map, pixel)
    }

    override fun setOnFeatureClickListener(listener: OnGeoJsonFeatureClickListener?) {
        onFeatureClick = listener
    }

    open fun onClick(feature: GeoJsonFeature): Boolean {
        if (onFeatureClick == null || !feature.isClickable()) {
            return false
        }
        onFeatureClick?.invoke(feature)
        return true
    }

    override fun start() {
        isStopped = false
        invalidate()
        ToolBroadcasts.subscribe(
            MapBroadcasts.GEOJSON_FEATURE_SELECTION_CHANGED,
            this::onSelectionBroadcast
        )
        refreshBroadcasts.forEach {
            ToolBroadcasts.subscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshInterval?.let { refreshTimer?.interval(it, it) }
    }

    override fun stop() {
        isStopped = true
        ToolBroadcasts.unsubscribe(
            MapBroadcasts.GEOJSON_FEATURE_SELECTION_CHANGED,
            this::onSelectionBroadcast
        )
        refreshBroadcasts.forEach {
            ToolBroadcasts.unsubscribe(it, this::onRefreshBroadcastReceived)
        }
        refreshTimer?.stop()
        taskRunner.stop()
        renderer.clear()
        sourceCleanupTask.start()
        isInvalid = true
        isLoaded = false
    }

    private fun onSelectionBroadcast(bundle: EventData) {
        val broadcastLayerId =
            bundle.getString(MapBroadcasts.PARAM_GEOJSON_LAYER_ID)
        if (broadcastLayerId == layerId) {
            val featureId = bundle.getString(MapBroadcasts.PARAM_GEOJSON_FEATURE_ID)
            renderer.setSelectedFeature(featureId)
        }
    }

    protected fun notifyListeners() {
        updateListener?.invoke()
    }

    override var percentOpacity: Float = 1f
}
