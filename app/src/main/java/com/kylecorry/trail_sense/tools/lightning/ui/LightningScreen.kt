@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.lightning.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

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
    val distanceText = relativeDistance?.let {
        formatService.formatDistance(
            it,
            Units.getDecimalPlaces(it.units),
            false
        )
    }.orEmpty()
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TrailSenseSpacing.screen, vertical = TrailSenseSpacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
            // Hero distance readout in a tonal primaryContainer card
            DistanceHero(
                distanceText = distanceText,
                isDangerous = isDangerous,
                waitingForThunder = waitingForThunder,
                modifier = Modifier.fillMaxWidth()
            )

            // Primary record action
            TrailSenseButton(
                text = stringResource(
                    if (!waitingForThunder) R.string.lightning else R.string.thunder
                ),
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
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Instructions and previous strike grouped in a surface card
            InfoCard(
                previousStrikeText = previousStrikeText,
                modifier = Modifier.fillMaxWidth()
            )
        }
        }
    }
}

@Composable
private fun DistanceHero(
    distanceText: String,
    isDangerous: Boolean,
    waitingForThunder: Boolean,
    modifier: Modifier = Modifier
) {
    val hasReading = distanceText.isNotBlank()

    val containerColor by animateColorAsState(
        targetValue = when {
            isDangerous -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(durationMillis = 250),
        label = "heroContainer"
    )
    val onContainerColor by animateColorAsState(
        targetValue = when {
            isDangerous -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = tween(durationMillis = 250),
        label = "heroOnContainer"
    )

    // Spring-animate the readout so a new distance value pops into place.
    val valueScale by animateFloatAsState(
        targetValue = if (hasReading) 1f else 0.96f,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "heroValueScale"
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = onContainerColor
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.xl,
                vertical = TrailSenseSpacing.xxl
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Text(
                text = if (hasReading) distanceText else "—",
                modifier = Modifier.scale(valueScale),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = onContainerColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    when {
                        isDangerous -> R.string.lightning_dangerously_close
                        waitingForThunder -> R.string.thunder
                        else -> R.string.lightning
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (isDangerous) {
                    onContainerColor
                } else {
                    onContainerColor.copy(alpha = 0.7f)
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCard(
    previousStrikeText: String,
    modifier: Modifier = Modifier
) {
    TrailSenseCard(modifier = modifier) {
        // Instructions section
        Text(
            text = stringResource(R.string.instructions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
        InstructionStep(
            number = stringResource(R.string.one),
            iconRes = R.drawable.ic_torch_on,
            text = stringResource(R.string.lightning)
        )
        Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
        InstructionStep(
            number = stringResource(R.string.two),
            iconRes = R.drawable.ic_thunder,
            text = stringResource(R.string.thunder)
        )

        // Previous strike section
        if (previousStrikeText.isNotBlank()) {
            Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
            Text(
                text = stringResource(R.string.previous),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(TrailSenseSpacing.xs))
            Text(
                text = previousStrikeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstructionStep(
    number: String,
    iconRes: Int,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(TrailSenseSpacing.lg))
        Text(
            text = "$number. $text",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getDistance(lightning: Instant, thunder: Instant): Distance {
    return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
}
