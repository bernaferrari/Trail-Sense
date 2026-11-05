package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLoadingIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplifier
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.map_layers.StaticLineGeoJsonSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SimplifyPathCommand(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val pathService: IPathService = PathService.getInstance(context)
) : IPathCommand {

    override fun execute(path: Path) {
        lifecycleOwner.inBackground {
            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, context.getString(R.string.loading))
            }
            val points = withContext(Dispatchers.Default) {
                pathService.getWaypoints(path.id).sortedBy { it.id }
            }
            withContext(Dispatchers.Main) {
                loading.dismiss()
                showPreview(path, points)
            }
        }
    }

    private fun showPreview(path: Path, points: List<PathPoint>) {
        val map = MapView(context)

        val originalCoordinates = points.map { it.coordinate }
        val originalLayer = GeoJsonLayer(
            StaticLineGeoJsonSource(originalCoordinates, Color.GRAY, thicknessScale = 1.5f),
            ORIGINAL_LAYER_ID
        ).also { it.percentOpacity = ORIGINAL_PATH_OPACITY }
        val simplifiedSource =
            StaticLineGeoJsonSource(emptyList(), path.style.color, thicknessScale = 0.75f)
        val simplifiedLayer = GeoJsonLayer(simplifiedSource, SIMPLIFIED_LAYER_ID)

        val selectedIndex = mutableIntStateOf(0)
        val removedText = mutableStateOf("")
        val isLoading = mutableStateOf(false)

        fun selectedQuality() = QUALITIES.getOrElse(selectedIndex.intValue) { QUALITIES[0] }.first

        val simplified = mutableMapOf<PathSimplificationQuality, List<PathPoint>>()
        var updateJob: Job? = null

        fun update() {
            val quality = selectedQuality()
            updateJob?.cancel()
            updateJob = lifecycleOwner.lifecycleScope.launch {
                isLoading.value = true
                val kept = simplified.getOrPut(quality) {
                    withContext(Dispatchers.Default) { PathSimplifier.simplify(points, quality) }
                }
                val removed = points.size - kept.size
                simplifiedSource.line = kept.map { it.coordinate }
                simplifiedLayer.invalidate()
                map.invalidate()
                removedText.value = context.resources.getQuantityString(
                    R.plurals.waypoints_to_be_deleted,
                    removed,
                    removed
                )
                isLoading.value = false
            }
        }

        val content = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)) {
                Text(
                    text = stringResource(R.string.quality),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    QUALITIES.forEachIndexed { index, (_, label) ->
                        TrailSenseChip(
                            text = stringResource(label),
                            selected = selectedIndex.intValue == index,
                            onClick = {
                                if (selectedIndex.intValue != index) {
                                    selectedIndex.intValue = index
                                    update()
                                }
                            }
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(dimensionResource(R.dimen.rounded_corner_size)))
                        .background(colorResource(R.color.colorSecondary))
                ) {
                    map.Content(Modifier.fillMaxSize())
                    if (isLoading.value) {
                        TrailSenseLoadingIndicator()
                    }
                }
                Text(
                    text = removedText.value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        map.setLayers(listOf(originalLayer, simplifiedLayer))
        map.start()
        if (originalCoordinates.size >= 2) {
            map.doOnLayout { map.fitIntoView(CoordinateBounds.from(originalCoordinates)) }
        }
        update()

        Alerts.dialog(
            context,
            context.getString(R.string.simplify),
            contentView = content
        ) { cancelled ->
            updateJob?.cancel()
            map.stop()
            if (!cancelled) {
                applySimplification(path, selectedQuality())
            }
        }
    }

    private fun applySimplification(path: Path, quality: PathSimplificationQuality) {
        lifecycleOwner.inBackground {
            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, context.getString(R.string.simplifying))
            }
            val deleted = withContext(Dispatchers.IO) {
                pathService.simplifyPath(path.id, quality)
            }
            withContext(Dispatchers.Main) {
                loading.dismiss()
                Alerts.toast(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.waypoints_deleted,
                        deleted,
                        deleted
                    )
                )
            }
        }
    }

    companion object {
        private val QUALITIES = listOf(
            PathSimplificationQuality.High to R.string.high,
            PathSimplificationQuality.Medium to R.string.moderate,
            PathSimplificationQuality.Low to R.string.low
        )
        private const val ORIGINAL_PATH_OPACITY = 50 / 255f
        private const val ORIGINAL_LAYER_ID = "simplify_preview_original"
        private const val SIMPLIFIED_LAYER_ID = "simplify_preview_simplified"
    }
}
