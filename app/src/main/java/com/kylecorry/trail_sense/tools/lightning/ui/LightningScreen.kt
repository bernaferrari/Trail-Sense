@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.lightning.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.trail.Thunder
import com.kylecorry.trail_sense.shared.icons.trail.TorchOn
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import com.kylecorry.trail_sense.tools.weather.ui.SkyPill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Lightning distance: a saturated card holds the distance, with sound waves rolling out of it while you wait for
 * the thunder. Under it, one big shaped button names the event to tap for next (the flash, then the thunder); the
 * screen flashes with the tap. The previous strike sits below as two tiles.
 */
@Composable
fun LightningScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val formatService = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val repo = remember { LightningRepo.getInstance(context) }

    DisposableEffect(view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previous
        }
    }

    var units by remember { mutableStateOf(prefs.baseDistanceUnits) }
    var lightningTime by remember { mutableStateOf<Instant?>(null) }
    var strike by remember { mutableStateOf<Reading<LightningStrike>?>(null) }
    var lastStrike by remember { mutableStateOf<Reading<LightningStrike>?>(null) }
    var now by remember { mutableStateOf(Instant.now()) }
    val flash = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        units = prefs.baseDistanceUnits
        lightningTime = null
        strike = null
        lastStrike = withContext(Dispatchers.IO) {
            repo.getLast()
        }
    }

    LaunchedEffect(lightningTime, strike) {
        while (true) {
            now = Instant.now()
            delay(20)
        }
    }

    val currentDistance = lightningTime?.let { getDistance(it, now) } ?: strike?.value?.distance
    val relativeDistance = currentDistance?.convertTo(units)?.toRelativeDistance()
    val isDangerous = relativeDistance?.let { Meteorology.isLightningStrikeDangerous(it) } ?: false

    val waitingForThunder = lightningTime != null
    val distanceValue = relativeDistance?.let {
        DecimalFormatter.format(it.value, Units.getDecimalPlaces(it.units), false)
    } ?: "--"
    val distanceUnit = formatService.getDistanceUnitName(relativeDistance?.units ?: units, true)
    val elapsed = lightningTime?.let { Duration.between(it, now).toMillis() / 1000f }

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_lightning_title),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
        ) {
            StrikeHero(
                value = distanceValue,
                unit = distanceUnit,
                hint = when {
                    isDangerous -> stringResource(R.string.lightning_dangerously_close)
                    waitingForThunder -> stringResource(R.string.lightning_hint_thunder)
                    else -> stringResource(R.string.lightning_hint_flash)
                },
                tone = when {
                    isDangerous -> TrailSenseTone.Error
                    waitingForThunder -> TrailSenseTone.Tertiary
                    else -> TrailSenseTone.Primary
                },
                elapsedSeconds = elapsed,
                modifier = Modifier.widthIn(max = TrailSenseSize.maximumContentWidth)
            )

            StrikeButton(
                waitingForThunder = waitingForThunder,
                onClick = {
                    val startedAt = lightningTime
                    if (startedAt == null) {
                        lightningTime = Instant.now()
                        scope.launch {
                            flash.snapTo(0.55f)
                            flash.animateTo(0f, tween(450))
                        }
                    } else {
                        val distance = getDistance(startedAt, Instant.now())
                        val currentStrike = Reading(LightningStrike(0, distance), Instant.now())
                        strike?.let { lastStrike = it }
                        strike = currentStrike
                        lightningTime = null
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repo.add(currentStrike)
                            }
                        }
                    }
                }
            )

            AnimatedVisibility(
                visible = lastStrike != null,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
                modifier = Modifier.widthIn(max = TrailSenseSize.maximumContentWidth)
            ) {
                val last = lastStrike
                if (last != null) {
                    val distance = last.value.distance.convertTo(units).toRelativeDistance()
                    Column(Modifier.fillMaxWidth()) {
                        TrailSenseGroupHeader(stringResource(R.string.previous))
                        TrailSenseStatGrid(
                            stats = listOf(
                                TrailSenseStat(
                                    label = stringResource(R.string.sky_strike_time),
                                    value = formatService.formatTime(last.time, includeSeconds = false),
                                    icon = TrailIcons.Thunder
                                ),
                                TrailSenseStat(
                                    label = stringResource(R.string.sky_strike_last_distance),
                                    value = formatService.formatDistance(
                                        distance,
                                        Units.getDecimalPlaces(distance.units),
                                        false
                                    ),
                                    tone = if (Meteorology.isLightningStrikeDangerous(distance)) {
                                        TrailSenseTone.Error
                                    } else {
                                        TrailSenseTone.Neutral
                                    }
                                )
                            ),
                            columns = 2
                        )
                    }
                }
            }
        }

        // The flash: the whole screen lights up for a moment when you tap for the lightning.
        if (flash.value > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = flash.value }
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
            )
        }
    }
}

/** The distance in a saturated card; while counting, sound waves roll out of its bottom edge. */
@Composable
private fun StrikeHero(
    value: String,
    unit: String,
    hint: String,
    tone: TrailSenseTone,
    elapsedSeconds: Float?,
    modifier: Modifier = Modifier
) {
    val colors = trailSenseToneColors(tone)
    val container by animateColorAsState(colors.container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "strikeHero")
    val accent by animateColorAsState(colors.accent, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "strikeAccent")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseShape.hero),
        color = container,
        contentColor = colors.content
    ) {
        Box(Modifier.heightIn(min = 240.dp)) {
            val waves = rememberInfiniteTransition(label = "waves")
            val phase by waves.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
                label = "wavePhase"
            )
            val counting = elapsedSeconds != null
            val waveAlpha by animateFloatAsState(if (counting) 1f else 0.35f, label = "waveAlpha")
            Canvas(
                Modifier
                    .matchParentSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseShape.hero))
            ) {
                val center = Offset(size.width / 2f, size.height + 40.dp.toPx())
                val maxRadius = size.height * 1.1f
                repeat(4) { index ->
                    val t = ((if (counting) phase else 0.6f) + index / 4f) % 1f
                    drawCircle(
                        color = accent.copy(alpha = (1f - t) * 0.28f * waveAlpha),
                        radius = 60.dp.toPx() + t * maxRadius,
                        center = center,
                        style = Stroke(width = (10.dp.toPx() * (1f - t)).coerceAtLeast(1f))
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(TrailSenseSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                Text(
                    stringResource(R.string.sky_strike_distance),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.content.copy(alpha = 0.78f)
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.content.copy(alpha = 0.78f),
                        modifier = Modifier.padding(start = TrailSenseSpacing.sm, bottom = 10.dp)
                    )
                }
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = colors.content.copy(alpha = 0.86f)
                )
                AnimatedVisibility(visible = counting) {
                    SkyPill(
                        text = stringResource(
                            R.string.sky_seconds_short,
                            DecimalFormatter.format(elapsedSeconds ?: 0f, 1, true)
                        ),
                        icon = TrailIcons.Thunder,
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                        modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                    )
                }
            }
        }
    }
}

/** The one action: a big shaped button that names the event to tap for next and changes shape between them. */
@Composable
private fun StrikeButton(
    waitingForThunder: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (waitingForThunder) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strikeButtonColor"
    )
    val content = if (waitingForThunder) {
        MaterialTheme.colorScheme.onTertiary
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    val turn by animateFloatAsState(
        if (waitingForThunder) 30f else 0f,
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "strikeTurn"
    )
    val shape = if (waitingForThunder) MaterialShapes.SoftBurst.toShape() else MaterialShapes.Cookie12Sided.toShape()
    Box(
        modifier = Modifier
            .size(208.dp)
            .trailSensePressScale(interactionSource, 0.94f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(208.dp)
                .graphicsLayer { rotationZ = turn }
                .background(container, shape)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (waitingForThunder) TrailIcons.Thunder else TrailIcons.TorchOn,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(56.dp)
            )
            Text(
                text = stringResource(if (waitingForThunder) R.string.thunder else R.string.lightning),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = content
            )
        }
    }
}

private fun getDistance(lightning: Instant, thunder: Instant): Distance {
    return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
}

