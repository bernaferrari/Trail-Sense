package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.domain.PathEditState

class MapDistanceSheetState(context: Context) {

    private val formatter = FormatService.getInstance(context)

    var view: View? = null
    var cancelListener: (() -> Unit)? = null
    var undoListener: (() -> Unit)? = null
    var redoListener: (() -> Unit)? = null
    var createPathListener: (() -> Unit)? = null

    private var isVisible by mutableStateOf(false)
    private var distanceText by mutableStateOf("")
    private var canUndo by mutableStateOf(false)
    private var canRedo by mutableStateOf(false)
    private var hasSelection by mutableStateOf(false)

    fun setEditState(state: PathEditState) {
        canUndo = state.canUndo
        canRedo = state.canRedo
        hasSelection = state.hasSelection
    }

    fun setDistance(distance: Distance) {
        distanceText =
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
    }

    fun show() {
        isVisible = true
        view?.isVisible = true
    }

    fun hide() {
        isVisible = false
        view?.isVisible = false
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) {
            return
        }
        MapDistanceSheetContent(
            distance = distanceText,
            canUndo = canUndo,
            canRedo = canRedo,
            hint = stringResource(
                if (hasSelection) R.string.path_edit_hint_selected else R.string.path_edit_hint
            ),
            onUndo = { undoListener?.invoke() },
            onRedo = { redoListener?.invoke() },
            onCancel = { cancelListener?.invoke() },
            onCreatePath = { createPathListener?.invoke() },
            modifier = modifier
        )
    }
}

@Composable
private fun MapDistanceSheetContent(
    distance: String,
    canUndo: Boolean,
    canRedo: Boolean,
    hint: String,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCancel: () -> Unit,
    onCreatePath: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(
            topStart = TrailSenseSpacing.xl,
            topEnd = TrailSenseSpacing.xl
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = TrailSenseSpacing.xxs
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    top = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.md
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = TrailSenseSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.distance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = distance,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = TrailIcons.Cancel,
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }
            }

            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TrailSenseSpacing.md)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        imageVector = TrailIcons.Undo,
                        contentDescription = stringResource(R.string.undo)
                    )
                }
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        imageVector = TrailIcons.Redo,
                        contentDescription = stringResource(R.string.redo)
                    )
                }
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.create_path),
                    onClick = onCreatePath,
                    modifier = Modifier.padding(start = TrailSenseSpacing.lg)
                )
            }
            Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))
        }
    }
}
