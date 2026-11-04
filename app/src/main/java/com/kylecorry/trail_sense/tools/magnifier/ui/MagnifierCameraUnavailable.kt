@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.magnifier.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.delay

/**
 * The magnifier without a camera: one metaphor, a viewfinder. Its corner brackets pull inward twice like a camera
 * hunting for focus and settle, around a small scalloped camera-off badge. One warm action opens settings; "Not
 * now" goes back.
 */
@Composable
internal fun MagnifierCameraUnavailable(onNotNow: (() -> Unit)?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val bracket = scheme.primary
    val focus = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250)
        repeat(2) {
            focus.animateTo(1f, tween(260))
            focus.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.8f))
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val pull = focus.value * 14.dp.toPx()
                val arm = size.minDimension * 0.2f
                val stroke = 6.dp.toPx()
                val inset = stroke / 2f + pull
                val corners = listOf(
                    Offset(inset, inset) to (1f to 1f),
                    Offset(size.width - inset, inset) to (-1f to 1f),
                    Offset(inset, size.height - inset) to (1f to -1f),
                    Offset(size.width - inset, size.height - inset) to (-1f to -1f)
                )
                corners.forEach { (corner, dir) ->
                    drawLine(bracket, corner, corner + Offset(arm * dir.first, 0f), stroke, StrokeCap.Round)
                    drawLine(bracket, corner, corner + Offset(0f, arm * dir.second), stroke, StrokeCap.Round)
                }
            }
            TrailSenseShapeBadge(
                icon = Icons.Rounded.NoPhotography,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 112.dp,
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.size(TrailSenseSpacing.xl))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.no_camera_access),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.camera_unavailable_description),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }
        Spacer(Modifier.size(TrailSenseSpacing.xl))
        Button(
            onClick = { tryOrNothing { context.startActivity(Intents.appSettings(context)) } },
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.tertiary,
                contentColor = scheme.onTertiary
            ),
            modifier = Modifier
                .fillMaxWidth(0.56f)
                .heightIn(min = 56.dp)
        ) {
            Text(stringResource(R.string.open_settings), style = MaterialTheme.typography.titleMedium)
        }
        if (onNotNow != null) {
            TextButton(onClick = onNotNow, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.magnifier_not_now))
            }
        }
        Spacer(Modifier.weight(1.2f))
    }
}
