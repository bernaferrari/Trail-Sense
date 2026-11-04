package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.cache.DependencyRegistry
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.roundToInt

/**
 * Scope adjustment: the clicks to dial lead, then the scope setup, then each miss as a group of how far and which
 * side (a segmented choice, not two look-alike buttons).
 */
@Composable
fun ScopeAdjustmentPage(
    modifier: Modifier = Modifier
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    val prefs = remember { DependencyRegistry.get<UserPreferences>() }
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
        BallisticsHero(
            label = stringResource(R.string.scope_adjustment),
            value = adjustmentText,
            valueTag = "adjustment_amount",
            style = MaterialTheme.typography.headlineMedium
        )

        TrailSenseRowGroup {
            row { position ->
                UnitPickerField(
                    label = stringResource(R.string.adjustment_per_click),
                    selected = selectedClickAmount,
                    options = clickAmounts.map { amount ->
                        stringResource(amount.first.first, amount.first.second) to amount.second
                    },
                    onSelected = setDistancePerClick,
                    position = position,
                    modifier = Modifier.testTag("click_amount")
                )
            }
            row { position ->
                DistanceField(
                    label = stringResource(R.string.distance_to_target),
                    value = distanceToTarget,
                    units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
                    defaultUnit = DistanceUnits.Yards,
                    onValueChange = { setDistanceToTarget(it ?: Distance.from(0f, DistanceUnits.Yards)) },
                    position = position,
                    modifier = Modifier.testTag("distance_to_target")
                )
            }
        }

        MissGroup(
            title = stringResource(R.string.windage_miss),
            offset = offsetX,
            onOffsetChange = { offsetX = it ?: Distance.from(0f, DistanceUnits.Inches) },
            options = listOf(
                TrailSenseChoice(ScopeDirection.Left, stringResource(R.string.left)),
                TrailSenseChoice(ScopeDirection.Right, stringResource(R.string.right))
            ),
            direction = offsetDirectionX,
            onDirectionChange = { offsetDirectionX = it },
            fieldTag = "miss_distance_x",
            directionTag = "miss_direction_x"
        )

        MissGroup(
            title = stringResource(R.string.elevation_miss),
            offset = offsetY,
            onOffsetChange = { offsetY = it ?: Distance.from(0f, DistanceUnits.Inches) },
            options = listOf(
                TrailSenseChoice(ScopeDirection.Up, stringResource(R.string.above)),
                TrailSenseChoice(ScopeDirection.Down, stringResource(R.string.below))
            ),
            direction = offsetDirectionY,
            onDirectionChange = { offsetDirectionY = it },
            fieldTag = "miss_distance_y",
            directionTag = "miss_direction_y"
        )
    }
}

/** One miss: how far off, then which side, as one group. */
@Composable
private fun MissGroup(
    title: String,
    offset: Distance,
    onOffsetChange: (Distance?) -> Unit,
    options: List<TrailSenseChoice<ScopeDirection>>,
    direction: ScopeDirection,
    onDirectionChange: (ScopeDirection) -> Unit,
    fieldTag: String,
    directionTag: String
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    TrailSenseRowGroup(title = title) {
        row { position ->
            DistanceField(
                label = stringResource(R.string.distance),
                value = offset,
                units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits),
                defaultUnit = DistanceUnits.Inches,
                onValueChange = onOffsetChange,
                position = position,
                modifier = Modifier.testTag(fieldTag)
            )
        }
        row { position ->
            TrailSenseGroupCard(
                position = position,
                contentPadding = PaddingValues(TrailSenseSpacing.sm)
            ) {
                TrailSenseConnectedChoice(
                    options = options,
                    selected = direction,
                    onSelect = onDirectionChange,
                    modifier = Modifier.testTag(directionTag)
                )
            }
        }
    }
}

/** The answer of a ballistics page, on the accent tone, its label above. */
@Composable
internal fun BallisticsHero(
    label: String,
    value: String,
    valueTag: String,
    style: TextStyle = MaterialTheme.typography.displaySmall
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupOuter),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                modifier = Modifier.testTag(valueTag),
                style = style.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A ballistics page is a plain column; the page around it owns scrolling and gutters. */
@Composable
internal fun BallisticsForm(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg),
        content = content
    )
}

@Composable
internal fun DistanceField(
    label: String,
    value: Distance?,
    units: List<DistanceUnits>,
    defaultUnit: DistanceUnits,
    onValueChange: (Distance?) -> Unit,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    UnitAmountField(
        label = label,
        value = value,
        units = units,
        defaultUnit = defaultUnit,
        getUnitName = { formatter.getDistanceUnitName(it) },
        getShortUnitName = { formatter.getDistanceUnitName(it, true) },
        getAmount = { it.value },
        getUnit = { it.units },
        createValue = { amount, unit -> Distance.from(amount, unit) },
        onValueChange = onValueChange,
        position = position,
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
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    UnitAmountField(
        label = label,
        value = value,
        units = units,
        defaultUnit = defaultUnit,
        getUnitName = { formatter.getBulletSpeedUnitName(it) },
        getShortUnitName = { formatter.getBulletSpeedUnitName(it, true) },
        getAmount = { it.value },
        getUnit = { it.distanceUnits },
        createValue = { amount, unit -> Speed.from(amount, unit, TimeUnits.Seconds) },
        onValueChange = onValueChange,
        position = position,
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
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    UnitAmountField(
        label = label,
        value = value,
        units = units,
        defaultUnit = defaultUnit,
        getUnitName = { formatter.getWeightUnitName(it) },
        getShortUnitName = { formatter.getWeightUnitName(it, true) },
        getAmount = { it.value },
        getUnit = { it.units },
        createValue = { amount, unit -> Weight.from(amount, unit) },
        onValueChange = onValueChange,
        position = position,
        modifier = modifier
    )
}

/** An amount and its unit as one field: the unit is the tonal button at its end. */
@Composable
private fun <T, U> UnitAmountField(
    label: String,
    value: T?,
    units: List<U>,
    defaultUnit: U,
    getUnitName: (U) -> String,
    getShortUnitName: (U) -> String,
    getAmount: (T) -> Float,
    getUnit: (T) -> U,
    createValue: (Float, U) -> T,
    onValueChange: (T?) -> Unit,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single
) {
    var amountText by remember { mutableStateOf(value?.let { DecimalFormatter.format(getAmount(it), 5, false) } ?: "") }
    var unit by remember { mutableStateOf(value?.let(getUnit) ?: defaultUnit) }
    var isPickingUnit by remember { mutableStateOf(false) }

    TrailSenseInputField(
        value = amountText,
        onValueChange = {
            amountText = it
            onValueChange(it.toFloatOrNull()?.let { amount -> createValue(amount, unit) })
        },
        label = label,
        position = position,
        unit = getShortUnitName(unit),
        onUnitClick = { isPickingUnit = true },
        modifier = modifier
    )

    if (isPickingUnit) {
        TrailSenseOptionsDialog(
            title = stringResource(R.string.units),
            options = units.map { getUnitName(it) to it },
            selected = unit,
            onSelect = {
                unit = it
                isPickingUnit = false
                onValueChange(amountText.toFloatOrNull()?.let { amount -> createValue(amount, it) })
            },
            onDismiss = { isPickingUnit = false }
        )
    }
}

/** A choice from a list, as a row: the label, what's chosen under it, a dialog to change it. */
@Composable
internal fun <T> UnitPickerField(
    label: String,
    selected: T,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition = TrailSenseGroupPosition.Single
) {
    var isOpen by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.second == selected }?.first

    TrailSenseListItem(
        title = label,
        subtitle = selectedName,
        position = position,
        onClick = { isOpen = true },
        modifier = modifier
    )

    if (isOpen) {
        TrailSenseOptionsDialog(
            title = label,
            options = options,
            selected = selected,
            onSelect = {
                onSelected(it)
                isOpen = false
            },
            onDismiss = { isOpen = false }
        )
    }
}

@Composable
internal fun rememberDistancePreference(key: String): Pair<Distance?, (Distance?) -> Unit> {
    val prefs = remember { DependencyRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getDistance(key)) }
    val setter: (Distance?) -> Unit = {
        prefs.putDistance(key, it)
        value = it
    }
    return value to setter
}

@Composable
internal fun rememberSpeedPreference(key: String): Pair<Speed?, (Speed?) -> Unit> {
    val prefs = remember { DependencyRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getSpeed(key)) }
    val setter: (Speed?) -> Unit = {
        prefs.putSpeed(key, it)
        value = it
    }
    return value to setter
}

@Composable
internal fun rememberWeightPreference(key: String): Pair<Weight?, (Weight?) -> Unit> {
    val prefs = remember { DependencyRegistry.get<PreferencesSubsystem>().preferences }
    var value by remember(key) { mutableStateOf(prefs.getWeight(key)) }
    val setter: (Weight?) -> Unit = {
        prefs.putWeight(key, it)
        value = it
    }
    return value to setter
}

@Composable
internal fun rememberFloatPreference(key: String): Pair<Float?, (Float?) -> Unit> {
    val prefs = remember { DependencyRegistry.get<PreferencesSubsystem>().preferences }
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
