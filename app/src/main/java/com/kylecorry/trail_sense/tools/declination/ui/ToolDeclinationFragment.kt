package com.kylecorry.trail_sense.tools.declination.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ToolDeclinationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    DeclinationScreen()
                }
            }
        }
    }
}

@Composable
private fun DeclinationScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    var location by remember { mutableStateOf(sensors.lastKnownLocation) }
    var isLocationUpToDate by remember { mutableStateOf(false) }
    var declination by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        location = withContext(Dispatchers.IO) {
            sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.RefreshIfInvalid)
        }
        isLocationUpToDate = true
    }

    LaunchedEffect(location) {
        declination = withContext(Dispatchers.Default) {
            Geophysics.getGeomagneticDeclination(location)
        }
    }

    val title = remember(declination, formatter) {
        if (Arithmetic.isZero(declination, 0.05f)) {
            formatter.formatDegrees(declination, 1)
        } else {
            "${formatter.formatDegrees(declination, 1)} (${
                formatter.formatDirection(
                    if (declination < 0) {
                        CompassDirection.West
                    } else {
                        CompassDirection.East
                    }
                )
            })"
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            DeclinationHeader(title = title)
            CoordinateInput(
                location = location,
                onLocationChanged = {
                    location = it
                    isLocationUpToDate = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("location")
            )
            Text(
                text = if (isLocationUpToDate) "" else stringResource(R.string.loading),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("loading"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DeclinationArrow(
                    declination = declination,
                    modifier = Modifier
                        .size(200.dp)
                        .testTag("declination")
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DeclinationHeader(
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("title")
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.declination),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CoordinateInput(
    location: Coordinate,
    onLocationChanged: (Coordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val input = remember {
        CoordinateInputView(context, null).apply {
            coordinate = location
            setOnCoordinateChangeListener {
                if (it != null) {
                    onLocationChanged(it)
                }
            }
        }
    }
    LaunchedEffect(location) {
        input.coordinate = location
    }
    DisposableEffect(input) {
        onDispose {
            input.pause()
        }
    }
    AndroidView(
        factory = { input },
        modifier = modifier,
        update = {
            it.coordinate = location
        }
    )
}

@Composable
private fun DeclinationArrow(
    declination: Float,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val imageSize = 24.dp.toPx()
        val offsetFromTop = imageSize * 1.25f
        val arrowVerticalOffset = imageSize * 2.25f
        val arrowHorizontalOffset = imageSize / 2f
        val centerX = size.width / 2f
        val bottom = size.height - strokeWidth

        drawLine(
            color = color,
            start = Offset(centerX, bottom),
            end = Offset(centerX, offsetFromTop),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawStar(color, Offset(centerX, imageSize * 0.5f), imageSize)

        rotate(
            degrees = declination,
            pivot = Offset(centerX, size.height)
        ) {
            drawLine(
                color = color,
                start = Offset(centerX, bottom),
                end = Offset(centerX, offsetFromTop),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            if (declination <= 0f) {
                drawLine(
                    color = color,
                    start = Offset(centerX - arrowHorizontalOffset, arrowVerticalOffset),
                    end = Offset(centerX, offsetFromTop),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            if (declination >= 0f) {
                drawLine(
                    color = color,
                    start = Offset(centerX + arrowHorizontalOffset, arrowVerticalOffset),
                    end = Offset(centerX, offsetFromTop),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    color: Color,
    center: Offset,
    size: Float
) {
    val radius = size / 2f
    val path = Path()
    val points = 10
    for (i in 0 until points) {
        val angle = -Math.PI / 2 + i * Math.PI / 5
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val point = Offset(
            x = center.x + (kotlin.math.cos(angle) * r).toFloat(),
            y = center.y + (kotlin.math.sin(angle) * r).toFloat()
        )
        if (i == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }
    path.close()
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}
