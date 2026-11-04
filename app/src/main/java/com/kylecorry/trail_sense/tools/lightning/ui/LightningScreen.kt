@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.lightning.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.TorchOn
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Thunder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSensePressScale
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Lightning distance: the distance is the hero, one big button below it says which event to tap for next (the
 * flash, then the thunder), and the previous strike sits under it. The hint under the reading replaces the old
 * numbered instructions card, since the button already names each step.
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
    val previousStrikeText = lastStrike?.let {
        val distance = it.value.distance.convertTo(units).toRelativeDistance()
        val lastTime = context.getString(
            R.string.last_lightning_strike_at,
            formatService.formatTime(it.time, includeSeconds = false)
        )
        val lastDistance = formatService.formatDistance(
            distance,
            Units.getDecimalPlaces(distance.units),
            false
        )
        context.getString(R.string.dash_separated_pair, lastTime, lastDistance)
    }.orEmpty()

    val waitingForThunder = lightningTime != null
    val distanceValue = relativeDistance?.let {
        DecimalFormatter.format(it.value, Units.getDecimalPlaces(it.units), false)
    } ?: "--"
    val distanceUnit = formatService.getDistanceUnitName(relativeDistance?.units ?: units, true)

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
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.xxxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                val readingColor by animateColorAsState(
                    targetValue = if (isDangerous) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                    label = "readingColor"
                )
                TrailSenseReading(
                    value = distanceValue,
                    unit = distanceUnit,
                    color = readingColor
                )
                Text(
                    text = when {
                        isDangerous -> stringResource(R.string.lightning_dangerously_close)
                        waitingForThunder -> stringResource(R.string.lightning_hint_thunder)
                        else -> stringResource(R.string.lightning_hint_flash)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDangerous) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                StrikeButton(
                    waitingForThunder = waitingForThunder,
                    onClick = {
                        val startedAt = lightningTime
                        if (startedAt == null) {
                            lightningTime = Instant.now()
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
                    visible = previousStrikeText.isNotBlank(),
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeIn(),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut()
                ) {
                    TrailSenseListItem(
                        title = stringResource(R.string.previous),
                        subtitle = previousStrikeText,
                        icon = rememberVectorPainter(TrailIcons.Thunder),
                        position = TrailSenseGroupPosition.Single
                    )
                }
            }
        }
    }
}

/** The one action: a big button that names the event to tap for next. */
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
    val height = ButtonDefaults.LargeContainerHeight
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .trailSensePressScale(interactionSource, 0.97f),
        shapes = ButtonDefaults.shapesFor(height),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = ButtonDefaults.contentPaddingFor(height)
    ) {
        Icon(
            painter = rememberVectorPainter(
                if (waitingForThunder) TrailIcons.Thunder else TrailIcons.TorchOn
            ),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
        )
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
        Text(
            text = stringResource(if (waitingForThunder) R.string.thunder else R.string.lightning),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

private fun getDistance(lightning: Instant, thunder: Instant): Distance {
    return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
}
