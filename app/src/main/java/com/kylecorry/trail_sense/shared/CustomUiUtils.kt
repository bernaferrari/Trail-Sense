package com.kylecorry.trail_sense.shared

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.qr.ui.QR_SHARE_MAX_LENGTH
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconGroup
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.floor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CustomUiUtils {
    @ColorInt
    fun getQualityColor(quality: Quality): Int {
        return when (quality) {
            Quality.Poor -> AppColor.Red.color
            Quality.Unknown -> AppColor.Gray.color
            Quality.Moderate -> AppColor.Yellow.color
            Quality.Good -> AppColor.Green.color
        }
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
        var distance: Distance? = default
        val distanceInput = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            DistancePromptContent(
                value = default,
                units = units,
                hint = hint,
                showFeetAndInches = showFeetAndInches,
                allowNegative = allowNegative,
                modifier = Modifier.padding(16.dp)
            ) {
                distance = it
            }
        }

        Alerts.dialog(
            context,
            title,
            content = description,
            contentView = distanceInput
        ) { cancelled ->
            if (cancelled) {
                onDistancePick.invoke(null, true)
            } else {
                onDistancePick.invoke(distance, false)
            }
        }
    }

    fun pickElevation(
        context: Context,
        default: Distance? = null,
        title: String,
        hint: String = context.getString(R.string.elevation),
        onElevationPick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        var elevation: Distance? = default
        val elevationInput = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
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
            contentView = elevationInput
        ) { cancelled ->
            if (cancelled) {
                onElevationPick.invoke(null, true)
            } else {
                onElevationPick.invoke(elevation, false)
            }
        }
    }

    @Composable
    private fun DistancePromptContent(
        value: Distance?,
        units: List<DistanceUnits>,
        hint: String,
        showFeetAndInches: Boolean,
        allowNegative: Boolean,
        modifier: Modifier = Modifier,
        onDistanceChange: (Distance?) -> Unit
    ) {
        val context = LocalContext.current
        val formatter = remember(context) { FormatService.getInstance(context) }
        val initialUnit = value?.units?.takeIf { units.contains(it) } ?: units.firstOrNull()
        var selectedUnit by remember(units) { mutableStateOf(initialUnit) }
        var amountText by remember { mutableStateOf(formatNumber(value?.let { getDistanceAmount(it, showFeetAndInches) })) }
        var secondaryAmountText by remember { mutableStateOf(formatNumber(value?.let { getDistanceSecondaryAmount(it, showFeetAndInches) })) }

        LaunchedEffect(value?.value, value?.units, showFeetAndInches) {
            selectedUnit = value?.units?.takeIf { units.contains(it) } ?: selectedUnit ?: units.firstOrNull()
            amountText = formatNumber(value?.let { getDistanceAmount(it, showFeetAndInches) })
            secondaryAmountText = formatNumber(value?.let { getDistanceSecondaryAmount(it, showFeetAndInches) })
            onDistanceChange(mapDistance(amountText, secondaryAmountText, selectedUnit, showFeetAndInches, allowNegative))
        }

        val showSecondaryAmount = showFeetAndInches && selectedUnit == DistanceUnits.Feet
        val displayUnits = remember(units, formatter) {
            units.map {
                DistanceDisplayUnit(
                    it,
                    formatter.getDistanceUnitName(it, true),
                    formatter.getDistanceUnitName(it)
                )
            }
        }

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    onDistanceChange(
                        mapDistance(
                            amountText,
                            secondaryAmountText,
                            selectedUnit,
                            showFeetAndInches,
                            allowNegative
                        )
                    )
                },
                label = { Text(if (showSecondaryAmount) context.getString(R.string.unit_feet) else hint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            if (showSecondaryAmount) {
                OutlinedTextField(
                    value = secondaryAmountText,
                    onValueChange = {
                        secondaryAmountText = it
                        onDistanceChange(
                            mapDistance(
                                amountText,
                                secondaryAmountText,
                                selectedUnit,
                                showFeetAndInches,
                                allowNegative
                            )
                        )
                    },
                    label = { Text(context.getString(R.string.unit_inches)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            TextButton(
                onClick = {
                    Pickers.item(
                        context,
                        "",
                        displayUnits.map { it.longName },
                        displayUnits.indexOfFirst { it.unit == selectedUnit }
                    ) { idx ->
                        if (idx != null) {
                            selectedUnit = displayUnits[idx].unit
                            onDistanceChange(
                                mapDistance(
                                    amountText,
                                    secondaryAmountText,
                                    selectedUnit,
                                    showFeetAndInches,
                                    allowNegative
                                )
                            )
                        }
                    }
                },
                enabled = displayUnits.isNotEmpty()
            ) {
                Text(displayUnits.firstOrNull { it.unit == selectedUnit }?.shortName ?: "")
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
        val sensorService = remember(context) { SensorService(context) }
        val altimeter = remember(sensorService) { sensorService.getAltimeter() }
        val location = remember(context) { LocationSubsystem.getInstance(context) }
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
                                    context,
                                    sort = ClosestBeaconSort(
                                        BeaconService(context),
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
                        painter = painterResource(R.drawable.ic_altitude),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            DistancePromptContent(
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

    private data class DistanceDisplayUnit(
        val unit: DistanceUnits,
        val shortName: String,
        val longName: String
    )

    private fun formatNumber(value: Number?): String {
        return value?.let { DecimalFormatter.format(it, 5, false) } ?: ""
    }

    private fun getDistanceAmount(value: Distance, showFeetAndInches: Boolean): Number {
        return if (showFeetAndInches && value.units == DistanceUnits.Feet) {
            floor(value.value)
        } else {
            value.value
        }
    }

    private fun getDistanceSecondaryAmount(value: Distance, showFeetAndInches: Boolean): Number? {
        if (!showFeetAndInches || value.units != DistanceUnits.Feet) {
            return null
        }
        val inches = (value.value % 1f) * 12
        return if (inches == 0f) null else inches
    }

    private fun mapDistance(
        amountText: String,
        secondaryAmountText: String,
        unit: DistanceUnits?,
        showFeetAndInches: Boolean,
        allowNegative: Boolean
    ): Distance? {
        val amount = amountText.toFloatCompat()
        if (amount == null || unit == null) {
            return null
        }
        if (!allowNegative && amount < 0f) {
            return null
        }
        val value = if (showFeetAndInches && unit == DistanceUnits.Feet) {
            amount + (secondaryAmountText.toFloatCompat() ?: 0f) / 12f
        } else {
            amount
        }
        return Distance.from(value, unit)
    }

    fun pickDate(
        context: Context,
        date: LocalDate = LocalDate.now(),
        dayViewDecorator: AndromedaDayViewDecorator? = null,
        onDatePick: (date: LocalDate?) -> Unit
    ) {
        Pickers.date(context, date, onDatePick)
    }

    fun pickTime(
        context: Context,
        use24Hours: Boolean,
        default: LocalTime = LocalTime.now(),
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        Pickers.time(context, use24Hours, default, onTimePick)
    }

    fun oneTimeToast(
        context: Context,
        message: String,
        shownKey: String,
        short: Boolean = true
    ) {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        if (prefs.getBoolean(shownKey) != true) {
            Alerts.toast(context, message, short)
            prefs.putBoolean(shownKey, true)
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
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        if (prefs.getBoolean(shownKey) != shownValue) {
            if (considerShownIfCancelled && cancelText == null) {
                Alerts.dialog(context, title, message, okText = okText, cancelText = null, allowLinks = true) {
                    prefs.putBoolean(shownKey, shownValue)
                    onClose(it, true)
                }
            } else {
                Alerts.dialog(
                    context,
                    title,
                    message,
                    okText = okText,
                    cancelText = cancelText,
                    allowLinks = true
                ) { cancelled ->
                    val agreed = !cancelled || considerShownIfCancelled
                    if (agreed) {
                        prefs.putBoolean(shownKey, shownValue)
                    }
                    onClose(cancelled, agreed)
                }
            }
        } else {
            onClose(false, true)
        }
    }

    fun setImageColor(textView: android.widget.TextView, @ColorInt color: Int?) {
        textView.compoundDrawables.forEach {
            it?.let { Colors.setImageColor(it, color) }
        }
    }

    fun snackbar(
        fragment: TrailSenseDestinationHost,
        text: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ): Snackbar {
        return fragment.showSnackbar(text, duration, action, onAction)
    }

    fun showQR(
        fragment: TrailSenseDestinationHost,
        title: String,
        qr: String
    ) {
        showQR(fragment.requireContext(), fragment.findNavController(), title, qr)
    }

    fun showQR(
        context: Context,
        navController: NavController,
        title: String,
        qr: String
    ) {
        if (qr.length > QR_SHARE_MAX_LENGTH) {
            Alerts.toast(context, context.getString(R.string.qr_text_too_long))
        }
        navController.navigate(
            R.id.viewQrFragment,
            Bundle().apply {
                putString("qr_title", title)
                putString("qr_text", qr.take(QR_SHARE_MAX_LENGTH))
            }
        )
    }

    suspend fun takePhoto(
        fragment: TrailSenseDestinationHost,
        size: Size? = null
    ): Uri? =
        suspendCoroutine { cont ->
            takePhoto(fragment, size) {
                cont.resume(it)
            }
        }

    private fun takePhoto(
        fragment: TrailSenseDestinationHost,
        size: Size? = null,
        onCapture: (uri: Uri?) -> Unit
    ) {
        fragment.requestCamera {
            if (!it) {
                onCapture(null)
                return@requestCamera
            }

            val prefs = UserPreferences(fragment.requireContext()).camera
            fragment.showPhotoImport(size, prefs.useZeroShutterLag) { uri ->
                onCapture(uri)
            }
        }
    }

    fun pickDatetime(
        context: Context,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        pickDate(context, default.toLocalDate()) { date ->
            if (date != null) {
                Pickers.time(context, use24Hours, default.toLocalTime()) { time ->
                    if (time != null) {
                        onDatetimePick(LocalDateTime.of(date, time))
                    } else {
                        onDatetimePick(null)
                    }
                }
            } else {
                onDatetimePick(null)
            }
        }
    }

    fun pickPressure(
        context: Context,
        title: String,
        message: CharSequence? = null,
        default: Pressure? = null,
        onPressurePick: (pressure: Pressure?) -> Unit
    ) {
        var selected = default
        val formatService = FormatService.getInstance(context)
        val view = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            var amount by remember { mutableStateOf(default?.value?.toString().orEmpty()) }
            var unit by remember { mutableStateOf(default?.units ?: PressureUnits.Hpa) }

            fun updateSelected() {
                selected = amount.toFloatCompat()?.let {
                    if (it < 0f) {
                        null
                    } else {
                        Pressure.from(it, unit)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        updateSelected()
                    },
                    label = { Text(context.getString(R.string.pressure)) },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        Pickers.item(
                            context,
                            title,
                            PressureUnits.entries.map(formatService::getPressureUnitName),
                            PressureUnits.entries.indexOf(unit)
                        ) { idx ->
                            if (idx != null) {
                                unit = PressureUnits.entries[idx]
                                updateSelected()
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(formatService.getPressureUnitName(unit))
                }
            }
        }

        Alerts.dialog(context, title, message, contentView = view) {
            if (!it) {
                onPressurePick(selected)
            } else {
                onPressurePick(null)
            }
        }
    }

    fun showComposeContentDialog(
        context: Context,
        title: String,
        content: CharSequence? = null,
        composeContent: @Composable () -> Unit
    ) {
        val view = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (content != null) {
                    Text(
                        text = content.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
                composeContent()
            }
        }
        Alerts.dialog(
            context,
            title,
            contentView = view,
            cancelText = null
        )
    }

    fun Resources.getPrimaryColor(context: Context): Int {
        return try {
            getAndroidColorAttr(context, androidx.appcompat.R.attr.colorPrimary)
        } catch (e: Exception) {
            e.printStackTrace()
            AppColor.Orange.color
        }
    }

    fun Resources.getColorOnPrimary(context: Context): Int {
        return try {
            getAndroidColorAttr(context, com.google.android.material.R.attr.colorOnPrimary)
        } catch (e: Exception) {
            e.printStackTrace()
            Color.WHITE
        }
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
