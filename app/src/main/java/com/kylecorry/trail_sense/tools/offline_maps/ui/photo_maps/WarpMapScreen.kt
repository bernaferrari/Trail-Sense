@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentBounds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.PerspectiveCorrection
import com.kylecorry.trail_sense.shared.views.PerspectiveCorrectionState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.rememberPerspectiveCorrectionState
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.fixPerspective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun WarpMapToolScreen(
    mapId: Long,
    viewModelKey: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = viewModel(key = viewModelKey) {
        WarpMapViewModel(getAppService(), FileSubsystem.getInstance(context.applicationContext), mapId)
    }
    val perspective = rememberPerspectiveCorrectionState()
    val currentOnComplete by rememberUpdatedState(onComplete)

    DisposableEffect(perspective) {
        onDispose { perspective.clearImage() }
    }

    // Saving can take a moment (the image is warped), so it shows the app's loading dialog meanwhile
    DisposableEffect(viewModel.isSaving) {
        val loading = if (viewModel.isSaving) {
            Alerts.loading(context, context.getString(R.string.saving))
        } else {
            null
        }
        onDispose { loading?.dismiss() }
    }

    LaunchedEffect(viewModel) {
        viewModel.saved.collect {
            perspective.clearImage()
            currentOnComplete()
        }
    }

    WarpMapScreen(
        map = viewModel.map,
        isPreview = viewModel.isPreview,
        perspective = perspective,
        onTogglePreview = viewModel::togglePreview,
        onInstructionsClick = {
            Alerts.dialog(
                context,
                context.getString(R.string.map_perspective_instructions),
                context.getString(R.string.crop_map_instructions_long),
                cancelText = null
            )
        },
        onNext = {
            val percentBounds = perspective.getPercentBounds()
            if (percentBounds != null) {
                viewModel.save(percentBounds, perspective.hasChanges)
            }
        },
        modifier = modifier
    )
}

internal class WarpMapViewModel(
    private val service: MapService,
    private val files: FileSubsystem,
    private val mapId: Long
) : ViewModel() {

    var map by mutableStateOf<PhotoMap?>(null)
        private set
    var isPreview by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set

    private val _saved = Channel<Unit>(Channel.BUFFERED)

    /** Sent once the corrected map is saved. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        viewModelScope.launch {
            map = withContext(Dispatchers.IO) {
                service.getPhotoMap(mapId)
            }
        }
    }

    fun togglePreview() {
        isPreview = !isPreview
    }

    fun save(percentBounds: PercentBounds, hasChanges: Boolean) {
        val currentMap = map ?: return
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    if (hasChanges) {
                        val bitmap = files.bitmap(currentMap.filename) ?: return@withContext false
                        val bounds =
                            percentBounds.toPixelBounds(bitmap.width.toFloat(), bitmap.height.toFloat())
                        val warped = bitmap.fixPerspective(bounds, true, Color.WHITE)
                        try {
                            files.save(currentMap.filename, warped, recycleOnSave = true)
                        } catch (e: IOException) {
                            return@withContext false
                        }

                        // Delete the pdf file if it exists
                        files.delete(currentMap.pdfFileName)
                    }

                    service.add(
                        currentMap.copy(
                            georeference = currentMap.georeference.copy(isWarpingCompleted = true)
                        )
                    )
                    true
                }
            } finally {
                isSaving = false
            }

            if (saved) {
                _saved.send(Unit)
            }
        }
    }
}

@Composable
private fun WarpMapScreen(
    map: PhotoMap?,
    isPreview: Boolean,
    onTogglePreview: () -> Unit,
    onInstructionsClick: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    perspective: PerspectiveCorrectionState = rememberPerspectiveCorrectionState()
) {

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Instruction card (tonal), tappable for full instructions
            TrailSenseCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = TrailSenseSpacing.screen,
                        end = TrailSenseSpacing.screen,
                        top = TrailSenseSpacing.screen
                    )
                    .testTag("instructions_text"),
                onClick = onInstructionsClick
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                ) {
                    Icon(
                        imageVector = TrailIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.map_perspective_instructions),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Preview / edit toggle (tonal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                TrailSenseButton(
                    text = stringResource(if (isPreview) R.string.edit else R.string.preview),
                    onClick = onTogglePreview,
                    style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
                    modifier = Modifier.testTag("perspective_toggle_btn")
                )
            }

            PerspectiveCorrection(
                state = perspective,
                imagePath = map?.filename,
                mapRotation = map?.georeference?.rotation ?: 0f,
                isPreview = isPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(TrailSenseSpacing.screen)
                    .testTag("perspective")
            )

            // Primary action (full-width)
            TrailSenseButton(
                text = stringResource(R.string.next),
                onClick = onNext,
                enabled = map != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.screen)
                    .testTag("next_button")
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarpMapScreenPreview() {
    TrailSenseTheme {
        WarpMapScreen(
            map = null,
            isPreview = false,
            onTogglePreview = {},
            onInstructionsClick = {},
            onNext = {}
        )
    }
}
