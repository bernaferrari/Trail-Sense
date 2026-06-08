package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import com.kylecorry.andromeda.core.system.Resources
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.findFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

class LinearSightingCompassView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs), ICompassView {

    val isCameraActive: Boolean
        get() = sightingCompass.isRunning()

    private var showSightingCompass = false
    private var isStarted = false
    private var isResumed = false
    private val lock = Any()
    private val viewCamera: CameraState
    private val viewCameraLine: View
    private val linearCompass: LinearCompassView
    private val sightingCompassButton: SightingCompassButton

    private val sightingCompass by lazy {
        SightingCompassView(
            viewCamera,
            viewCameraLine,
            linearCompass
        ) {
            // Forward the click event from the sighting compass
            callOnClick()
        }
    }

    private val hooks = Hooks()

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

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        hooks.effect("visibility", isVisible) {
            if (isVisible) {
                start()
            } else {
                stop()
            }
        }
    }

    init {
        viewCamera = CameraState(context).apply {
            keepScreenOn = true
            isVisible = false
        }
        val viewCameraPreview = ComposeView(context).apply {
            id = R.id.view_camera
            setContent {
                TrailSenseTheme {
                    CameraPreview(
                        state = viewCamera,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        addView(
            viewCameraPreview,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.circular_display_size)
            ).apply {
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        viewCameraLine = View(context).apply {
            id = R.id.view_camera_line
            setBackgroundColor(Resources.getPrimaryColor(context))
            isVisible = false
        }
        addView(
            viewCameraLine,
            LayoutParams(
                Resources.dp(context, 1f).toInt(),
                0
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = R.id.view_camera
                bottomToBottom = R.id.view_camera
            }
        )

        linearCompass = LinearCompassView(context, null).apply {
            id = R.id.linear_compass
        }
        addView(
            linearCompass,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                Resources.dp(context, 100f).toInt()
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = R.id.view_camera
                bottomToBottom = R.id.view_camera
            }
        )

        sightingCompassButton = SightingCompassButton(context).apply {
            id = R.id.sighting_compass_btn
            contentDescription = context.getString(R.string.sighting_compass)
            isClickable = true
            isFocusable = true
        }
        addView(
            sightingCompassButton.view,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = R.id.linear_compass
                endToEnd = LayoutParams.PARENT_ID
                topMargin = Resources.dp(context, 20f).toInt()
                marginEnd = Resources.dp(context, 16f).toInt()
            }
        )

        sightingCompassButton.setState(false)
        sightingCompassButton.setOnClickListener {
            setSightingCompass(!showSightingCompass)
        }

        doOnAttach {
            val observer = findViewTreeLifecycleOwner()
            observer?.lifecycle?.addObserver(lifecycleListener)
            resume()
        }

        doOnDetach {
            val observer = findViewTreeLifecycleOwner()
            observer?.lifecycle?.removeObserver(lifecycleListener)
            pause()
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
            viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        }
        if (isVisible) {
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
            viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
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
            updateTimer.interval(20)
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
            sightingCompassButton.setState(false)
            updateTimer.stop()
        }
    }

    private fun setSightingCompass(shouldShow: Boolean) {
        showSightingCompass = shouldShow
        if (!shouldShow) {
            sightingCompass.stop()
            sightingCompassButton.setState(false)
        } else if (!sightingCompass.isRunning()) {
            sightingCompassButton.setState(true)
            val fragment = findFragment<AndromedaFragment>()
            fragment.requestCamera { hasPermission ->
                if (hasPermission) {
                    sightingCompass.start()
                } else {
                    fragment.alertNoCameraPermission()
                    setSightingCompass(false)
                }
            }
        }
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

}

private class SightingCompassButton(context: Context) {
    private var isOn by mutableStateOf(false)

    val view = ComposeView(context).apply {
        setContent {
            TrailSenseTheme {
                Content()
            }
        }
    }

    var id: Int
        get() = view.id
        set(value) {
            view.id = value
        }

    var contentDescription: CharSequence?
        get() = view.contentDescription
        set(value) {
            view.contentDescription = value
        }

    var isClickable: Boolean
        get() = view.isClickable
        set(value) {
            view.isClickable = value
        }

    var isFocusable: Boolean
        get() = view.isFocusable
        set(value) {
            view.isFocusable = value
        }

    fun setOnClickListener(listener: View.OnClickListener?) {
        view.setOnClickListener(listener)
    }

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    private fun Content() {
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
            modifier = Modifier.size(40.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = background,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
