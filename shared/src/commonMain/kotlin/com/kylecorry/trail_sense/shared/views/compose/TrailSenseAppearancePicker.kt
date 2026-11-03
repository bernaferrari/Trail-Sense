@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class
)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.TrailSenseColorStyle
import com.kylecorry.trail_sense.shared.views.TrailSensePalette
import com.kylecorry.trail_sense.shared.views.trailSenseColorScheme
import com.materialkolor.hct.Hct

/**
 * The family's appearance block (SDK Monitor, QuietGuard, WiFi Weakspot…): connected cards for the theme mode, the
 * color and, for the app's own colors, their style. Round outside, tight where they meet. Wallpaper colors come as
 * Android gives them, so the style card slides away while they are chosen.
 */
@Composable
fun <M> TrailSenseAppearancePicker(
    modes: List<TrailSenseChoice<M>>,
    selectedMode: M,
    onModeSelected: (M) -> Unit,
    palettes: List<TrailSensePalette>,
    selectedPalette: TrailSensePalette,
    onPaletteSelected: (TrailSensePalette) -> Unit,
    paletteLabel: (TrailSensePalette) -> String,
    selectedStyle: TrailSenseColorStyle,
    onStyleSelected: (TrailSenseColorStyle) -> Unit,
    styleLabel: (TrailSenseColorStyle) -> String,
    styleTitle: String,
    modifier: Modifier = Modifier
) {
    val card = MaterialTheme.colorScheme.surfaceContainerLow
    val seed = selectedPalette.seed
    val colorsBottom by animateDpAsState(
        if (seed != null) TrailSenseShape.groupInner else TrailSenseShape.groupOuter,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "colorsBottomCorner"
    )
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        TrailSenseConnectedChoice(
            options = modes,
            selected = selectedMode,
            onSelect = onModeSelected,
            modifier = Modifier
                .background(card, trailSenseGroupShape(TrailSenseGroupPosition(isFirst = true, isLast = false)))
                .padding(TrailSenseSpacing.lg)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    card,
                    RoundedCornerShape(
                        topStart = TrailSenseShape.groupInner,
                        topEnd = TrailSenseShape.groupInner,
                        bottomStart = colorsBottom,
                        bottomEnd = colorsBottom
                    )
                )
                .padding(TrailSenseSpacing.md)
                .selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
        ) {
            palettes.forEach { palette ->
                Swatch(
                    color = palette.seed ?: MaterialTheme.colorScheme.primary,
                    isWallpaper = palette.seed == null,
                    label = paletteLabel(palette),
                    isSelected = palette == selectedPalette,
                    onClick = { onPaletteSelected(palette) }
                )
            }
        }

        // The style card unfolds downward from the colors above it, and folds back up.
        AnimatedVisibility(
            visible = seed != null,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            ColorStylePicker(
                selected = selectedStyle,
                palette = if (seed != null) selectedPalette else TrailSensePalette.Default,
                dark = dark,
                label = styleLabel,
                title = styleTitle,
                onSelect = onStyleSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(card, trailSenseGroupShape(TrailSenseGroupPosition(isFirst = false, isLast = true)))
                    .padding(TrailSenseSpacing.md)
            )
        }
    }
}

/** A color: a softly lit swatch that rounds off and turns when chosen, with a check that grows in. */
@Composable
private fun Swatch(
    color: Color,
    isWallpaper: Boolean,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val brush = remember(color) {
        Brush.linearGradient(listOf(lerp(color, Color.White, 0.16f), color, lerp(color, Color.Black, 0.10f)))
    }
    val checkColor = when {
        isWallpaper -> MaterialTheme.colorScheme.onPrimary
        color.luminance() > 0.24f -> Color.Black.copy(alpha = 0.78f)
        else -> Color.White
    }
    val bouncy = spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val bouncyFloat = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val corner by animateDpAsState(if (isSelected) 8.dp else 18.dp, bouncy, label = "swatchCorner")
    val ring by animateFloatAsState(if (isSelected) 1f else 0f, bouncyFloat, label = "swatchRing")
    val ringColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
        label = "swatchRingColor"
    )
    val rotation by animateFloatAsState(
        if (isSelected) 0f else 45f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "swatchRotation"
    )
    val check by animateFloatAsState(if (isSelected) 1f else 0f, bouncyFloat, label = "swatchCheck")
    val ringWidth = (2.dp * ring).coerceAtLeast(0.dp)
    val inner = (corner - ringWidth - 2.dp).coerceAtLeast(0.dp)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (!isSelected) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onClick()
                    }
                }
            )
            .semantics {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .rotate(rotation)
                .clip(RoundedCornerShape(corner))
                .background(brush)
                .padding(ringWidth)
                .clip(RoundedCornerShape((corner - ringWidth).coerceAtLeast(0.dp)))
                .background(ringColor)
                .padding(2.dp)
                .clip(RoundedCornerShape(inner))
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            if (isWallpaper && !isSelected) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(-rotation),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(-rotation)
                    .graphicsLayer {
                        val progress = check.coerceAtLeast(0f)
                        scaleX = progress
                        scaleY = progress
                        alpha = check.coerceIn(0f, 1f)
                    },
                tint = checkColor
            )
        }
    }
}

/**
 * The color style as a connected group with no words, like the mode buttons above it: each segment is its style's
 * three accents blended into one soft mesh, so it shows the style's mood for the chosen color. The chosen segment
 * widens into a pill with a frosted check. Names stay for screen readers.
 */
@Composable
private fun ColorStylePicker(
    selected: TrailSenseColorStyle,
    palette: TrailSensePalette,
    dark: Boolean,
    label: (TrailSenseColorStyle) -> String,
    title: String,
    onSelect: (TrailSenseColorStyle) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp
) {
    Row(
        modifier
            .height(height)
            .selectableGroup()
            .semantics { contentDescription = title },
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        TrailSenseColorStyle.entries.forEachIndexed { index, style ->
            val mood = remember(palette, style, dark) {
                val scheme = trailSenseColorScheme(palette, style, dark)
                mood(listOf(scheme.primary, scheme.secondary, scheme.tertiary), dark)
            }
            Segment(
                label = label(style),
                mood = mood,
                selected = style == selected,
                first = index == 0,
                last = index == TrailSenseColorStyle.entries.lastIndex,
                height = height,
                onClick = { onSelect(style) }
            )
        }
    }
}

/** The three accents at one bright tone (deeper in dark mode), so every style is compared fairly. */
private fun mood(accents: List<Color>, dark: Boolean): List<Color> =
    accents.zip(if (dark) listOf(62.0, 68.0, 64.0) else listOf(72.0, 78.0, 74.0)) { color, tone ->
        Color(Hct.fromInt(color.toArgb()).withTone(tone).toInt())
    }

@Composable
private fun RowScope.Segment(
    label: String,
    mood: List<Color>,
    selected: Boolean,
    first: Boolean,
    last: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    val width by animateFloatAsState(
        if (selected) 1.7f else 1f,
        MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "styleWidth"
    )
    val dpSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>()
    val start by animateDpAsState(if (selected || first) height / 2 else 8.dp, dpSpec, label = "styleStart")
    val end by animateDpAsState(if (selected || last) height / 2 else 8.dp, dpSpec, label = "styleEnd")
    val shape = RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
    Box(
        Modifier
            .weight(width)
            .fillMaxHeight()
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { if (!selected) onClick() })
            .semantics { contentDescription = label }
            .drawBehind {
                val (primary, secondary, tertiary) = mood
                drawRect(Brush.horizontalGradient(listOf(primary, secondary)))
                val low = Offset(size.width * .85f, size.height)
                drawCircle(
                    Brush.radialGradient(listOf(tertiary, tertiary.copy(alpha = 0f)), low, size.width * .7f),
                    size.width * .7f,
                    low
                )
                val high = Offset(size.width * .1f, 0f)
                drawCircle(
                    Brush.radialGradient(listOf(primary, primary.copy(alpha = 0f)), high, size.width * .6f),
                    size.width * .6f,
                    high
                )
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .35f), Color.Transparent),
                        endY = size.height * .55f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val shown by animateFloatAsState(
            if (selected) 1f else 0f,
            MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "styleCheck"
        )
        if (shown > .01f) {
            Box(
                Modifier
                    .size(height * .58f)
                    .graphicsLayer {
                        alpha = shown.coerceIn(0f, 1f)
                        scaleX = .6f + .4f * shown
                        scaleY = .6f + .4f * shown
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = .78f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFF1B1B1F),
                    modifier = Modifier.size(height * .38f)
                )
            }
        }
    }
}
