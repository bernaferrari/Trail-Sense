package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.isVisible
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class MapDistanceSheetState(context: Context) {

    private val formatter = FormatService.getInstance(context)

    var view: View? = null
    var cancelListener: (() -> Unit)? = null
    var undoListener: (() -> Unit)? = null
    var createPathListener: (() -> Unit)? = null

    private var isVisible by mutableStateOf(false)
    private var distanceText by mutableStateOf("")

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
            onUndo = { undoListener?.invoke() },
            onCancel = { cancelListener?.invoke() },
            onCreatePath = { createPathListener?.invoke() },
            modifier = modifier
        )
    }
}

@Composable
private fun MapDistanceSheetContent(
    distance: String,
    onUndo: () -> Unit,
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
                IconButton(onClick = onUndo) {
                    Icon(
                        painter = painterResource(R.drawable.ic_undo),
                        contentDescription = stringResource(R.string.undo)
                    )
                }
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
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = stringResource(android.R.string.cancel)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onCreatePath,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(text = stringResource(R.string.create_path))
                }
            }
            Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))
        }
    }
}
