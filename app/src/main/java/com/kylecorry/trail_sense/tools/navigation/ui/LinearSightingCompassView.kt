package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.system.Resources
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.fragment.app.findFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.views.CameraView
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
    private val viewCamera: CameraView
    private val viewCameraLine: View
    private val linearCompass: LinearCompassView
    private val sightingCompassButton: FloatingActionButton

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
        viewCamera = CameraView(context, null).apply {
            id = R.id.view_camera
            keepScreenOn = true
            isVisible = false
        }
        addView(
            viewCamera,
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

        sightingCompassButton = FloatingActionButton(context).apply {
            id = R.id.sighting_compass_btn
            size = FloatingActionButton.SIZE_MINI
            contentDescription = context.getString(R.string.sighting_compass)
            isClickable = true
            isFocusable = true
            setImageResource(R.drawable.ic_camera)
            imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
            backgroundTintList = ColorStateList.valueOf(
                Resources.getAndroidColorAttr(context, android.R.attr.colorBackgroundFloating)
            )
        }
        addView(
            sightingCompassButton,
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

        CustomUiUtils.setButtonState(sightingCompassButton, false)
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
            CustomUiUtils.setButtonState(sightingCompassButton, false)
            updateTimer.stop()
        }
    }

    private fun setSightingCompass(shouldShow: Boolean) {
        showSightingCompass = shouldShow
        if (!shouldShow) {
            sightingCompass.stop()
            CustomUiUtils.setButtonState(sightingCompassButton, false)
        } else if (!sightingCompass.isRunning()) {
            CustomUiUtils.setButtonState(sightingCompassButton, true)
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
