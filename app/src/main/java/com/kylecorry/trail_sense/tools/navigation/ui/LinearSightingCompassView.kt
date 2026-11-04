package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.andromeda.core.system.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

class LinearSightingCompassView(private val context: Context) : ICompassView {

    val isCameraActive: Boolean
        get() = sightingCompass.isRunning()

    private var showSightingCompass = false
    private var isStarted = false
    private var isResumed = false
    private val lock = Any()
    private val viewCamera: CameraState
    private val linearCompass: LinearCompassView
    private val sightingCompassButton: SightingCompassButton
    var requestCameraPermission: (((Boolean) -> Unit) -> Unit)? = null
    var onCameraPermissionDenied: (() -> Unit)? = null
    private var compassInvisible by mutableStateOf(false)
    private var clickListener by mutableStateOf<(() -> Unit)?>(null)

    private val sightingCompass by lazy {
        SightingCompassView(
            viewCamera,
            linearCompass
        ) {
            // Forward the click event from the sighting compass
            clickListener?.invoke()
        }
    }

    // TODO: Make this more efficient (only update when the camera changes)
    private val updateTimer = CoroutineTimer {
        sightingCompass.update()
    }

    private val lifecycleListener = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                resume()
            }

            Lifecycle.Event.ON_PAUSE -> {
                pause()
            }

            else -> {
                // Do nothing
            }
        }
    }

    private val isCompassVisible: Boolean
        get() = !compassInvisible

    var isInvisible: Boolean
        get() = compassInvisible
        set(value) {
            compassInvisible = value
            if (value) {
                stop()
            } else if (isResumed) {
                start()
            }
        }

    init {
        viewCamera = CameraState(context).apply {
            keepScreenOn = true
            isVisible = false
        }
        linearCompass = LinearCompassView(context)
        sightingCompassButton = SightingCompassButton()

        sightingCompassButton.setState(false)
        sightingCompassButton.onClick = {
            setSightingCompass(!showSightingCompass)
        }
    }

    /**
     * Called when the view is resumed or attached
     */
    private fun resume() {
        synchronized(lock) {
            if (isResumed) {
                return
            }
            isResumed = true
            showSightingCompass = false
        }
        if (isCompassVisible) {
            start()
        }
    }

    /**
     * Called when the view is paused or detached
     */
    private fun pause() {
        synchronized(lock) {
            if (!isResumed) {
                return
            }
            isResumed = false
        }
        stop()
    }

    /**
     * Called when the view becomes visible
     */
    private fun start() {
        synchronized(lock) {
            if (isStarted) {
                return
            }
            isStarted = true
            setSightingCompass(showSightingCompass)
        }
    }

    /**
     * Called when the view becomes invisible
     */
    private fun stop() {
        synchronized(lock) {
            if (!isStarted) {
                return
            }
            isStarted = false
            sightingCompass.stop()
            linearCompass.range = 180f
            sightingCompassButton.setState(false)
            updateTimer.stop()
        }
    }

    private fun setSightingCompass(shouldShow: Boolean) {
        showSightingCompass = shouldShow
        if (!shouldShow) {
            sightingCompass.stop()
            linearCompass.range = 180f
            sightingCompassButton.setState(false)
            updateTimer.stop()
        } else if (!sightingCompass.isRunning()) {
            sightingCompassButton.setState(true)
            val permissionRequester = requestCameraPermission ?: { onResult: (Boolean) -> Unit ->
                onResult(false)
            }
            permissionRequester { hasPermission ->
                if (hasPermission && isStarted) {
                    sightingCompass.start()
                    sightingCompass.update()
                    if (sightingCompass.isRunning()) {
                        updateTimer.interval(250)
                    }
                } else {
                    if (!hasPermission) {
                        onCameraPermissionDenied?.invoke()
                    }
                    setSightingCompass(false)
                }
            }
        }
    }

    fun setOnClickListener(listener: (() -> Unit)?) {
        clickListener = listener
    }

    override var compassCenter: Coordinate
        get() = linearCompass.compassCenter
        set(value) {
            linearCompass.compassCenter = value
        }
    override var useTrueNorth: Boolean
        get() = linearCompass.useTrueNorth
        set(value) {
            linearCompass.useTrueNorth = value
        }
    override var declination: Float
        get() = linearCompass.declination
        set(value) {
            linearCompass.declination = value
        }
    override var azimuth: Float
        get() = linearCompass.azimuth
        set(value) {
            linearCompass.azimuth = value
        }

    override fun addCompassLayer(layer: ICompassLayer) {
        linearCompass.addCompassLayer(layer)
    }

    override fun removeCompassLayer(layer: ICompassLayer) {
        linearCompass.removeCompassLayer(layer)
    }

    override fun setCompassLayers(layers: List<ICompassLayer>) {
        linearCompass.setCompassLayers(layers)
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        linearCompass.draw(reference, size)
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        linearCompass.draw(bearing, stopAt)
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val lifecycle = lifecycleOwner.lifecycle
            lifecycle.addObserver(lifecycleListener)
            resume()
            onDispose {
                lifecycle.removeObserver(lifecycleListener)
                pause()
            }
        }

        if (!isCompassVisible) {
            return
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable { clickListener?.invoke() }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                CameraPreview(
                    state = viewCamera,
                    modifier = Modifier.fillMaxSize()
                )
                if (viewCamera.isVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1.dp)
                            .height(200.dp)
                            .background(ComposeColor(Resources.getPrimaryColor(context)))
                    )
                }
                linearCompass.Content(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
            sightingCompassButton.Content(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
            )
        }
    }

}

private class SightingCompassButton {
    private var isOn by mutableStateOf(false)
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val background = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val foreground = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        androidx.compose.material3.Surface(
            modifier = modifier
                .size(40.dp)
                .clickable { onClick?.invoke() },
            shape = androidx.compose.foundation.shape.CircleShape,
            color = background,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = TrailIcons.Camera,
                    contentDescription = stringResource(R.string.sighting_compass),
                    tint = foreground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
