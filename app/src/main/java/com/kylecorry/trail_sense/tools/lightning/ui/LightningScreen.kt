@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.lightning.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.kylecorry.trail_sense.shared.icons.trail.BaselineRefresh24
import com.kylecorry.trail_sense.shared.icons.trail.Thunder
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.math.max

/** A strike closer than this is dangerous: the 30-second flash-to-thunder rule. */
private const val DANGER_METERS = 10_000f

/** The sky the object shows at rest; it widens if the thunder takes longer to arrive. */
private const val SKY_METERS = 15_000f

/** A previous strike shows as a ghost ring only while it belongs to the same storm. */
private val GHOST_MAX_AGE: Duration = Duration.ofMinutes(30)

private enum class StrikeStage { Ready, Counting, Result }

/**
 * Lightning distance, like the boil timer: the object is the storm seen from above. The flash lights it, then the
 * sound spreads out of the middle as rings at the speed of sound, its distance riding on the front, until you tap at
 * the thunder and the front stops where the strike is. The danger zone fades in as the sound nears it; the previous
 * strike of the same storm is a ghost ring. One instruction (or verdict) line and one button that names what to tap
 * for next: Lightning, Thunder, Again. The sky is itself that button, so you can tap the storm without looking down.
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
        lastStrike = withContext(Dispatchers.IO) {
            repo.getLast()
        }
    }

    // While counting, the sound front moves every frame.
    LaunchedEffect(lightningTime) {
        if (lightningTime == null) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            now = Instant.now()
        }
    }

    val stage = when {
        lightningTime != null -> StrikeStage.Counting
        strike != null -> StrikeStage.Result
        else -> StrikeStage.Ready
    }
    val meters = when (stage) {
        StrikeStage.Counting -> getDistance(lightningTime!!, now).meters().value
        StrikeStage.Result -> strike!!.value.distance.meters().value
        StrikeStage.Ready -> null
    }
    val isDangerous = stage == StrikeStage.Result && (meters ?: Float.MAX_VALUE) <= DANGER_METERS
    val relative = meters?.let { Distance.meters(it).convertTo(units).toRelativeDistance() }
    val distanceValue = relative?.let { DecimalFormatter.format(it.value, Units.getDecimalPlaces(it.units), false) }
    val distanceUnit = formatService.getDistanceUnitName(relative?.units ?: units, true)
    // While the sound travels, its distance rides on the front, ticking up in the user's units.
    val liveLabel = relative?.takeIf { stage == StrikeStage.Counting }?.let {
        formatService.formatDistance(it, Units.getDecimalPlaces(it.units), false)
    }
    val ghostMeters = lastStrike
        ?.takeIf { Duration.between(it.time, Instant.now()) <= GHOST_MAX_AGE }
        ?.value?.distance?.meters()?.value
    val dangerLabel = Distance.meters(DANGER_METERS).convertTo(units).toRelativeDistance().let {
        formatService.formatDistance(it, 0, false)
    }

    val actionLabel = stringResource(
        when (stage) {
            StrikeStage.Ready -> R.string.lightning
            StrikeStage.Counting -> R.string.thunder
            StrikeStage.Result -> R.string.lightning_again
        }
    )

    // The one action, shared by the button and the sky itself: both tap for the next step.
    fun act() {
        when (stage) {
            StrikeStage.Ready -> {
                lightningTime = Instant.now()
                now = Instant.now()
                scope.launch {
                    flash.snapTo(1f)
                    flash.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
                }
            }

            StrikeStage.Counting -> {
                val thunder = Instant.now()
                val distance = getDistance(lightningTime!!, thunder)
                val currentStrike = Reading(LightningStrike(0, distance), thunder)
                strike = currentStrike
                lightningTime = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.add(currentStrike)
                    }
                }
            }

            StrikeStage.Result -> {
                // This strike becomes the ghost the next one is compared against.
                lastStrike = strike
                strike = null
            }
        }
    }

    TrailSensePage(
        title = stringResource(R.string.tool_lightning_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier
    ) {
        item(key = "storm") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StormSky(
                    stage = stage,
                    meters = meters,
                    value = distanceValue,
                    unit = distanceUnit,
                    liveLabel = liveLabel,
                    danger = isDangerous,
                    ghostMeters = ghostMeters,
                    dangerLabel = dangerLabel,
                    flash = flash.value,
                    actionLabel = actionLabel,
                    onClick = { act() }
                )
                // The one line: what to do next, or the verdict once the thunder came.
                val line = when {
                    stage == StrikeStage.Ready -> stringResource(R.string.lightning_hint_flash)
                    stage == StrikeStage.Counting -> stringResource(R.string.lightning_hint_thunder)
                    isDangerous -> stringResource(R.string.lightning_verdict_danger)
                    else -> stringResource(R.string.lightning_verdict_calm)
                }
                val lineColor by animateColorAsState(
                    if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    MaterialTheme.motionScheme.defaultEffectsSpec(),
                    label = "lineColor"
                )
                Text(
                    text = line,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = lineColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.lg, start = TrailSenseSpacing.lg, end = TrailSenseSpacing.lg)
                        .testTag("lightning_verdict")
                )
            }
        }

        item(key = "action") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.md),
                contentAlignment = Alignment.Center
            ) {
                TrailSenseButton(
                    text = actionLabel,
                    icon = when (stage) {
                        StrikeStage.Ready -> Icons.Rounded.Bolt
                        StrikeStage.Counting -> TrailIcons.Thunder
                        StrikeStage.Result -> TrailIcons.BaselineRefresh24
                    },
                    style = if (stage == StrikeStage.Result) TrailSenseButtonStyle.Tonal else TrailSenseButtonStyle.Filled,
                    onClick = { act() },
                    modifier = Modifier
                        .widthIn(max = TrailSenseSize.maximumContentWidth)
                        .fillMaxWidth()
                        .testTag("lightning_button")
                )
            }
        }
    }
}

/**
 * The storm from above. The face is still at rest, with the danger zone as a dashed circle. The flash lights the
 * whole face for a moment; while counting, the filled sound front grows from the middle at the speed of sound with
 * rings rolling out inside it; at the thunder the front stops at the strike, red inside the danger zone.
 */
@Composable
private fun StormSky(
    stage: StrikeStage,
    meters: Float?,
    value: String?,
    unit: String,
    liveLabel: String?,
    danger: Boolean,
    ghostMeters: Float?,
    dangerLabel: String,
    flash: Float,
    actionLabel: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<androidx.compose.ui.graphics.Color>()
    val accent by animateColorAsState(if (danger) scheme.error else scheme.primary, effects, label = "accent")
    val zoneFill by animateColorAsState(
        if (danger) scheme.errorContainer else scheme.surfaceContainerHigh,
        effects,
        label = "zone"
    )
    val face = scheme.surfaceContainerLow
    val zoneLine = if (danger) scheme.error else scheme.outline
    val ghostColor = scheme.onSurfaceVariant
    val ink = if (danger) scheme.onErrorContainer else scheme.onSurface
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = scheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()

    // The sky widens if the sound goes beyond it, so the front never leaves the object.
    val sky by animateFloatAsState(
        targetValue = max(SKY_METERS, (meters ?: 0f) * 1.15f),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "sky"
    )
    // The front settles with a spring when the thunder stops it.
    val front by animateFloatAsState(
        targetValue = meters ?: 0f,
        animationSpec = if (stage == StrikeStage.Counting) {
            tween(0)
        } else {
            MaterialTheme.motionScheme.defaultSpatialSpec()
        },
        label = "front"
    )
    val zoneTarget = when (stage) {
        StrikeStage.Ready -> 0f
        StrikeStage.Counting -> ((front - DANGER_METERS * 0.6f) / (DANGER_METERS * 0.4f)).coerceIn(0f, 1f)
        StrikeStage.Result -> 1f
    }
    val zoneAlpha by animateFloatAsState(
        targetValue = zoneTarget,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "zoneAlpha"
    )
    val pill = scheme.surfaceContainerHighest
    val liveStyle = MaterialTheme.typography.labelLarge.copy(
        color = scheme.onSurface,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.SemiBold
    )
    val rolling by rememberInfiniteTransition(label = "sound").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "roll"
    )
    val description = value?.let { "$it $unit" } ?: stringResource(R.string.lightning_hint_flash)
    // The sky is the button too: it presses in under the finger and ripples, then springs back.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "press"
    )

    Box(
        modifier = Modifier
            .padding(vertical = TrailSenseSpacing.sm)
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = actionLabel,
                onClick = onClick
            )
            .testTag("lightning_sky")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension / 2f
            val perMeter = radius / sky
            // The face, lit by the flash.
            drawCircle(lerp(face, scheme.primaryContainer, flash), radius)

            // The danger zone appears only as the sound nears it, and stays for the verdict; red when the strike
            // landed inside it.
            val zone = DANGER_METERS * perMeter
            if (zoneAlpha > 0f) {
                drawCircle(zoneFill.copy(alpha = (if (danger) 0.7f else 0.6f) * zoneAlpha), zone)
                drawCircle(
                    zoneLine.copy(alpha = 0.7f * zoneAlpha),
                    zone,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
                    )
                )
                val label = measurer.measure(dangerLabel, labelStyle)
                drawText(
                    label,
                    topLeft = Offset(center.x - label.size.width / 2f, center.y - zone + 6.dp.toPx()),
                    alpha = zoneAlpha
                )
            }

            // The last strike of this storm, as a ghost to compare with.
            ghostMeters?.let { ghost ->
                drawCircle(
                    ghostColor.copy(alpha = 0.45f),
                    (ghost * perMeter).coerceAtMost(radius - 2.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))
                    )
                )
            }

            // The sound: a soft filled disc out to the front, rings rolling out inside it while you wait.
            // A front only a few pixels wide (a strike a few hundred metres off, on a 10 km scale) would collapse
            // into a stray dot under the number, so it is drawn only once it is a real ring.
            if (stage != StrikeStage.Ready && front > 0f && front * perMeter > 14.dp.toPx()) {
                val r = (front * perMeter).coerceAtMost(radius)
                drawCircle(accent.copy(alpha = 0.14f), r)
                if (stage == StrikeStage.Counting) {
                    // Each ring is a second-and-a-bit of sound; they keep the front's speed.
                    val spacing = 343f * 1.2f * perMeter
                    var ring = r - rolling * spacing
                    while (ring > 0f) {
                        drawCircle(
                            accent.copy(alpha = 0.45f * (ring / r)),
                            ring,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        ring -= spacing
                    }
                }
                drawCircle(accent, r, style = Stroke(width = 4.dp.toPx()))

                // The live distance rides on the top of the front, kept clear of the strike in the middle.
                if (liveLabel != null) {
                    val text = measurer.measure(liveLabel, liveStyle)
                    val padX = 10.dp.toPx()
                    val padY = 4.dp.toPx()
                    val w = text.size.width + padX * 2
                    val h = text.size.height + padY * 2
                    val y = (center.y - r).coerceAtMost(center.y - 36.dp.toPx())
                    val topLeft = Offset(center.x - w / 2f, y - h / 2f)
                    drawRoundRect(
                        pill,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f)
                    )
                    drawRoundRect(
                        accent,
                        topLeft = topLeft,
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawText(text, topLeft = Offset(topLeft.x + padX, topLeft.y + padY))
                }
            }

            // The flash itself: a burst of light that fades from the middle.
            if (flash > 0f) {
                drawCircle(scheme.primary.copy(alpha = 0.35f * flash), radius * (0.4f + 0.6f * (1f - flash)))
            }
        }

        val showDistance = stage == StrikeStage.Result && value != null
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag("lightning_title_container")
        ) {
            Icon(
                Icons.Rounded.Bolt,
                contentDescription = null,
                tint = lerp(if (stage == StrikeStage.Ready) scheme.onSurfaceVariant else accent, scheme.primary, flash),
                modifier = Modifier
                    .size(
                        when (stage) {
                            StrikeStage.Ready -> 56.dp
                            StrikeStage.Counting -> 36.dp
                            StrikeStage.Result -> 28.dp
                        }
                    )
                    .graphicsLayer {
                        scaleX = 1f + 0.4f * flash
                        scaleY = 1f + 0.4f * flash
                    }
            )
            if (showDistance) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value!!,
                        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = ink,
                        modifier = Modifier
                            .alignByBaseline()
                            .testTag("lightning_title")
                    )
                    Text(
                        unit,
                        style = MaterialTheme.typography.titleLarge,
                        color = ink.copy(alpha = 0.75f),
                        modifier = Modifier
                            .padding(start = TrailSenseSpacing.xs)
                            .alignByBaseline()
                    )
                }
            }
        }
    }
}

private fun getDistance(lightning: Instant, thunder: Instant): Distance {
    return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
}
