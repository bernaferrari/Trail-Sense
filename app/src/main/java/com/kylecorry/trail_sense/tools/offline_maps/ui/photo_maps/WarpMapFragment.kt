package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.dialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.views.PerspectiveCorrectionView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.fixPerspective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class WarpMapFragment : Fragment() {

    private val service = getAppService<OfflineMapService>()

    private var mapId = 0L
    private var map by mutableStateOf<PhotoMap?>(null)
    private var isPreview by mutableStateOf(false)
    private var perspectiveView: PerspectiveCorrectionView? = null

    private var onDone: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    WarpMapScreen(
                        map = map,
                        isPreview = isPreview,
                        onTogglePreview = ::togglePreview,
                        onInstructionsClick = ::showInstructions,
                        onNext = ::next,
                        onPerspectiveReady = { perspectiveView = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadMap()
    }

    override fun onDestroyView() {
        perspectiveView?.clearImage()
        perspectiveView = null
        super.onDestroyView()
    }

    private fun showInstructions() {
        dialog(
            getString(R.string.map_perspective_instructions),
            getString(R.string.crop_map_instructions_long),
            cancelText = null
        )
    }

    private fun togglePreview() {
        isPreview = !isPreview
    }

    private fun loadMap() {
        viewLifecycleOwner.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                service.getPhotoMap(mapId)
            }
            loaded?.let {
                onMapLoad(it)
            }
        }
    }


    private fun onMapLoad(map: PhotoMap) {
        this.map = map
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    private fun next() {
        val currentMap = map ?: return
        val perspective = perspectiveView ?: return
        val percentBounds = perspective.getPercentBounds() ?: return
        val hasChanges = perspective.hasChanges

        viewLifecycleOwner.lifecycleScope.launch {
            val loading = Alerts.loading(requireContext(), getString(R.string.saving))
            val saved = withContext(Dispatchers.IO) {
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

            loading.dismiss()
            if (saved) {
                perspective.clearImage()
                onDone.invoke()
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
    onPerspectiveReady: (PerspectiveCorrectionView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onTogglePreview,
                    modifier = Modifier.testTag("perspective_toggle_btn")
                ) {
                    Text(
                        text = stringResource(
                            if (isPreview) R.string.edit else R.string.preview
                        )
                    )
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .testTag("perspective"),
                factory = { context ->
                    PerspectiveCorrectionView(context).apply {
                        id = R.id.perspective
                        onPerspectiveReady(this)
                    }
                },
                update = { view ->
                    view.isPreview = isPreview
                    map?.let {
                        view.mapRotation = it.georeference.rotation
                        if (view.tag != it.filename) {
                            view.tag = it.filename
                            view.setImage(it.filename)
                        }
                    }
                }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(onClick = onInstructionsClick)
                            .testTag("instructions_text"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.map_perspective_instructions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(
                        onClick = onNext,
                        enabled = map != null,
                        modifier = Modifier.testTag("next_button")
                    ) {
                        Text(text = stringResource(R.string.next))
                    }
                }
            }
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
            onNext = {},
            onPerspectiveReady = {}
        )
    }
}
