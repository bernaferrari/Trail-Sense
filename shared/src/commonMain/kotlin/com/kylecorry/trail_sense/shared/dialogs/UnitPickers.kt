package com.kylecorry.trail_sense.shared.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.icons.trail.*
import kotlin.math.floor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString

/** Dialogs that ask for an amount in a unit the user picks (a distance, a pressure). */
object UnitPickers {

    fun distance(
        host: AppHost,
        units: List<DistanceUnits>,
        default: Distance? = null,
        title: String,
        showFeetAndInches: Boolean = false,
        allowNegative: Boolean = false,
        hint: String = getAppService<AppStrings>().get(Res.string.distance),
        description: String? = null,
        onDistancePick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        var distance: Distance? = default
        val distanceInput: @Composable () -> Unit = {
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
            host,
            title,
            content = description,
            body = distanceInput
        ) { cancelled ->
            if (cancelled) {
                onDistancePick.invoke(null, true)
            } else {
                onDistancePick.invoke(distance, false)
            }
        }
    }

    fun pressure(
        host: AppHost,
        title: String,
        message: CharSequence? = null,
        default: Pressure? = null,
        onPressurePick: (pressure: Pressure?) -> Unit
    ) {
        var selected = default
        val formatService = FormatService.getInstance()
        val view: @Composable () -> Unit = {
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
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        updateSelected()
                    },
                    label = appString(Res.string.pressure),
                    unit = formatService.getPressureUnitName(unit),
                    onUnitClick = {
                        Pickers.item(
                            host,
                            title,
                            PressureUnits.entries.map(formatService::getPressureUnitName),
                            PressureUnits.entries.indexOf(unit)
                        ) { idx ->
                            if (idx != null) {
                                unit = PressureUnits.entries[idx]
                                updateSelected()
                            }
                        }
                    }
                )
            }
        }

        Alerts.dialog(host, title, message, body = view) {
            if (!it) {
                onPressurePick(selected)
            } else {
                onPressurePick(null)
            }
        }
    }

    @Composable
    fun DistancePromptContent(
        value: Distance?,
        units: List<DistanceUnits>,
        hint: String,
        showFeetAndInches: Boolean,
        allowNegative: Boolean,
        modifier: Modifier = Modifier,
        onDistanceChange: (Distance?) -> Unit
    ) {
        val formatter = remember { FormatService.getInstance() }
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

        fun publishDistance() {
            onDistanceChange(
                mapDistance(amountText, secondaryAmountText, selectedUnit, showFeetAndInches, allowNegative)
            )
        }

        val pickUnit = {
            Pickers.item(
                AppHost.current(),
                "",
                displayUnits.map { it.longName },
                displayUnits.indexOfFirst { it.unit == selectedUnit }
            ) { idx ->
                if (idx != null) {
                    selectedUnit = displayUnits[idx].unit
                    publishDistance()
                }
            }
        }
        val unitName = displayUnits.firstOrNull { it.unit == selectedUnit }?.shortName ?: ""

        // The kit's field, with the unit as its trailing button; feet and inches are two fields side by side.
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    publishDistance()
                },
                label = if (showSecondaryAmount) appString(Res.string.unit_feet) else hint,
                keyboardType = KeyboardType.Number,
                unit = if (showSecondaryAmount) null else unitName,
                onUnitClick = if (showSecondaryAmount || displayUnits.isEmpty()) null else pickUnit,
                modifier = Modifier.weight(1f)
            )
            if (showSecondaryAmount) {
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                    value = secondaryAmountText,
                    onValueChange = {
                        secondaryAmountText = it
                        publishDistance()
                    },
                    label = appString(Res.string.unit_inches),
                    keyboardType = KeyboardType.Number,
                    unit = unitName,
                    onUnitClick = if (displayUnits.isEmpty()) null else pickUnit,
                    modifier = Modifier.weight(1f)
                )
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
}
