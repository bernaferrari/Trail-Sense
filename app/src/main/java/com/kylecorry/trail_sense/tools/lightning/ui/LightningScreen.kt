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
import androidx.compose.foundation.layout.aspectRatio
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
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

/**
 * Lightning distance, an instrument: one ring holds the distance, with sound waves rolling out of it while you wait
 * for the thunder. Under it, one filled button names the event to tap for next (the flash, then the thunder). The
 * previous strike sits below as a quiet list.
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

    // An instrument with the large title like the other instruments; the ring, then its one button, then the
    // previous strike as a quiet list.
    com.kylecorry.trail_sense.shared.views.compose.TrailSensePage(
        title = stringResource(R.string.tool_lightning_title),
        onBack = com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        item(key = "strike") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                // One question: how far away was that strike? The ring answers it; the verdict says if it is too
                // close. The ring is a display only: the button under it is the one thing to tap.
                StrikeHero(
                    value = distanceValue,
                    unit = distanceUnit,
                    hint = when {
                        isDangerous -> stringResource(R.string.lightning_dangerously_close)
                        waitingForThunder -> stringResource(R.string.lightning_hint_thunder)
                        else -> stringResource(R.string.lightning_hint_both_taps)
                    },
                    danger = isDangerous,
                    elapsedSeconds = elapsed,
                    flash = flash.value,
                    modifier = Modifier.widthIn(max = TrailSenseSize.maximumContentWidth)
                )
                Box(Modifier.widthIn(max = TrailSenseSize.maximumContentWidth)) {
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
            }
        }

        item(key = "previous") {
            AnimatedVisibility(
                visible = lastStrike != null,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.lg)
            ) {
                val last = lastStrike
                if (last != null) {
                    val distance = last.value.distance.convertTo(units).toRelativeDistance()
                    val distanceText = formatService.formatDistance(
                        distance,
                        Units.getDecimalPlaces(distance.units),
                        false
                    )
                    val dangerous = Meteorology.isLightningStrikeDangerous(distance)
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup(
                        title = stringResource(R.string.previous)
                    ) {
                        row { position ->
                            SkyValueRow(
                                title = stringResource(R.string.sky_strike_last_distance),
                                value = distanceText,
                                valueColor = if (dangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                position = position
                            )
                        }
                        row { position ->
                            SkyValueRow(
                                title = stringResource(R.string.sky_strike_time),
                                value = formatService.formatTime(last.time, includeSeconds = false),
                                position = position
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The instrument: one ring on the plain page. Idle it is a still track with the bolt inside; while you count, sound
 * waves roll out of it (their motion is the count) and the distance grows inside. Its colour is primary, or error
 * when the strike is dangerously close. The verdict sits right under it as headlineMedium.
 */
@Composable
private fun StrikeHero(
    value: String,
    unit: String,
    hint: String,
    danger: Boolean,
    elapsedSeconds: Float?,
    flash: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val accent by animateColorAsState(
        if (danger) scheme.error else scheme.primary,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "strikeAccent"
    )
    val track = scheme.surfaceContainerHigh
    val counting = elapsedSeconds != null
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val waves = rememberInfiniteTransition(label = "waves")
            val phase by waves.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
                label = "wavePhase"
            )
            Canvas(Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val core = size.minDimension * 0.34f
                val maxRadius = size.minDimension / 2f
                // The flash lights the ring's face for a moment.
                drawCircle(androidx.compose.ui.graphics.lerp(track, accent.copy(alpha = 0.35f), flash), radius = core, center = center)
                drawCircle(accent, radius = core, center = center, style = Stroke(width = 3.dp.toPx()))
                if (counting) {
                    repeat(3) { index ->
                        val t = (phase + index / 3f) % 1f
                        drawCircle(
                            color = accent.copy(alpha = (1f - t) * 0.5f),
                            radius = core + t * (maxRadius - core),
                            center = center,
                            style = Stroke(width = (6.dp.toPx() * (1f - t)).coerceAtLeast(1f))
                        )
                    }
                }
            }
            if (value == "—") {
                // At rest the ring is a stopwatch at zero, so it reads as a display, not as a second button.
                Text(
                    stringResource(R.string.sky_seconds_short, DecimalFormatter.format(0f, 1, true)),
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            value,
                            style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                            modifier = Modifier.alignByBaseline()
                        )
                        Text(
                            unit,
                            style = MaterialTheme.typography.titleLarge,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = TrailSenseSpacing.xs)
                                .alignByBaseline()
                        )
                    }
                    if (elapsedSeconds != null) {
                        Text(
                            stringResource(
                                R.string.sky_seconds_short,
                                DecimalFormatter.format(elapsedSeconds, 1, true)
                            ),
                            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            hint,
            style = if (danger) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = if (danger) FontWeight.SemiBold else FontWeight.Normal,
            color = if (danger) scheme.error else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
        )
    }
}

/** The one action: a tall filled primary button that names the event to tap for next. */
@Composable
private fun StrikeButton(
    waitingForThunder: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val height = androidx.compose.material3.ButtonDefaults.LargeContainerHeight
    androidx.compose.material3.Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .trailSensePressScale(interactionSource, 0.97f),
        shapes = androidx.compose.material3.ButtonDefaults.shapesFor(height),
        contentPadding = androidx.compose.material3.ButtonDefaults.contentPaddingFor(height)
    ) {
        Icon(
            imageVector = if (waitingForThunder) TrailIcons.Thunder else androidx.compose.material.icons.Icons.Rounded.Bolt,
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
