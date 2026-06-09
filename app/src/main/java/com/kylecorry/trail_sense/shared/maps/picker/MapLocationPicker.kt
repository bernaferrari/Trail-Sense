package com.kylecorry.trail_sense.shared.maps.picker

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
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
        val prompt = createPromptView(context, defaultLocation)
        val view = prompt.view
        val mapView = prompt.map

        val manager = MapLocationPickerViewManager()
        var selectedLocation = defaultLocation
        manager.setSelectedLocation(defaultLocation)

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
            prompt.selectedLocation = it
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

    private class PromptState(selectedLocation: Coordinate) {
        var selectedLocation by mutableStateOf(selectedLocation)
    }

    private data class PromptView(
        val view: View,
        val map: MapView,
        val state: PromptState
    ) {
        var selectedLocation: Coordinate
            get() = state.selectedLocation
            set(value) {
                state.selectedLocation = value
            }
    }

    private fun createPromptView(context: Context, defaultLocation: Coordinate): PromptView {
        val map = MapView(context).apply {
            id = R.id.map
        }
        val state = PromptState(defaultLocation)
        val view = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = formatter.formatLocation(state.selectedLocation),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .aspectRatio(1f / 1.25f)
                ) {
                    map.Content(Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        IconButton(onClick = { map.zoom(2f) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = { map.zoom(0.5f) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_zoom_out),
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return PromptView(
            view = view,
            map = map,
            state = state
        )
    }

}
