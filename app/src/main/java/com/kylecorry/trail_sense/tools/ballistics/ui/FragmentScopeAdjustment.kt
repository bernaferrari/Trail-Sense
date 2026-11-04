package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroupScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Arrow
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.Ballistics
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
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
import androidx.compose.animation.core.animateFloat
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
        ScopeHero(
            adjustmentX = adjustmentX,
            adjustmentY = adjustmentY,
            missX = offsetX.convertTo(DistanceUnits.Inches).value *
                (if (offsetDirectionX == ScopeDirection.Left) -1f else 1f),
            missY = offsetY.convertTo(DistanceUnits.Inches).value *
                (if (offsetDirectionY == ScopeDirection.Down) -1f else 1f),
            description = adjustmentText
        )

        // The one input the answer waits on comes first; the scope's click size is a quieter setting under it.
        TrailSenseRowGroup {
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
        }

        val missUnits = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
        val leftLabel = stringResource(R.string.left)
        val rightLabel = stringResource(R.string.right)
        val aboveLabel = stringResource(R.string.above)
        val belowLabel = stringResource(R.string.below)
        val windageLabel = stringResource(R.string.windage_miss)
        val elevationLabel = stringResource(R.string.elevation_miss)
        val whereHitLabel = stringResource(R.string.ballistics_where_hit)
        // Each miss is a real field (how far, with its unit) and, right under it, which side: the kit's selection.
        TrailSenseRowGroup(title = whereHitLabel) {
            missRows(
                label = windageLabel,
                offset = offsetX,
                onOffsetChange = { offsetX = it ?: Distance.from(0f, DistanceUnits.Inches) },
                options = listOf(
                    TrailSenseChoice(ScopeDirection.Left, leftLabel),
                    TrailSenseChoice(ScopeDirection.Right, rightLabel)
                ),
                direction = offsetDirectionX,
                onDirectionChange = { offsetDirectionX = it },
                fieldTag = "miss_distance_x",
                directionTag = "miss_direction_x",
                units = missUnits
            )
            missRows(
                label = elevationLabel,
                offset = offsetY,
                onOffsetChange = { offsetY = it ?: Distance.from(0f, DistanceUnits.Inches) },
                options = listOf(
                    TrailSenseChoice(ScopeDirection.Up, aboveLabel),
                    TrailSenseChoice(ScopeDirection.Down, belowLabel)
                ),
                direction = offsetDirectionY,
                onDirectionChange = { offsetDirectionY = it },
                fieldTag = "miss_distance_y",
                directionTag = "miss_direction_y",
                units = missUnits
            )
        }
    }
}

/** One miss as two rows of the group: how far off (a filled field with its unit) and which side. */
private fun TrailSenseRowGroupScope.missRows(
    label: String,
    offset: Distance,
    onOffsetChange: (Distance?) -> Unit,
    options: List<TrailSenseChoice<ScopeDirection>>,
    direction: ScopeDirection,
    onDirectionChange: (ScopeDirection) -> Unit,
    fieldTag: String,
    directionTag: String,
    units: List<DistanceUnits>
) {
    row { position ->
        DistanceField(
            label = label,
            value = offset.takeIf { it.value != 0f },
            units = units,
            defaultUnit = offset.units,
            onValueChange = onOffsetChange,
            position = position,
            modifier = Modifier.testTag(fieldTag)
        )
    }
    row { position ->
        TrailSenseGroupCard(
            position = position,
            // As in every family settings card: the choice sits 16 dp in on every side.
            contentPadding = PaddingValues(TrailSenseSpacing.lg)
        ) {
            TrailSenseConnectedChoice(
                options = options,
                selected = direction,
                onSelect = onDirectionChange,
                label = label,
                modifier = Modifier.testTag(directionTag)
            )
        }
    }
}

/** The answer of a ballistics page as calm emphasised type: the value large and SemiBold, its name under it. */
@Composable
internal fun BallisticsHero(
    label: String,
    value: String,
    valueTag: String,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    @Suppress("UNUSED_PARAMETER") icon: androidx.compose.ui.graphics.vector.ImageVector = TrailIcons.Ballistics
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.lg)
            .testTag(valueTag)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        Text(
            value,
            style = style.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The answer, plainly: the turret moves as emphasised type ("4 clicks right", "2 clicks up"), beside a small reticle
 * showing where the shot landed and the way back to the centre. No card.
 */
@Composable
private fun ScopeHero(
    adjustmentX: ScopeAdjustment?,
    adjustmentY: ScopeAdjustment?,
    missX: Float,
    missY: Float,
    description: String
) {
    val scheme = MaterialTheme.colorScheme
    val waiting = adjustmentX == null && adjustmentY == null
    val onTarget = !waiting && (adjustmentX?.clicks ?: 0) == 0 && (adjustmentY?.clicks ?: 0) == 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.lg)
            .testTag("adjustment_amount")
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            when {
                // Idle: the reticle at rest and one hint line, no "waiting" header.
                waiting -> Text(
                    stringResource(R.string.ballistics_waiting_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant
                )

                onTarget -> Text(
                    stringResource(R.string.ballistics_on_target),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold
                )

                else -> {
                    TurretMove(adjustmentX)
                    TurretMove(adjustmentY)
                }
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.lg))
        Reticle(
            missX = missX,
            missY = missY,
            ink = scheme.onSurfaceVariant,
            modifier = Modifier.size(120.dp)
        )
    }
}

@Composable
private fun TurretMove(adjustment: ScopeAdjustment?) {
    val scheme = MaterialTheme.colorScheme
    val none = adjustment == null || adjustment.clicks == 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            imageVector = if (none) TrailIcons.Check else TrailIcons.Arrow,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    rotationZ = when (adjustment?.direction) {
                        ScopeDirection.Right -> 90f
                        ScopeDirection.Down -> 180f
                        ScopeDirection.Left -> 270f
                        else -> 0f
                    }
                }
        )
        Spacer(Modifier.width(TrailSenseSpacing.sm))
        Column {
            Text(
                text = if (adjustment == null) {
                    stringResource(R.string.dash)
                } else {
                    pluralStringResource(R.plurals.ballistics_clicks, adjustment.clicks, adjustment.clicks)
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when (adjustment?.direction) {
                    ScopeDirection.Left -> stringResource(R.string.left)
                    ScopeDirection.Right -> stringResource(R.string.right)
                    ScopeDirection.Up -> stringResource(R.string.ballistics_up)
                    ScopeDirection.Down -> stringResource(R.string.ballistics_down)
                    null -> stringResource(R.string.ballistics_enter_distance)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/** Where the shot hit (x right, y up, any unit), scaled so the furthest miss sits near the edge. */
@Composable
private fun Reticle(missX: Float, missY: Float, ink: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val hit = scheme.tertiary
    val path = scheme.primary
    val face = scheme.surfaceContainer
    val x by animateFloatAsState(missX, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "missX")
    val y by animateFloatAsState(missY, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "missY")
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(face, radius, center)
        // The live object keeps its primary ring at rest, so it never reads as a ghost.
        drawCircle(path, radius - 1.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        listOf(0.33f, 0.66f).forEach {
            drawCircle(ink.copy(alpha = 0.3f), radius * it, center, style = Stroke(1.5.dp.toPx()))
        }
        val gap = 8.dp.toPx()
        val stroke = 2.dp.toPx()
        drawLine(ink.copy(alpha = 0.55f), Offset(center.x - radius + 6.dp.toPx(), center.y), Offset(center.x - gap, center.y), stroke, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.55f), Offset(center.x + gap, center.y), Offset(center.x + radius - 6.dp.toPx(), center.y), stroke, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.55f), Offset(center.x, center.y - radius + 6.dp.toPx()), Offset(center.x, center.y - gap), stroke, StrokeCap.Round)
        drawLine(ink.copy(alpha = 0.55f), Offset(center.x, center.y + gap), Offset(center.x, center.y + radius - 6.dp.toPx()), stroke, StrokeCap.Round)
        drawCircle(path, 3.dp.toPx(), center)

        val largest = maxOf(kotlin.math.abs(x), kotlin.math.abs(y))
        if (largest > 0f) {
            val scale = radius * 0.7f / largest
            val impact = Offset(center.x + x * scale, center.y - y * scale)
            drawLine(
                path,
                impact,
                center,
                3.dp.toPx(),
                StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
            )
            drawCircle(hit, 8.dp.toPx(), impact)
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
        trailingText = selectedName,
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
