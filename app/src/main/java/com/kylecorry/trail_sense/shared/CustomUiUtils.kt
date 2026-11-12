package com.kylecorry.trail_sense.shared

import com.kylecorry.trail_sense.shared.dialogs.UnitPickers
import com.kylecorry.trail_sense.shared.dialogs.oneTimeSnackbar

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.util.Size
import androidx.annotation.ColorInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.QualityColors
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.from
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.dialogs.date
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.dialogs.disclaimer
import com.kylecorry.trail_sense.shared.dialogs.item
import com.kylecorry.trail_sense.shared.dialogs.snackbar
import com.kylecorry.trail_sense.shared.dialogs.time
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.time.nowLocal
import com.kylecorry.trail_sense.shared.time.today
import com.kylecorry.trail_sense.shared.views.TrailSenseColors
import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.useDynamicColors
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

object CustomUiUtils {
    /** Snackbar lengths for [snackbar], the same values as Material's Snackbar.LENGTH_SHORT and LENGTH_LONG. */
    const val SNACKBAR_SHORT = -1
    const val SNACKBAR_LONG = 0

    @ColorInt
    fun getQualityColor(quality: Quality): Int {
        return QualityColors.of(quality)
    }

    fun pickDistance(
        context: Context,
        units: List<DistanceUnits>,
        default: Distance? = null,
        title: String,
        showFeetAndInches: Boolean = false,
        allowNegative: Boolean = false,
        hint: String = context.getString(R.string.distance),
        description: String? = null,
        onDistancePick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        UnitPickers.distance(
            AppHost.from(context),
            units,
            default,
            title,
            showFeetAndInches,
            allowNegative,
            hint,
            description,
            onDistancePick
        )
    }

    fun pickElevation(
        context: Context,
        default: Distance? = null,
        title: String,
        hint: String = context.getString(R.string.elevation),
        onElevationPick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        var elevation: Distance? = default
        val elevationInput: @Composable () -> Unit = {
            ElevationPromptContent(
                default = default,
                hint = hint,
                modifier = Modifier.padding(16.dp)
            ) {
                elevation = it
            }
        }

        Alerts.dialog(
            context,
            title,
            body = elevationInput
        ) { cancelled ->
            if (cancelled) {
                onElevationPick.invoke(null, true)
            } else {
                onElevationPick.invoke(elevation, false)
            }
        }
    }

    @Composable
    private fun ElevationPromptContent(
        default: Distance?,
        hint: String,
        modifier: Modifier = Modifier,
        onElevationChange: (Distance?) -> Unit
    ) {
        val context = LocalContext.current
        val formatter = remember(context) { FormatService.getInstance(context) }
        val sensorService = remember(context) { getAppService<SensorService>() }
        val altimeter = remember(sensorService) { sensorService.getAltimeter() }
        val location = remember(context) { getAppService<ILocationSubsystem>() }
        val scope = rememberCoroutineScope()
        val units = remember(formatter) {
            formatter.sortDistanceUnits(DistanceUtils.elevationDistanceUnits)
        }
        var inputValue by remember { mutableStateOf(default) }
        var selectedElevation by remember { mutableStateOf(default) }
        var isLoading by remember { mutableStateOf(false) }

        fun changeElevation(elevation: Distance?) {
            val targetUnit = selectedElevation?.units ?: inputValue?.units ?: units.firstOrNull() ?: DistanceUnits.Meters
            val converted = elevation?.convertTo(targetUnit)
            val rounded = converted?.let {
                Distance.from(
                    converted.value.roundPlaces(Units.getDecimalPlaces(converted.units)),
                    converted.units
                )
            }
            inputValue = rounded
            selectedElevation = rounded
            onElevationChange(rounded)
        }

        val altimeterCallback = {
            changeElevation(Distance.meters(altimeter.altitude))
            isLoading = false
            false
        }

        fun autofillWithAltimeter() {
            if (isLoading) return
            isLoading = true
            altimeter.start(altimeterCallback)
        }

        DisposableEffect(altimeter) {
            onDispose {
                altimeter.stop(altimeterCallback)
            }
        }

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    Pickers.item(
                        context,
                        context.getString(R.string.autofill_source),
                        listOf(
                            context.getString(R.string.pref_altimeter_calibration_title),
                            context.getString(R.string.beacon)
                        ),
                        0
                    ) {
                        when (it) {
                            0 -> autofillWithAltimeter()
                            1 -> scope.launch {
                                val beacon = BeaconPickers.pickBeacon(
                                    AppHost.from(context),
                                    sort = ClosestBeaconSort(
                                        getAppService<IBeaconService>(),
                                        location::location
                                    )
                                ) { beacons ->
                                    beacons.filter { beacon ->
                                        beacon is BeaconGroup || (beacon is Beacon && beacon.elevation != null)
                                    }
                                } ?: return@launch
                                changeElevation(beacon.elevation?.let { elevation -> Distance.meters(elevation) })
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.size(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = TrailIcons.Altitude,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            UnitPickers.DistancePromptContent(
                value = inputValue,
                units = units,
                hint = hint,
                showFeetAndInches = false,
                allowNegative = true,
                modifier = Modifier.weight(1f)
            ) {
                selectedElevation = it
                onElevationChange(it)
            }
        }
    }


    fun pickDate(
        context: Context,
        date: LocalDate = today(),
        onDatePick: (date: LocalDate?) -> Unit
    ) {
        Pickers.date(context, date, onDatePick)
    }

    fun pickTime(
        context: Context,
        use24Hours: Boolean,
        default: LocalTime = nowLocal().time,
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        Pickers.time(context, use24Hours, default, onTimePick)
    }

    fun oneTimeSnackbar(
        context: Context,
        message: String,
        shownKey: String,
        short: Boolean = true
    ) {
        Alerts.oneTimeSnackbar(
            AppHost.from(context),
            message,
            shownKey,
            short,
            PreferencesSubsystem.getInstance(context).preferences
        )
    }

    fun confirmDiscardChanges(context: Context, onDiscard: () -> Unit) {
        Alerts.dialog(
            context,
            context.getString(R.string.unsaved_changes),
            context.getString(R.string.unsaved_changes_message),
            okText = context.getString(R.string.dialog_leave)
        ) { cancelled ->
            if (!cancelled) {
                onDiscard()
            }
        }
    }

    fun disclaimer(
        context: Context,
        title: String,
        message: CharSequence,
        shownKey: String,
        okText: String = context.getString(android.R.string.ok),
        cancelText: String? = context.getString(android.R.string.cancel),
        considerShownIfCancelled: Boolean = true,
        shownValue: Boolean = true,
        onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
    ) {
        Alerts.disclaimer(
            AppHost.from(context),
            title,
            message,
            shownKey,
            okText = okText,
            cancelText = cancelText,
            considerShownIfCancelled = considerShownIfCancelled,
            shownValue = shownValue,
            preferences = PreferencesSubsystem.getInstance(context).preferences,
            onClose = onClose
        )
    }

    /** A snackbar on [host]'s screen; [onAction] runs when its action is pressed. */
    fun snackbar(
        host: AndroidDestinationHost,
        text: String,
        duration: Int = SNACKBAR_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ) {
        host.lifecycleOwner.lifecycleScope.launch {
            if (host.showSnackbar(text, action, long = duration == SNACKBAR_LONG)) {
                onAction()
            }
        }
    }

    fun showQR(
        host: AndroidDestinationHost,
        title: String,
        qr: String
    ) {
        showQR(host.context, host.navigator, title, qr)
    }

    fun showQR(
        context: Context,
        appNavigator: TrailSenseNavigator,
        title: String,
        qr: String
    ) {
        com.kylecorry.trail_sense.tools.qr.ui.showQR(AppHost.from(context), appNavigator, title, qr)
    }

    /** Asks for the camera, then takes or picks a photo; null when refused or cancelled. */
    suspend fun takePhoto(
        host: AndroidDestinationHost,
        size: Size? = null
    ): Uri? {
        if (!host.requestPermission(AppPermission.Camera)) return null
        val zeroShutterLag = UserPreferences(host.context).camera.useZeroShutterLag
        return host.capturePhoto(size?.width, size?.height, zeroShutterLag)?.let(Uri::parse)
    }

    fun pickDatetime(
        context: Context,
        use24Hours: Boolean,
        default: LocalDateTime = nowLocal(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        Pickers.datetime(AppHost.from(context), use24Hours, default, onDatetimePick)
    }

    fun pickPressure(
        context: Context,
        title: String,
        message: CharSequence? = null,
        default: Pressure? = null,
        onPressurePick: (pressure: Pressure?) -> Unit
    ) {
        UnitPickers.pressure(AppHost.from(context), title, message, default, onPressurePick)
    }

    fun showComposeContentDialog(
        context: Context,
        title: String,
        content: CharSequence? = null,
        composeContent: @Composable () -> Unit
    ) {
        com.kylecorry.trail_sense.shared.dialogs.showComposeContentDialog(
            AppHost.from(context),
            title,
            content,
            composeContent
        )
    }

    /** The theme's primary color, for drawing outside composition; composables use MaterialTheme.colorScheme. */
    fun Resources.getPrimaryColor(context: Context): Int {
        return TrailSenseColors.current.primary.toArgb()
    }

    fun Resources.getColorOnPrimary(context: Context): Int {
        return TrailSenseColors.current.onPrimary.toArgb()
    }

    fun Resources.getCardinalDirectionColor(context: Context): Int {
        return getPrimaryMarkerColor(context)
    }

    fun Resources.getPrimaryMarkerColor(context: Context): Int {
        val prefs = UserPreferences(context)
        return if (prefs.useDynamicColors && prefs.useDynamicColorsOnCompass) {
            getPrimaryColor(context)
        } else {
            AppColor.Orange.color
        }
    }

    fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

}
