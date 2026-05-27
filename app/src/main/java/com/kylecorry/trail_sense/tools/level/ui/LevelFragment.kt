package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.fragments.TrailSenseComposeFragment
import com.kylecorry.andromeda.sense.level.Level
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.ui.compose.TrailSenseSpacing
import kotlin.math.abs
import kotlin.math.min

/**
 * Level tool entry as TrailSenseComposeFragment (legacy deep link / PendingIntent compat).
 * Pure Compose Canvas implementation — no XML, no custom views, expressive M3.
 * Provides LevelContent() for Nav3 host.
 */
class LevelFragment : TrailSenseComposeFragment() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    @Composable
    fun LevelContent() {
        val context = androidx.compose.ui.platform.LocalContext.current
        var x by remember { mutableFloatStateOf(0f) }
        var y by remember { mutableFloatStateOf(0f) }

        // Fully Compose sensor lifecycle (expressive, no legacy fragment ties in UI)
        DisposableEffect(Unit) {
            val sensorService = SensorService(context)
            val level = Level(sensorService.getOrientation())
            level.start {
                x = level.x
                y = -level.y
                true
            }
            onDispose {
                level.stop { true }
            }
        }

        LevelBubbleContent(
            xAngle = x,
            yAngle = y,
            formatService = formatService,
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun LevelBubbleContent(
        xAngle: Float,
        yAngle: Float,
        formatService: FormatService,
        modifier: Modifier = Modifier
    ) {
        val isLevel = abs(xAngle) < 2.5f && abs(yAngle) < 2.5f
        val statusColor = if (isLevel) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiary
        }
        val statusText = if (isLevel) "LEVEL" else "TILTED"

        Column(
            modifier = modifier
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            // Expressive status pill
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(bottom = TrailSenseSpacing.sm)
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Live angle readout (matches test expectations for bubble_level_angles)
            Text(
                text = stringResource(
                    R.string.bubble_level_angles,
                    formatService.formatDegrees(abs(xAngle), 1),
                    formatService.formatDegrees(abs(yAngle), 1)
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(TrailSenseSpacing.md))

            // Expressive bubble visualization (pure Canvas, large touch-friendly)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(TrailSenseSpacing.md)
                    .testTag("level"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(320.dp)
                        .testTag("level_canvas")
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = min(size.width, size.height) / 2f - 12.dp.toPx()

                    // Outer frame (expressive ring)
                    drawCircle(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 6.dp.toPx())
                    )

                    // Inner guide circle
                    drawCircle(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        radius = radius * 0.6f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Crosshairs (subtle, M3 toned)
                    val crossLen = radius * 0.85f
                    val crossColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    drawLine(
                        color = crossColor,
                        start = Offset(cx - crossLen, cy),
                        end = Offset(cx + crossLen, cy),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = crossColor,
                        start = Offset(cx, cy - crossLen),
                        end = Offset(cx, cy + crossLen),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Center target dot
                    drawCircle(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )

                    // The bubble (expressive, scaled movement, color state)
                    val maxTilt = 25f // degrees for full deflection
                    val bubbleRadius = 22.dp.toPx()
                    val maxOffset = radius - bubbleRadius - 8.dp.toPx()

                    val normX = (xAngle / maxTilt).coerceIn(-1f, 1f)
                    val normY = (yAngle / maxTilt).coerceIn(-1f, 1f)

                    val bubbleX = cx - normX * maxOffset   // invert for natural bubble physics
                    val bubbleY = cy - normY * maxOffset

                    // Bubble shadow for depth (expressive)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = bubbleRadius + 2.dp.toPx(),
                        center = Offset(bubbleX + 3.dp.toPx(), bubbleY + 3.dp.toPx())
                    )

                    // Main bubble
                    val bubbleColor = if (isLevel) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                    drawCircle(
                        color = bubbleColor,
                        radius = bubbleRadius,
                        center = Offset(bubbleX, bubbleY)
                    )

                    // Inner highlight on bubble
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = bubbleRadius * 0.45f,
                        center = Offset(bubbleX - bubbleRadius * 0.25f, bubbleY - bubbleRadius * 0.25f)
                    )
                }
            }

            // Hint
            Text(
                text = "Keep phone flat for best results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
