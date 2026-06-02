package com.kylecorry.trail_sense.tools.lightning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.lightning.domain.LightningStrike
import com.kylecorry.trail_sense.tools.lightning.infrastructure.persistence.LightningRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class FragmentToolLightning : Fragment() {
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val repo by lazy { LightningRepo.getInstance(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    LightningScreen(
                        formatService = formatService,
                        prefs = prefs,
                        loadLastStrike = {
                            withContext(Dispatchers.IO) {
                                repo.getLast()
                            }
                        },
                        saveStrike = {
                            withContext(Dispatchers.IO) {
                                repo.add(it)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LightningScreen(
    formatService: FormatService,
    prefs: UserPreferences,
    loadLastStrike: suspend () -> Reading<LightningStrike>?,
    saveStrike: suspend (Reading<LightningStrike>) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

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
        lastStrike = loadLastStrike()
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = distanceText,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                if (isDangerous) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.lightning_dangerously_close),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
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
                            saveStrike(currentStrike)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lightningTime == null) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (lightningTime == null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (lightningTime == null) R.drawable.ic_torch_on else R.drawable.ic_thunder
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(
                            if (lightningTime == null) R.string.lightning else R.string.thunder
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (previousStrikeText.isNotBlank()) {
                Text(
                    text = previousStrikeText,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun getDistance(lightning: Instant, thunder: Instant): Distance {
    return Distance.meters(Meteorology.getLightningStrikeDistance(lightning, thunder))
}
