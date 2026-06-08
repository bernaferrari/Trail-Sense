package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.maps.picker.MapLocationPicker
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

class CoordinateInputView(context: Context?, attrs: AttributeSet? = null) :
    ConstraintLayout(context ?: throw IllegalArgumentException("Context is required"), attrs) {

    private val formatService by lazy { FormatService.getInstance(getContext()) }
    private val sensorService by lazy { SensorService(getContext()) }
    lateinit var gps: IGPS

    private val errorHandler = CoroutineTimer {
        locationEdit.error = getContext().getString(R.string.coordinate_input_invalid_location)
    }

    var coordinate: Coordinate?
        get() = _coordinate
        set(value) {
            _coordinate = value
            if (value == null) {
                locationEdit.setText("")
            } else {
                val formatted = formatService.formatLocation(value)
                locationEdit.setText(formatted)
            }
        }
    private var _coordinate: Coordinate? = null

    var required: Boolean = false
        set(value) {
            field = value
            if (value) {
                locationEditHolder.hint =
                    context.getString(R.string.field_required, context.getString(R.string.location))
            } else {
                locationEditHolder.hint = context.getString(R.string.location)
            }
        }

    private var changeListener: ((coordinate: Coordinate?) -> Unit)? = null
    private var beaconListener: ((beacon: Beacon) -> Unit)? = null
    private var autofillListener: (() -> Unit)? = null

    private val mapPicker = MapLocationPicker()
    private lateinit var locationEdit: TextInputEditText
    private lateinit var locationEditHolder: TextInputLayout
    private val gpsBtnState = CoordinateInputButtonState()
    private lateinit var gpsBtn: View
    private lateinit var helpBtn: View

    init {
        context?.let {
            val iconSize = Resources.dp(it, 32f).toInt()
            val iconPadding = Resources.dp(it, 8f).toInt()
            gpsBtn = createCoordinateIconButton(
                it,
                id = R.id.gps_btn,
                icon = R.drawable.ic_location,
                showBackground = true,
                state = gpsBtnState
            ).apply {
                elevation = Resources.dp(it, 4f)
            }
            addView(
                gpsBtn,
                LayoutParams(iconSize, iconSize).apply {
                    startToStart = LayoutParams.PARENT_ID
                    topToTop = R.id.location_edit_holder
                    bottomToBottom = R.id.location_edit_holder
                    marginEnd = iconPadding
                }
            )

            locationEditHolder = TextInputLayout(it).apply {
                id = R.id.location_edit_holder
                hint = it.getString(R.string.location)
            }
            locationEdit = TextInputEditText(it).apply {
                id = R.id.utm
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
            locationEditHolder.addView(
                locationEdit,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                locationEditHolder,
                LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    startToEnd = R.id.gps_btn
                    endToStart = R.id.coordinate_input_help_btn
                    topToTop = LayoutParams.PARENT_ID
                    bottomToBottom = LayoutParams.PARENT_ID
                }
            )

            helpBtn = createCoordinateIconButton(
                it,
                id = R.id.coordinate_input_help_btn,
                icon = R.drawable.ic_help,
                showBackground = false,
                state = CoordinateInputButtonState()
            )
            addView(
                helpBtn,
                LayoutParams(iconSize, iconSize).apply {
                    endToEnd = LayoutParams.PARENT_ID
                    topToTop = R.id.location_edit_holder
                    bottomToBottom = R.id.location_edit_holder
                }
            )

            gps = sensorService.getGPS()

            gpsBtn.visibility = View.VISIBLE
            gpsBtnState.isLoading = false

            locationEdit.addTextChangedListener {
                onChange()
            }

            helpBtn.setOnClickListener {
                Alerts.dialog(
                    getContext(),
                    getContext().getString(R.string.location_input_help_title),
                    getContext().getString(R.string.location_input_help),
                    cancelText = null
                )
            }

            gpsBtn.setOnClickListener {
                val owner = findViewTreeLifecycleOwner()

                Pickers.item(
                    context,
                    getContext().getString(R.string.autofill_source),
                    listOfNotNull(
                        getContext().getString(R.string.gps),
                        getContext().getString(R.string.beacon),
                        if (owner != null) context.getString(R.string.map) else null
                    ),
                    0
                ) {
                    when (it) {
                        0 -> {
                            autofillListener?.invoke()
                            autofill()
                        }

                        1 -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                val beacon = BeaconPickers.pickBeacon(
                                    context,
                                    sort = ClosestBeaconSort(BeaconService(context), gps::location)
                                ) ?: return@launch
                                coordinate = beacon.coordinate
                                beaconListener?.invoke(beacon)
                            }
                        }

                        2 -> {
                            if (owner == null) return@item
                            mapPicker.pickLocation(
                                context,
                                owner,
                                coordinate ?: gps.location,
                                userLocation = gps.location,
                                onLocationPicked = { newLocation ->
                                    if (newLocation != null) {
                                        coordinate = newLocation
                                        onChange()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            gpsBtn.setOnLongClickListener {
                autofillListener?.invoke()
                autofill()
                true
            }
        }
    }

    private fun onGPSUpdate(): Boolean {
        coordinate = gps.location
        gpsBtn.visibility = View.VISIBLE
        gpsBtn.isEnabled = true
        gpsBtnState.isLoading = false
        locationEdit.isEnabled = true
        return false
    }

    private fun onChange() {
        val locationText = locationEdit.text.toString()
        _coordinate = Coordinate.parse(locationText)
        errorHandler.stop()
        if (_coordinate == null && locationText.isNotEmpty()) {
            errorHandler.once(Duration.ofSeconds(2))
        } else {
            locationEdit.error = null
        }
        changeListener?.invoke(_coordinate)
    }

    fun pause() {
        gps.stop(this::onGPSUpdate)
        gpsBtn.visibility = View.VISIBLE
        gpsBtn.isEnabled = true
        gpsBtnState.isLoading = false
        locationEdit.isEnabled = true
        errorHandler.stop()
    }

    fun autofill() {
        if (!gpsBtn.isVisible || gpsBtnState.isLoading) return
        gpsBtn.isEnabled = false
        gpsBtnState.isLoading = true
        locationEdit.isEnabled = false
        gps.start(this::onGPSUpdate)
    }

    fun setOnAutoLocationClickListener(listener: (() -> Unit)?) {
        autofillListener = listener
    }

    fun setOnCoordinateChangeListener(listener: ((coordinate: Coordinate?) -> Unit)?) {
        changeListener = listener
    }

    fun setOnBeaconSelectedListener(listener: ((beacon: Beacon) -> Unit)?) {
        beaconListener = listener
    }

}

private fun createCoordinateIconButton(
    context: Context,
    id: Int,
    icon: Int,
    showBackground: Boolean,
    state: CoordinateInputButtonState
): CoordinateInputButtonView {
    return CoordinateInputButtonView(context, id, icon, showBackground, state)
}

private class CoordinateInputButtonView(
    context: Context,
    id: Int,
    private val icon: Int,
    showBackground: Boolean,
    private val state: CoordinateInputButtonState
) : AbstractComposeView(context) {

    init {
        this.id = id
        if (showBackground) {
            background = Resources.drawable(context, R.drawable.rounded_rectangle)
        }
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private class CoordinateInputButtonState {
    var isLoading by mutableStateOf(false)
}
