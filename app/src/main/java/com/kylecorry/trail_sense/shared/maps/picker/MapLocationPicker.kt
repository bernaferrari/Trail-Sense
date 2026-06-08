package com.kylecorry.trail_sense.shared.maps.picker

import android.content.Context
import android.graphics.Color
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.map.ui.MapView

class MapLocationPicker {

    private val formatter = getAppService<FormatService>()

    fun pickLocation(
        context: Context,
        owner: LifecycleOwner,
        defaultLocation: Coordinate,
        userLocation: Coordinate? = null,
        onLocationPicked: (Coordinate?) -> Unit
    ) {
        val prompt = createPromptView(context)
        val view = prompt.view
        val mapView = prompt.map
        val locationView = prompt.location
        val zoomInButton = prompt.zoomIn
        val zoomOutButton = prompt.zoomOut
        zoomInButton.setOnClickListener { mapView.zoom(2f) }
        zoomOutButton.setOnClickListener { mapView.zoom(0.5f) }

        val manager = MapLocationPickerViewManager()
        var selectedLocation = defaultLocation
        manager.setSelectedLocation(defaultLocation)
        locationView.text = formatter.formatLocation(defaultLocation)

        val start = {
            manager.start(mapView, owner)
        }

        val stop = {
            manager.stop(mapView)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> {}
            }
        }

        mapView.backgroundColorOverride = Color.rgb(127, 127, 127)
        mapView.resolution = 2f
        // Rendering in widget mode prevents the map from showing the location marker as an arrow which may cause confusion
        mapView.isWidget = true
        userLocation?.let { mapView.userLocation = it }
        mapView.mapCenter = defaultLocation
        mapView.setOnSingleTapListener {
            selectedLocation = it
            manager.setSelectedLocation(it)
            locationView.text = formatter.formatLocation(it)
        }

        owner.lifecycle.addObserver(observer)

        Alerts.dialog(
            context,
            "",
            contentView = view
        ) { cancelled ->
            stop()
            owner.lifecycle.removeObserver(observer)
            if (cancelled) {
                onLocationPicked.invoke(null)
            } else {
                onLocationPicked.invoke(selectedLocation)
            }
        }
    }

    private data class PromptView(
        val view: ConstraintLayout,
        val map: MapView,
        val location: TextView,
        val zoomIn: MaterialButton,
        val zoomOut: MaterialButton
    )

    private fun createPromptView(context: Context): PromptView {
        val root = ConstraintLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val location = TextView(context).apply {
            id = R.id.location
            setPadding(
                Resources.dp(context, 16f).toInt(),
                Resources.dp(context, 16f).toInt(),
                Resources.dp(context, 16f).toInt(),
                0
            )
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        root.addView(
            location,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
        )

        val map = MapView(context).apply {
            id = R.id.map
        }
        root.addView(
            map,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                topToBottom = R.id.location
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = Resources.dp(context, 4f).toInt()
                dimensionRatio = "1:1.25"
            }
        )

        val zoomOut = createZoomButton(context, R.id.zoom_out_btn, R.drawable.ic_zoom_out)
        root.addView(
            zoomOut,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                endToEnd = R.id.map
                bottomToBottom = R.id.map
                marginEnd = Resources.dp(context, 4f).toInt()
                bottomMargin = Resources.dp(context, 4f).toInt()
            }
        )

        val zoomIn = createZoomButton(context, R.id.zoom_in_btn, R.drawable.ic_add)
        root.addView(
            zoomIn,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = R.id.zoom_out_btn
                bottomToTop = R.id.zoom_out_btn
            }
        )

        return PromptView(root, map, location, zoomIn, zoomOut)
    }

    private fun createZoomButton(context: Context, id: Int, icon: Int): MaterialButton {
        return MaterialButton(context).apply {
            this.id = id
            alpha = 0.86f
            isClickable = true
            isFocusable = true
            gravity = Gravity.CENTER
            this.icon = androidx.core.content.ContextCompat.getDrawable(context, icon)
            iconTint = ColorStateList.valueOf(Resources.androidTextColorPrimary(context))
        }
    }

}
