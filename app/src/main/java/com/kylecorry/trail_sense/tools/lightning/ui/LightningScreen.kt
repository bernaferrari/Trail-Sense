@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.lightning.ui

import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
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
    } ?: "—"
    val distanceUnit = formatService.getDistanceUnitName(relativeDistance?.units ?: units, true)
    val elapsed = lightningTime?.let { Duration.between(it, now).toMillis() / 1000f }

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_lightning_title),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
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
                flash = flash.value,
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .weight(1f)
            ) {
            StrikeButton(
                waitingForThunder = waitingForThunder,
                onClick = {
                    val startedAt = lightningTime
                    if (startedAt == null) {
                        lightningTime = Instant.now()
                        scope.launch {
                            flash.snapTo(0.9f)
                            flash.animateTo(0f, tween(550))
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
            }

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

    }
}

/**
 * The instrument: one tall saturated card. A small shaped bolt badge and the label at the top, the distance at
 * display size in the middle (sound waves roll out behind it while counting), and the action as a large button at
 * its foot. The card lights up for a moment when you tap for the flash.
 */
@Composable
private fun StrikeHero(
    value: String,
    unit: String,
    hint: String,
    tone: TrailSenseTone,
    elapsedSeconds: Float?,
    flash: Float,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit
) {
    val colors = trailSenseToneColors(tone)
    val container by animateColorAsState(colors.container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "strikeHero")
    val accent by animateColorAsState(colors.accent, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "strikeAccent")
    val lit = androidx.compose.ui.graphics.lerp(container, MaterialTheme.colorScheme.surfaceContainerLowest, flash)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseShape.hero),
        color = lit,
        contentColor = colors.content
    ) {
        Box(Modifier.fillMaxSize()) {
            val waves = rememberInfiniteTransition(label = "waves")
            val phase by waves.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
                label = "wavePhase"
            )
            val counting = elapsedSeconds != null
            val waveAlpha by animateFloatAsState(if (counting) 1f else 0.55f, label = "waveAlpha")
            Canvas(
                Modifier
                    .matchParentSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseShape.hero))
            ) {
                val center = Offset(size.width / 2f, size.height * 0.45f)
                val maxRadius = size.maxDimension * 0.75f
                // A deep well under the reading, so the rings feel like a target rather than lines on a card.
                drawCircle(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0f)),
                        center = center,
                        radius = maxRadius * 0.7f
                    ),
                    radius = maxRadius * 0.7f,
                    center = center
                )
                repeat(4) { index ->
                    val t = ((if (counting) phase else 0.55f) + index / 4f) % 1f
                    drawCircle(
                        color = accent.copy(alpha = (1f - t) * 0.5f * waveAlpha),
                        radius = 124.dp.toPx() + t * maxRadius,
                        center = center,
                        style = Stroke(width = (12.dp.toPx() * (1f - t)).coerceAtLeast(1f))
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(TrailSenseSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(TrailSenseSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkyFocalShape(
                        icon = androidx.compose.material.icons.Icons.Rounded.Bolt,
                        shape = MaterialShapes.Cookie12Sided.toShape(),
                        size = 44.dp,
                        containerColor = colors.accent,
                        contentColor = colors.onAccent,
                        spin = counting
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.md))
                    Text(
                        stringResource(R.string.sky_strike_distance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    AnimatedVisibility(visible = counting) {
                        SkyPill(
                            text = stringResource(
                                R.string.sky_seconds_short,
                                DecimalFormatter.format(elapsedSeconds ?: 0f, 1, true)
                            ),
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                val placeholder = value == "—"
                // Idle: no dash. A big turning bolt and the instruction as the headline.
                if (placeholder) {
                    SkyFocalShape(
                        icon = androidx.compose.material.icons.Icons.Rounded.Bolt,
                        shape = MaterialShapes.Burst.toShape(),
                        size = 128.dp,
                        containerColor = accent,
                        contentColor = colors.onAccent,
                        spin = true
                    )
                    Spacer(Modifier.height(TrailSenseSpacing.xl))
                    Text(
                        hint,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        value,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 104.sp,
                            lineHeight = 108.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.SemiBold,
                        modifier = if (placeholder) Modifier else Modifier.alignByBaseline()
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.content.copy(alpha = 0.78f),
                        modifier = Modifier
                            .padding(start = TrailSenseSpacing.sm)
                            .then(if (placeholder) Modifier else Modifier.alignByBaseline())
                    )
                }
                if (!placeholder) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = colors.content.copy(alpha = 0.86f),
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                Spacer(Modifier.weight(1f))
                action()
            }
        }
    }
}

/** The one action: a tall button at the foot of the card that names the event to tap for next. */
@Composable
private fun StrikeButton(
    waitingForThunder: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        // The flash is the screen's main action, in the warm action role; the thunder tap that follows is primary.
        targetValue = if (waitingForThunder) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strikeButtonColor"
    )
    val content = if (waitingForThunder) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onTertiary
    }

    val height = androidx.compose.material3.ButtonDefaults.LargeContainerHeight
    androidx.compose.material3.Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .trailSensePressScale(interactionSource, 0.97f),
        shapes = androidx.compose.material3.ButtonDefaults.shapesFor(height),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = androidx.compose.material3.ButtonDefaults.contentPaddingFor(height)
    ) {
        Icon(
            imageVector = if (waitingForThunder) TrailIcons.Thunder else TrailIcons.TorchOn,
            contentDescription = null,
            modifier = Modifier.size(androidx.compose.material3.ButtonDefaults.iconSizeFor(height))
        )
        Spacer(Modifier.width(androidx.compose.material3.ButtonDefaults.iconSpacingFor(height)))
        Text(
            text = stringResource(if (waitingForThunder) R.string.thunder else R.string.lightning),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun getDistance(lightning: Instant, thunder: Instant): Distance {
    return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
}
