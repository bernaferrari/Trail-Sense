@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.toPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.material3.toShape
import androidx.graphics.shapes.RoundedPolygon
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

/** The Material shape each beacon shape is drawn with. */
fun BeaconShape.polygon(): RoundedPolygon = when (this) {
    BeaconShape.Cookie -> MaterialShapes.Cookie9Sided
    BeaconShape.Circle -> MaterialShapes.Circle
    BeaconShape.Square -> MaterialShapes.Square
    BeaconShape.Clover -> MaterialShapes.Clover4Leaf
    BeaconShape.Sunny -> MaterialShapes.Sunny
    BeaconShape.Flower -> MaterialShapes.Flower
    BeaconShape.Gem -> MaterialShapes.Gem
    BeaconShape.Puffy -> MaterialShapes.Puffy
}

@Composable
fun BeaconShape.toComposeShape(): Shape = polygon().toShape()

@Composable
private fun BeaconShape.label(): String = stringResource(
    when (this) {
        BeaconShape.Cookie -> R.string.places_shape_cookie
        BeaconShape.Circle -> R.string.places_shape_circle
        BeaconShape.Square -> R.string.places_shape_square
        BeaconShape.Clover -> R.string.places_shape_clover
        BeaconShape.Sunny -> R.string.places_shape_sun
        BeaconShape.Flower -> R.string.places_shape_flower
        BeaconShape.Gem -> R.string.places_shape_gem
        BeaconShape.Puffy -> R.string.places_shape_puffy
    }
)

/** Black or white, whichever reads on [background]. */
fun beaconInkOn(background: Color): Color = if (background.luminance() > 0.45f) Color(0xFF1B1B1F) else Color.White

/**
 * ChangeDetection's icon sheet, for a beacon: the sheet *is* the beacon. A large live preview on a soft glow of its
 * colour, then the shapes as real previews (the chosen one ringed by an outline that hugs it, with a small wobble as
 * it is picked), then the colours. Choices apply as they are made, so the sheet needs no button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconShapeSheet(
    icon: ImageVector,
    color: AppColor,
    shape: BeaconShape,
    onShape: (BeaconShape) -> Unit,
    onColor: (AppColor) -> Unit,
    onDismiss: () -> Unit
) {
    val title = stringResource(R.string.places_shape_and_color)
    val haptics = LocalHapticFeedback.current
    // The title stays in the content, centred under the live preview it names
    TrailSenseSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.semantics { paneTitle = title }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Stage(icon, Color(color.color), shape)
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BoxWithConstraints(Modifier.widthIn(max = 400.dp)) {
                val inset = ((maxWidth / 4 - SHAPE_SIZE) / 2).coerceAtLeast(0.dp)
                Column {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BeaconShape.entries.chunked(4).forEach { row ->
                            Row {
                                row.forEach { option ->
                                    ShapeChoice(
                                        shape = option,
                                        icon = icon,
                                        color = Color(color.color),
                                        chosen = option == shape,
                                        ring = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onShape(option)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    ColorGrid(
                        current = color,
                        ring = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 20.dp, start = inset, end = inset),
                        onSelect = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onColor(it)
                        }
                    )
                }
            }
        }
    }
}

/** The beacon large, on a faint halo of its colour. */
@Composable
private fun Stage(icon: ImageVector, color: Color, shape: BeaconShape) {
    val glow by animateColorAsState(color.copy(alpha = 0.22f), label = "glow")
    val container by animateColorAsState(color, label = "stage")
    Box(
        Modifier
            .padding(top = 4.dp, bottom = 12.dp)
            .size(136.dp)
            .drawBehind {
                val radius = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center, radius), radius)
            },
        contentAlignment = Alignment.Center
    ) {
        BeaconShapeMark(shape, container, icon, 96.dp)
    }
}

/** A beacon's mark: its icon on its shape in its colour. */
@Composable
fun BeaconShapeMark(shape: BeaconShape, color: Color, icon: ImageVector?, size: Dp, modifier: Modifier = Modifier) {
    val outline = shape.toComposeShape()
    androidx.compose.material3.Surface(
        modifier = modifier.size(size),
        shape = outline,
        color = color,
        contentColor = beaconInkOn(color)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.46f))
            }
        }
    }
}

/** One shape, as the real result; the chosen one gets a ring hugging its outline and a small wobble. */
@Composable
private fun ShapeChoice(
    shape: BeaconShape,
    icon: ImageVector,
    color: Color,
    chosen: Boolean,
    ring: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        if (chosen) 1f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chosen"
    )
    val polygon = remember(shape) { shape.polygon() }
    val label = shape.label()
    Box(
        modifier
            .height(SHAPE_SIZE + 20.dp)
            .semantics { contentDescription = label }
            .selectable(
                selected = chosen,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!chosen) onClick() },
        contentAlignment = Alignment.Center
    ) {
        // The ring: the same outline, a little larger, drawn as the choice settles in.
        Canvas(Modifier.size(SHAPE_SIZE + 14.dp)) {
            if (progress > 0.01f) {
                val native = polygon.toPath()
                native.transform(android.graphics.Matrix().apply { setScale(size.width, size.height) })
                drawPath(native.asComposePath(), ring.copy(alpha = progress.coerceIn(0f, 1f)), style = Stroke(width = 2.5.dp.toPx()))
            }
        }
        BeaconShapeMark(
            shape,
            color,
            icon,
            SHAPE_SIZE,
            Modifier.graphicsLayer {
                rotationZ = if (chosen) (1f - progress) * -12f else 0f
                val s = 0.9f + 0.1f * progress
                scaleX = s
                scaleY = s
            }
        )
    }
}

/** The colours, two rows of five, flush with the shapes above. */
@Composable
private fun ColorGrid(current: AppColor, ring: Color, modifier: Modifier = Modifier, onSelect: (AppColor) -> Unit) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = ((maxWidth - SWATCH_SIZE * 5) / 4).coerceAtLeast(8.dp)
        Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(gap)) {
            AppColor.entries.chunked(5).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { option ->
                        val chosen = option == current
                        Box(
                            Modifier
                                .size(SWATCH_SIZE + 8.dp)
                                .semantics { contentDescription = option.name }
                                .selectable(
                                    selected = chosen,
                                    role = Role.RadioButton,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { if (!chosen) onSelect(option) },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                if (chosen) {
                                    drawCircle(ring, radius = size.minDimension / 2, style = Stroke(2.5.dp.toPx()))
                                }
                                drawCircle(Color(option.color), radius = SWATCH_SIZE.toPx() / 2 - (if (chosen) 2.dp.toPx() else 0f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private val SHAPE_SIZE = 52.dp
private val SWATCH_SIZE = 40.dp
