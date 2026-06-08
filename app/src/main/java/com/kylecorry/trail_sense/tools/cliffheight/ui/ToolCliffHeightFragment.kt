package com.kylecorry.trail_sense.tools.cliffheight.ui

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.cliffheight.domain.CliffHeightService
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

class ToolCliffHeightFragment : TrailSenseComposeFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.disclaimer_message_title),
            getString(R.string.tool_cliff_height_disclaimer),
            "cache_dialog_tool_cliff_height",
            considerShownIfCancelled = true,
            cancelText = null
        )
    }

    @Composable
    override fun FragmentContent() {
        CliffHeightScreen(
            onOpenGuide = {
                UserGuideUtils.showGuide(this@ToolCliffHeightFragment, R.raw.guide_tool_cliff_height)
            }
        )
    }
}

@Composable
private fun CliffHeightScreen(
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val service = remember { CliffHeightService() }
    val formatter = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val gps = remember { SensorService(context).getGPS() }
    val units = remember { prefs.baseDistanceUnits }
    val location = remember { gps.location.takeIf { it != Coordinate.zero } }
    val singlePressDuration = remember { Duration.ofMillis(200) }
    var startTime by remember { mutableStateOf<Instant?>(null) }
    var running by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }

    LaunchedEffect(running, startTime, units, location) {
        while (running) {
            title = formatHeight(service, formatter, startTime, units, location)
            delay(16)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                CliffHeightHeader(
                    title = title,
                    onOpenGuide = onOpenGuide
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.tool_cliff_height_disclaimer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
                        .testTag("tide_disclaimer"),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(
                        if (running) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .pointerInput(running) {
                        detectTapGestures(
                            onPress = {
                                if (!running) {
                                    startTime = Instant.now()
                                    running = true
                                    val released = tryAwaitRelease()
                                    if (released && Duration.between(startTime, Instant.now()) > singlePressDuration) {
                                        running = false
                                    }
                                } else {
                                    running = false
                                }
                            }
                        )
                    }
                    .testTag("start_btn"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_tool_cliff_height),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp)
                )
            }
        }
    }
}

@Composable
private fun CliffHeightHeader(
    title: String,
    onOpenGuide: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
            .testTag("cliff_height_title")
    ) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.CenterStart),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            onClick = onOpenGuide,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null
            )
        }
    }
}

private fun formatHeight(
    service: CliffHeightService,
    formatter: FormatService,
    startTime: Instant?,
    units: DistanceUnits,
    location: Coordinate?
): String {
    val start = startTime ?: return ""
    val height = service.getCliffHeight(start, Instant.now(), location)
    val converted = height.convertTo(units)
    return formatter.formatDistance(converted, 2)
}
