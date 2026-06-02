package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import kotlin.math.roundToInt

@Composable
fun ScopeAdjustmentPage(
    modifier: Modifier = Modifier
) {
    val formatter = remember { AppServiceRegistry.get<FormatService>() }
    val prefs = remember { AppServiceRegistry.get<UserPreferences>() }
    val referenceDistance = remember { Distance.from(100f, DistanceUnits.Yards) }
    var offsetX by remember { mutableStateOf(Distance.from(0f, DistanceUnits.Inches)) }
    var offsetY by remember { mutableStateOf(Distance.from(0f, DistanceUnits.Inches)) }
    var offsetDirectionX by remember { mutableStateOf(ScopeDirection.Left) }
    var offsetDirectionY by remember { mutableStateOf(ScopeDirection.Up) }
    val (distanceToTarget, setDistanceToTarget) = rememberDistancePreference("cache-ballistics-sight-in-range")
    val (distancePerClick, setDistancePerClick) = rememberDistancePreference("cache-ballistics-scope-click-adjustment")

    val clickAmounts = remember {
        listOf(
            R.string.moa_amount to "1/8" to Distance.from(0.125f, DistanceUnits.Inches),
            R.string.moa_amount to "1/4" to Distance.from(0.25f, DistanceUnits.Inches),
            R.string.moa_amount to "1/2" to Distance.from(0.5f, DistanceUnits.Inches),
            R.string.moa_amount to "1" to Distance.from(1f, DistanceUnits.Inches),
            R.string.mil_amount to DecimalFormatter.format(0.1, 1) to Distance.from(0.36f, DistanceUnits.Inches),
            R.string.mil_amount to DecimalFormatter.format(0.05, 2) to Distance.from(0.18f, DistanceUnits.Inches)
        )
    }

    val selectedClickAmount = distancePerClick ?: clickAmounts[
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) 1 else 4
    ].second

    val adjustmentX = remember(offsetX, offsetDirectionX, distanceToTarget, selectedClickAmount) {
        getScopeAdjustment(
            selectedClickAmount,
            referenceDistance,
            offsetX,
            offsetDirectionX,
            distanceToTarget
        )
    }
    val adjustmentY = remember(offsetY, offsetDirectionY, distanceToTarget, selectedClickAmount) {
        getScopeAdjustment(
            selectedClickAmount,
            referenceDistance,
            offsetY,
            offsetDirectionY,
            distanceToTarget
        )
    }
    val adjustmentText = listOfNotNull(adjustmentX, adjustmentY).joinToString("\n") {
        "${it.direction.name} ${it.clicks} clicks"
    }.ifBlank { stringResource(R.string.none) }

    BallisticsForm(modifier = modifier) {
        UnitPickerField(
            label = stringResource(R.string.adjustment_per_click),
            selected = selectedClickAmount,
            options = clickAmounts.map { amount ->
                stringResource(amount.first.first, amount.first.second) to amount.second
            },
            onSelected = setDistancePerClick,
            modifier = Modifier.testTag("click_amount")
        )
        DistanceField(
            label = stringResource(R.string.distance_to_target),
            value = distanceToTarget,
            units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
            defaultUnit = DistanceUnits.Yards,
            onValueChange = { setDistanceToTarget(it ?: Distance.from(0f, DistanceUnits.Yards)) },
            modifier = Modifier.testTag("distance_to_target")
        )
        SectionLabel(text = stringResource(R.string.windage_miss))
        DistanceField(
            label = stringResource(R.string.distance),
            value = offsetX,
            units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits),
            defaultUnit = DistanceUnits.Inches,
            onValueChange = { offsetX = it ?: Distance.from(0f, DistanceUnits.Inches) },
            modifier = Modifier.testTag("miss_distance_x")
        )
        ToggleRow(
            options = listOf(
                stringResource(R.string.left) to ScopeDirection.Left,
                stringResource(R.string.right) to ScopeDirection.Right
            ),
            selected = offsetDirectionX,
            onSelected = { offsetDirectionX = it },
            modifier = Modifier.testTag("miss_direction_x")
        )
        SectionLabel(text = stringResource(R.string.elevation_miss))
        DistanceField(
            label = stringResource(R.string.distance),
            value = offsetY,
            units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits),
            defaultUnit = DistanceUnits.Inches,
            onValueChange = { offsetY = it ?: Distance.from(0f, DistanceUnits.Inches) },
            modifier = Modifier.testTag("miss_distance_y")
        )
        ToggleRow(
            options = listOf(
                stringResource(R.string.above) to ScopeDirection.Up,
                stringResource(R.string.below) to ScopeDirection.Down
            ),
            selected = offsetDirectionY,
            onSelected = { offsetDirectionY = it },
            modifier = Modifier.testTag("miss_direction_y")
        )
        SectionLabel(text = stringResource(R.string.scope_adjustment))
        Text(
            text = adjustmentText,
            modifier = Modifier.testTag("adjustment_amount"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun BallisticsForm(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
internal fun DistanceField(
    label: String,
    value: Distance?,
    units: List<DistanceUnits>,
    defaultUnit: DistanceUnits,
    onValueChange: (Distance?) -> Unit,
    modifier: Modifier = Modifier
) {
    UnitAmountField(
        label = label,
        value = value,
        units = units,
        defaultUnit = defaultUnit,
        getUnitName = { AppServiceRegistry.get<FormatService>().getDistanceUnitName(it) },
        getAmount = { it.value },
        getUnit = { it.units },
        createValue = { amount, unit -> Distance.from(amount, unit) },
        onValueChange = onValueChange,
        modifier = modifier
    )
}

@Composable
internal fun SpeedField(
    label: String,
    value: Speed?,
    units: List<DistanceUnits>,
    defaultUnit: DistanceUnits,
    onValueChange: (Speed?) -> Unit,
    modifier: Modifier = Modifier
) {
    UnitAmountField(
        label = label,
        value = value,
        units = units,
        defaultUnit = defaultUnit,
        getUnitName = { AppServiceRegistry.get<FormatService>().getBulletSpeedUnitName(it) },
        getAmount = { it.value },
        getUnit = { it.distanceUnits },
        createValue = { amount, unit -> Speed.from(amount, unit, TimeUnits.Seconds) },
        onValueChange = onValueChange,
        modifier = modifier
    )
}

@Composable
internal fun WeightField(
    label: String,
    value: Weight?,
    units: List<WeightUnits>,
    defaultUnit: WeightUnits,
    onValueChange: (Weight?) -> Unit,
    modifier: Modifier = Modifier
) {
    UnitAmountField(
        label = label,
        value = value,
        units = units,
        defaultUnit = defaultUnit,
        getUnitName = { AppServiceRegistry.get<FormatService>().getWeightUnitName(it) },
        getAmount = { it.value },
        getUnit = { it.units },
        createValue = { amount, unit -> Weight.from(amount, unit) },
        onValueChange = onValueChange,
        modifier = modifier
    )
}

@Composable
private fun <T, U> UnitAmountField(
    label: String,
    value: T?,
    units: List<U>,
    defaultUnit: U,
    getUnitName: (U) -> String,
    getAmount: (T) -> Float,
    getUnit: (T) -> U,
    createValue: (Float, U) -> T,
    onValueChange: (T?) -> Unit,
    modifier: Modifier = Modifier
) {
    var amountText by remember { mutableStateOf(value?.let { DecimalFormatter.format(getAmount(it), 5, false) } ?: "") }
    var unit by remember { mutableStateOf(value?.let(getUnit) ?: defaultUnit) }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                onValueChange(it.toFloatOrNull()?.let { amount -> createValue(amount, unit) })
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        UnitPickerField(
            label = stringResource(R.string.units),
            selected = unit,
            options = units.map { getUnitName(it) to it },
            onSelected = {
                unit = it
                onValueChange(amountText.toFloatOrNull()?.let { amount -> createValue(amount, it) })
            }
        )
    }
}

@Composable
internal fun <T> UnitPickerField(
    label: String,
    selected: T,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.second == selected }?.first.orEmpty()

    OutlinedButton(
        onClick = { isOpen = true },
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = if (selectedName.isBlank()) label else "$label: $selectedName",
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (isOpen) {
        AlertDialog(
            onDismissRequest = { isOpen = false },
            title = { Text(label) },
            text = {
                Column {
                    options.forEach { option ->
                        TextButton(
                            onClick = {
                                onSelected(option.second)
                                isOpen = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option.first,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isOpen = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
internal fun <T> ToggleRow(
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option.second == selected
            OutlinedButton(
                onClick = { onSelected(option.second) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.first,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
internal fun rememberDistancePreference(key: String): Pair<Distance?, (Distance?) -> Unit> {
    val prefs = remember { AppServiceRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getDistance(key)) }
    val setter: (Distance?) -> Unit = {
        prefs.putDistance(key, it)
        value = it
    }
    return value to setter
}

@Composable
internal fun rememberSpeedPreference(key: String): Pair<Speed?, (Speed?) -> Unit> {
    val prefs = remember { AppServiceRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getSpeed(key)) }
    val setter: (Speed?) -> Unit = {
        prefs.putSpeed(key, it)
        value = it
    }
    return value to setter
}

@Composable
internal fun rememberWeightPreference(key: String): Pair<Weight?, (Weight?) -> Unit> {
    val prefs = remember { AppServiceRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getWeight(key)) }
    val setter: (Weight?) -> Unit = {
        prefs.putWeight(key, it)
        value = it
    }
    return value to setter
}

@Composable
internal fun rememberFloatPreference(key: String): Pair<Float?, (Float?) -> Unit> {
    val prefs = remember { AppServiceRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getFloat(key)) }
    val setter: (Float?) -> Unit = {
        if (it == null) {
            prefs.remove(key)
        } else {
            prefs.putFloat(key, it)
        }
        value = it
    }
    return value to setter
}

private fun IPreferences.getDistance(key: String): Distance? {
    val value = getFloat("$key-value") ?: return null
    val unit = getInt("$key-unit") ?: return null
    return Distance.from(value, DistanceUnits.entries.firstOrNull { it.id == unit } ?: DistanceUnits.Meters)
}

private fun IPreferences.putDistance(key: String, distance: Distance?) {
    if (distance == null) {
        remove("$key-value")
        remove("$key-unit")
    } else {
        putFloat("$key-value", distance.value)
        putInt("$key-unit", distance.units.id)
    }
}

private fun IPreferences.getSpeed(key: String): Speed? {
    val value = getFloat("$key-value") ?: return null
    val distanceUnit = getInt("$key-distance-unit") ?: return null
    val timeUnit = getInt("$key-time-unit") ?: return null
    return Speed.from(
        value,
        DistanceUnits.entries.firstOrNull { it.id == distanceUnit } ?: DistanceUnits.Meters,
        TimeUnits.entries.firstOrNull { it.id == timeUnit } ?: TimeUnits.Seconds
    )
}

private fun IPreferences.putSpeed(key: String, speed: Speed?) {
    if (speed == null) {
        remove("$key-value")
        remove("$key-distance-unit")
        remove("$key-time-unit")
    } else {
        putFloat("$key-value", speed.value)
        putInt("$key-distance-unit", speed.distanceUnits.id)
        putInt("$key-time-unit", speed.timeUnits.id)
    }
}

private fun IPreferences.getWeight(key: String): Weight? {
    val value = getFloat("$key-value") ?: return null
    val unit = getInt("$key-unit") ?: return null
    return Weight.from(value, WeightUnits.entries.firstOrNull { it.id == unit } ?: WeightUnits.Kilograms)
}

private fun IPreferences.putWeight(key: String, weight: Weight?) {
    if (weight == null) {
        remove("$key-value")
        remove("$key-unit")
    } else {
        putFloat("$key-value", weight.value)
        putInt("$key-unit", weight.units.id)
    }
}

private fun getScopeAdjustment(
    distancePerClick: Distance,
    referenceDistance: Distance,
    offset: Distance,
    offsetDirection: ScopeDirection,
    distanceToTarget: Distance?
): ScopeAdjustment? {
    distanceToTarget ?: return null
    val offsetInches = offset.convertTo(DistanceUnits.Inches).value
    val distanceToTargetYards = distanceToTarget.convertTo(DistanceUnits.Yards).value
    val referenceYards = referenceDistance.convertTo(DistanceUnits.Yards).value
    val inchesPerClick = distancePerClick.convertTo(DistanceUnits.Inches).value

    if (Arithmetic.isZero(inchesPerClick) || Arithmetic.isZero(distanceToTargetYards)) {
        return null
    }

    val clicks = (offsetInches / inchesPerClick) * (referenceYards / distanceToTargetYards)
    val clickDirection = when (offsetDirection) {
        ScopeDirection.Left -> ScopeDirection.Right
        ScopeDirection.Right -> ScopeDirection.Left
        ScopeDirection.Up -> ScopeDirection.Down
        ScopeDirection.Down -> ScopeDirection.Up
    }
    return ScopeAdjustment(clicks.roundToInt(), clickDirection)
}

private data class ScopeAdjustment(val clicks: Int, val direction: ScopeDirection)

private enum class ScopeDirection {
    Left,
    Right,
    Up,
    Down
}
