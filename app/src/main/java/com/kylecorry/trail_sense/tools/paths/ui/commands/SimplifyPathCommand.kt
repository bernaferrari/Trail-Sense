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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLoadingIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplificationQuality
import com.kylecorry.trail_sense.tools.paths.domain.PathSimplifier
import com.kylecorry.trail_sense.tools.paths.map_layers.StaticLineGeoJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A path and its points, ready for the simplify preview. */
class SimplifyPathRequest(val path: Path, val points: List<PathPoint>)

/**
 * Simplifies a path: loads its points, hands them to [onPreview] so the screen can show [SimplifyPathDialog], then
 * [apply] removes the points for the quality picked there.
 */
class SimplifyPathCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pathService: IPathService,
    private val onPreview: (SimplifyPathRequest) -> Unit
) : IPathCommand {

    override fun execute(path: Path) {
        scope.launch {
            val loading = Alerts.loading(context, context.getString(R.string.loading))
            val points = withContext(Dispatchers.Default) {
                pathService.getWaypoints(path.id).sortedBy { it.id }
            }
            loading.dismiss()
            onPreview(SimplifyPathRequest(path, points))
        }
    }

    fun apply(path: Path, quality: PathSimplificationQuality) {
        scope.launch {
            val loading = Alerts.loading(context, context.getString(R.string.simplifying))
            val deleted = withContext(Dispatchers.IO) {
                pathService.simplifyPath(path.id, quality)
            }
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

/**
 * The simplify preview: the quality chips, the original path in grey under the simplified one, and how many points
 * would go. [onApply] runs with the chosen quality when it is confirmed.
 */
@Composable
fun SimplifyPathDialog(
    request: SimplifyPathRequest,
    onApply: (PathSimplificationQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val path = request.path
    val points = request.points
    val originalCoordinates = remember(request) { points.map { it.coordinate } }
    val simplifiedSource = remember(request) {
        StaticLineGeoJsonSource(emptyList(), path.style.color, thicknessScale = 0.75f)
    }
    val simplifiedLayer = remember(request) { GeoJsonLayer(simplifiedSource, SIMPLIFIED_LAYER_ID) }
    // The preview map is a rendering object, so it lives with the dialog.
    val map = remember(request) {
        MapView(context).apply {
            val originalLayer = GeoJsonLayer(
                StaticLineGeoJsonSource(originalCoordinates, Color.GRAY, thicknessScale = 1.5f),
                ORIGINAL_LAYER_ID
            ).also { it.percentOpacity = ORIGINAL_PATH_OPACITY }
            setLayers(listOf(originalLayer, simplifiedLayer))
        }
    }
    val simplified = remember(request) { mutableMapOf<PathSimplificationQuality, List<PathPoint>>() }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var removed by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun selectedQuality() = QUALITIES.getOrElse(selectedIndex) { QUALITIES[0] }.first

    DisposableEffect(map) {
        map.start()
        if (originalCoordinates.size >= 2) {
            map.doOnLayout { map.fitIntoView(CoordinateBounds.from(originalCoordinates)) }
        }
        onDispose {
            map.stop()
        }
    }

    LaunchedEffect(map, selectedIndex) {
        val quality = selectedQuality()
        isLoading = true
        val kept = simplified.getOrPut(quality) {
            withContext(Dispatchers.Default) { PathSimplifier.simplify(points, quality) }
        }
        removed = points.size - kept.size
        simplifiedSource.line = kept.map { it.coordinate }
        simplifiedLayer.invalidate()
        map.invalidate()
        isLoading = false
    }

    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.simplify),
        confirmText = stringResource(android.R.string.ok),
        onConfirm = {
            onDismiss()
            onApply(selectedQuality())
        },
        dismissText = stringResource(android.R.string.cancel)
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
                        selected = selectedIndex == index,
                        onClick = {
                            if (selectedIndex != index) {
                                selectedIndex = index
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
                if (isLoading) {
                    TrailSenseLoadingIndicator()
                }
            }
            Text(
                text = removed?.let { pluralStringResource(R.plurals.waypoints_to_be_deleted, it, it) } ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private val QUALITIES = listOf(
    PathSimplificationQuality.High to R.string.high,
    PathSimplificationQuality.Medium to R.string.moderate,
    PathSimplificationQuality.Low to R.string.low
)
private const val ORIGINAL_PATH_OPACITY = 50 / 255f
private const val ORIGINAL_LAYER_ID = "simplify_preview_original"
private const val SIMPLIFIED_LAYER_ID = "simplify_preview_simplified"
