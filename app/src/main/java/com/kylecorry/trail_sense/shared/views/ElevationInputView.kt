package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A view that allows the user to enter an elevation
 */
class ElevationInputView(context: Context?, attrs: AttributeSet? = null) :
    ConstraintLayout(context ?: throw IllegalArgumentException("Context is required"), attrs) {

    private val sensorService by lazy { SensorService(getContext()) }
    private val altimeter by lazy { sensorService.getAltimeter() }
    private val location by lazy { LocationSubsystem.getInstance(getContext()) }
    private val formatter by lazy { FormatService.getInstance(getContext()) }

    /**
     * The entered elevation
     */
    var elevation: Distance?
        get() = elevationInput.value
        set(value) {
            elevationInput.value = value
        }

    /**
     * The hint for the elevation input
     */
    var hint: CharSequence?
        get() = elevationInput.hint
        set(value) {
            elevationInput.hint = value
        }

    private var changeListener: ((elevation: Distance?) -> Unit)? = null
    private var autofillListener: (() -> Unit)? = null

    private val gpsBtnState = ElevationInputButtonState()
    private lateinit var elevationInput: DistanceInputView
    private lateinit var gpsBtn: View

    init {
        context?.let {
            val iconSize = Resources.dp(it, 32f).toInt()
            val iconPadding = Resources.dp(it, 8f).toInt()
            gpsBtn = createElevationIconButton(it, gpsBtnState).apply {
                id = R.id.gps_btn
                elevation = Resources.dp(it, 4f)
            }
            addView(
                gpsBtn,
                LayoutParams(iconSize, iconSize).apply {
                    startToStart = LayoutParams.PARENT_ID
                    topToTop = R.id.elevation_input
                    bottomToBottom = R.id.elevation_input
                    marginEnd = iconPadding
                }
            )

            elevationInput = DistanceInputView(it).apply {
                id = R.id.elevation_input
            }
            addView(
                elevationInput,
                LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    startToEnd = R.id.gps_btn
                    endToEnd = LayoutParams.PARENT_ID
                    topToTop = LayoutParams.PARENT_ID
                    bottomToBottom = LayoutParams.PARENT_ID
                }
            )

            // Set up elevation input
            elevationInput.defaultHint = it.getString(R.string.elevation)
            elevationInput.hint = it.getString(R.string.elevation)
            elevationInput.showFeetAndInches = false
            elevationInput.allowNegative = true
            elevationInput.units = formatter.sortDistanceUnits(DistanceUtils.elevationDistanceUnits)
            elevationInput.setOnValueChangeListener { distance ->
                changeListener?.invoke(distance)
            }

            // Set up buttons
            gpsBtn.isVisible = true
            gpsBtnState.isLoading = false

            gpsBtn.setOnLongClickListener {
                autofillWithAltimeter()
                true
            }

            gpsBtn.setOnClickListener {
                Pickers.item(
                    getContext(), getContext().getString(R.string.autofill_source), listOf(
                        getContext().getString(R.string.pref_altimeter_calibration_title),
                        getContext().getString(R.string.beacon)
                    ), 0
                ) {
                    when (it) {
                        0 -> autofillWithAltimeter()
                        1 -> autofillWithBeacon()
                    }
                }
            }
        }
    }

    private fun autofillWithAltimeter() {
        autofillListener?.invoke()
        autofill()
    }

    private fun autofillWithBeacon() {
        CoroutineScope(Dispatchers.Main).launch {
            val beacon = BeaconPickers.pickBeacon(
                context, sort = ClosestBeaconSort(
                    BeaconService(context), location::location
                )
            ) { beacons ->
                beacons.filter { beacon ->
                    beacon is BeaconGroup || (beacon is Beacon && beacon.elevation != null)
                }
            } ?: return@launch
            changeElevation(beacon.elevation?.let { ele -> Distance.meters(ele) })
        }
    }

    private fun changeElevation(elevation: Distance?) {
        val converted = elevation?.convertTo(elevationInput.unit ?: DistanceUnits.Meters)
        val rounded = converted?.let {
            Distance.from(
                converted.value.roundPlaces(Units.getDecimalPlaces(converted.units)),
                converted.units
            )
        }
        elevationInput.value = rounded
    }

    private fun onAltimeterUpdate(): Boolean {
        changeElevation(Distance.meters(altimeter.altitude))
        gpsBtn.visibility = View.VISIBLE
        gpsBtn.isEnabled = true
        gpsBtnState.isLoading = false
        elevationInput.isEnabled = true
        return false
    }

    /**
     * Autofill the elevation from the altimeter
     */
    fun autofill() {
        if (!gpsBtn.isVisible || gpsBtnState.isLoading) return
        gpsBtn.isEnabled = false
        gpsBtnState.isLoading = true
        elevationInput.isEnabled = false
        altimeter.start(this::onAltimeterUpdate)
    }

    /**
     * Pause the altimeter if it is running
     */
    fun pause() {
        altimeter.stop(this::onAltimeterUpdate)
        gpsBtn.isVisible = true
        gpsBtn.isEnabled = true
        gpsBtnState.isLoading = false
        elevationInput.isEnabled = true
    }

    /**
     * Set the listener for when the user clicks the autofill button
     */
    fun setOnAutoElevationClickListener(listener: (() -> Unit)?) {
        autofillListener = listener
    }

    /**
     * Set the listener for when the elevation changes
     */
    fun setOnElevationChangeListener(listener: ((elevation: Distance?) -> Unit)?) {
        changeListener = listener
    }


}

private fun createElevationIconButton(
    context: Context,
    state: ElevationInputButtonState
): ComposeView {
    return ComposeView(context).apply {
        background = Resources.drawable(context, R.drawable.rounded_rectangle)
        setContent {
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
                            painter = painterResource(R.drawable.ic_altitude),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private class ElevationInputButtonState {
    var isLoading by mutableStateOf(false)
}
