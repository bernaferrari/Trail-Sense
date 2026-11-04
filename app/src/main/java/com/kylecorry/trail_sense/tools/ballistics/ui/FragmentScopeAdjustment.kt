package com.kylecorry.trail_sense.tools.ballistics.ui

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

        val windageLabel = stringResource(R.string.windage_miss)
        val leftLabel = stringResource(R.string.left)
        val rightLabel = stringResource(R.string.right)
        val elevationLabel = stringResource(R.string.elevation_miss)
        val aboveLabel = stringResource(R.string.above)
        val belowLabel = stringResource(R.string.below)
        val missUnits = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
        Column {
            TrailSenseGroupHeader(stringResource(R.string.ballistics_where_hit))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
            MissCard(
                shape = trailSenseGridCellShape(0, 2, 0, 1),
                modifier = Modifier.weight(1f).fillMaxHeight(),
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
            MissCard(
                shape = trailSenseGridCellShape(1, 2, 0, 1),
                modifier = Modifier.weight(1f).fillMaxHeight(),
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
}

/**
 * One miss as a card: the axis and its unit chip in the header, how far off as a large number you type straight
 * into, and which side under it. Windage and elevation sit side by side so the pair reads as one answer, not a form.
 */
@Composable
private fun MissCard(
    label: String,
    offset: Distance,
    onOffsetChange: (Distance?) -> Unit,
    options: List<TrailSenseChoice<ScopeDirection>>,
    direction: ScopeDirection,
    onDirectionChange: (ScopeDirection) -> Unit,
    fieldTag: String,
    directionTag: String,
    units: List<DistanceUnits>,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    val scheme = MaterialTheme.colorScheme
    var amountText by remember {
        mutableStateOf(offset.value.takeIf { it != 0f }?.let { DecimalFormatter.format(it, 5, false) } ?: "")
    }
    var unit by remember { mutableStateOf(offset.units) }
    var isPickingUnit by remember { mutableStateOf(false) }
    Surface(modifier = modifier, shape = shape, color = scheme.surfaceContainerLow) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Surface(
                    onClick = { isPickingUnit = true },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                ) {
                    Text(
                        formatter.getDistanceUnitName(unit, true),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.xs)
                    )
                }
            }
            androidx.compose.foundation.text.BasicTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    onOffsetChange(it.toFloatOrNull()?.let { amount -> Distance.from(amount, unit) })
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(fieldTag),
                decorationBox = { inner ->
                    Box {
                        if (amountText.isEmpty()) {
                            Text(
                                "0",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                }
            )
            TrailSenseConnectedChoice(
                options = options,
                selected = direction,
                onSelect = onDirectionChange,
                modifier = Modifier.testTag(directionTag)
            )
        }
    }
    if (isPickingUnit) {
        TrailSenseOptionsDialog(
            title = stringResource(R.string.units),
            options = units.map { formatter.getDistanceUnitName(it) to it },
            selected = unit,
            onSelect = {
                unit = it
                isPickingUnit = false
                onOffsetChange(amountText.toFloatOrNull()?.let { amount -> Distance.from(amount, it) })
            },
            onDismiss = { isPickingUnit = false }
        )
    }
}

/** The answer of a ballistics page as a Spotlight: a shaped badge, the label above, the value large and SemiBold. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BallisticsHero(
    label: String,
    value: String,
    valueTag: String,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    icon: androidx.compose.ui.graphics.vector.ImageVector = TrailIcons.Ballistics
) {
    TrailSenseSpotlight(
        title = value,
        eyebrow = label,
        titleStyle = style,
        modifier = Modifier
            .testTag(valueTag)
            .semantics(mergeDescendants = true) {},
        badge = {
            TrailSenseShapeBadge(
                icon = icon,
                shape = MaterialShapes.Burst.toShape(),
                size = 64.dp
            )
        }
    )
}

/**
 * The scope hero: a reticle showing where the shot landed and the way back to the centre, beside the two turret
 * moves as badges with arrows ("4 clicks" right, "2 clicks" up).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScopeHero(
    adjustmentX: ScopeAdjustment?,
    adjustmentY: ScopeAdjustment?,
    missX: Float,
    missY: Float,
    description: String
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adjustment_amount")
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Text(
                stringResource(R.string.scope_adjustment),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Reticle(
                    missX = missX,
                    missY = missY,
                    modifier = Modifier.size(136.dp)
                )
                Spacer(Modifier.width(TrailSenseSpacing.xl))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
                ) {
                    val waiting = adjustmentX == null && adjustmentY == null
                    val onTarget = !waiting && (adjustmentX?.clicks ?: 0) == 0 && (adjustmentY?.clicks ?: 0) == 0
                    when {
                        waiting -> {
                            Text(
                                stringResource(R.string.ballistics_waiting),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.ballistics_waiting_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        onTarget -> {
                            TrailSenseShapeBadge(
                                icon = TrailIcons.Check,
                                shape = MaterialShapes.Sunny.toShape(),
                                size = 52.dp
                            )
                            Text(
                                stringResource(R.string.ballistics_on_target),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        else -> {
                            TurretMove(adjustmentX, MaterialShapes.Cookie9Sided)
                            TurretMove(adjustmentY, MaterialShapes.Cookie6Sided)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TurretMove(adjustment: ScopeAdjustment?, shape: androidx.graphics.shapes.RoundedPolygon) {
    val scheme = MaterialTheme.colorScheme
    val none = adjustment == null || adjustment.clicks == 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        TrailSenseShapeBadge(
            icon = if (none) TrailIcons.Check else TrailIcons.Arrow,
            shape = shape.toShape(),
            size = 44.dp,
            containerColor = if (none) scheme.surfaceContainerLowest.copy(alpha = 0.7f) else scheme.primary,
            contentColor = if (none) scheme.onPrimaryContainer else scheme.onPrimary,
            iconRotation = when (adjustment?.direction) {
                ScopeDirection.Right -> 90f
                ScopeDirection.Down -> 180f
                ScopeDirection.Left -> 270f
                else -> 0f
            }
        )
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Column {
            Text(
                text = if (adjustment == null) {
                    stringResource(R.string.dash)
                } else {
                    pluralStringResource(R.plurals.ballistics_clicks, adjustment.clicks, adjustment.clicks)
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
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
                color = scheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

/** Where the shot hit (x right, y up, any unit), scaled so the furthest miss sits near the edge. */
@Composable
private fun Reticle(missX: Float, missY: Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onPrimaryContainer
    val hit = scheme.tertiary
    val path = scheme.primary
    val face = scheme.surfaceContainerLowest.copy(alpha = 0.7f)
    val x by animateFloatAsState(missX, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "missX")
    val y by animateFloatAsState(missY, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "missY")
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(face, radius, center)
        listOf(0.33f, 0.66f).forEach {
            drawCircle(ink.copy(alpha = 0.18f), radius * it, center, style = Stroke(1.5.dp.toPx()))
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
            drawCircle(hit.copy(alpha = 0.3f), 13.dp.toPx(), impact)
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
