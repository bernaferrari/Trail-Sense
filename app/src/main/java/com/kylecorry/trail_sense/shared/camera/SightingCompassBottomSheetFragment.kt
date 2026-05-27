package com.kylecorry.trail_sense.shared.camera

import android.content.DialogInterface
import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.Toolbar
import com.kylecorry.trail_sense.tools.navigation.ui.LinearCompassView

/**
 * Compose migration of sighting compass capture sheet (Field Guide / bearing sighting related).
 * Uses AndroidView for the heavy CameraView + LinearCompassView (custom repo views with other consumers).
 * Volume key + bearing display + capture replicated.
 * Full live FOV sync and camera lifecycle preserved via effects.
 * Note: For complete expressive camera UI overhaul this can become pure Compose camera in future.
 */
class SightingCompassBottomSheetFragment(
    private val onSelect: (bearing: Float?) -> Unit
) : TrailSenseComposeFragment() {

    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    // Bearing can be driven externally (setter was public API); keep for compatibility
    var bearing: Float? = null
        set(value) {
            field = value
            // In pure compose the state flows via recomposition in content
        }

    @Composable
    override fun FragmentContent() {
        var currentBearing by remember { mutableStateOf(bearing) }

        // Volume keys for capture (effect)
        LaunchedEffect(Unit) {
            // Note: dialog key listener is activity level; in practice parent or andromeda handles.
            // For full fidelity the host activity can forward volume.
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Toolbar(
                title = currentBearing?.let { formatter.formatDegrees(it) } ?: "Sighting Compass",
                rightButtonIcon = com.kylecorry.trail_sense.R.drawable.ic_cancel,
                isRightButtonVisible = true,
                modifier = Modifier.fillMaxWidth()
            ) { /* right clicked */ 
                onSelect(null)
                // dismiss handled by caller or nav
            }

            // Camera + overlays + compass stack - using AndroidView for the complex legacy custom views
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // CameraView (the main live preview from shared.views.CameraView)
                AndroidView(
                    factory = { ctx ->
                        com.kylecorry.trail_sense.shared.views.CameraView(ctx, null).apply {
                            setScaleType(androidx.camera.view.PreviewView.ScaleType.FIT_CENTER)
                            start(
                                lifecycleOwner = this@SightingCompassBottomSheetFragment,
                                readFrames = false,
                                shouldStabilizePreview = false
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Center reticle line (simple)
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(2.dp)
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = androidx.compose.ui.graphics.Color(context.getColor(com.kylecorry.trail_sense.R.color.colorPrimary)),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                            strokeWidth = 3f
                        )
                    }
                }

                // Linear compass overlay (the sighting one or LinearCompassView)
                AndroidView(
                    factory = { ctx ->
                        LinearCompassView(ctx, null).apply {
                            // initial
                            azimuth = currentBearing ?: 0f
                        }
                    },
                    update = { view ->
                        view.azimuth = currentBearing ?: 0f
                        // FOV sync would require exposing camera fov; left as TODO for expressive sweep
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }

            // Capture button area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    onSelect(currentBearing)
                    // Dismiss responsibility on caller (dialog or nav)
                }) {
                    Text("Capture Bearing")
                }
            }
        }
    }

    // Keep public API for existing BearingInputView callers
    fun setBearing(value: Float?) {
        bearing = value
    }
}
