package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

/**
 * Keepout's settings grid, shared: tiles that read as one connected group (round outside, tight where they meet,
 * 2 dp apart), on the same quiet container as list rows, each with an icon well in its own accent. The only color on
 * the page lives in the wells; the accent is pulled toward the user's palette so it always belongs.
 */
@Immutable
data class TrailSenseTile(
    val key: Any,
    val title: String,
    val accent: Color,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
    val subtitle: String? = null
)

/** The family's tile accents (Keepout's set): distinct hues that each harmonize into any palette. */
object TrailSenseAccents {
    val Sky = Color(0xFF74C5FF)
    val Lavender = Color(0xFFC5ACFF)
    val Coral = Color(0xFFFF907F)
    val Mint = Color(0xFF46EBC8)
    val Periwinkle = Color(0xFFA3BCFF)
    val Sun = Color(0xFFFFD878)
    val Leaf = Color(0xFF7EED92)
    val Rose = Color(0xFFFFA7E0)
    val Violet = Color(0xFF9B8CFF)
    val Blush = Color(0xFFFF9EC4)
    val Apricot = Color(0xFFFFB86B)
    val Seafoam = Color(0xFF7FE0D0)
    val Stone = Color(0xFFC9C3B8)
}

@Immutable
data class TrailSenseWellColors(val container: Color, val content: Color)

/**
 * One rule for every well, whatever its hue: the accent's container tone with its on-container icon (pale well and
 * dark icon in light mode, deep well and pale icon in dark mode).
 */
@Composable
fun trailSenseWellColors(accent: Color): TrailSenseWellColors {
    val tones = trailSenseAccentScheme(accent)
    return TrailSenseWellColors(tones.primaryContainer, tones.onPrimaryContainer)
}

/**
 * The full tonal palette of an accent, harmonized toward the theme: its container for tinted tiles, its primary for
 * badges. Tonal rather than vibrant, so greens and cyans stay soft instead of turning neon beside the theme.
 */
@Composable
fun trailSenseAccentScheme(accent: Color): androidx.compose.material3.ColorScheme {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val hue = if (accent == Color.Unspecified) scheme.primary else harmonize(accent, scheme.primary)
    return remember(hue, dark) {
        dynamicColorScheme(
            seedColor = hue,
            isDark = dark,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
    }
}

/** The 40 dp rounded icon well every tile and row uses. */
@Composable
fun TrailSenseIconWell(colors: TrailSenseWellColors, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.container,
        contentColor = colors.content,
        modifier = modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/** The shape of one cell of a connected grid: outer corners on the group's outside, small ones inside. */
fun trailSenseGridCellShape(column: Int, columns: Int, row: Int, rows: Int): RoundedCornerShape {
    fun corner(outer: Boolean) = if (outer) TrailSenseShape.groupOuter else TrailSenseShape.groupInner
    val top = row == 0
    val bottom = row == rows - 1
    val start = column == 0
    val end = column == columns - 1
    return RoundedCornerShape(
        topStart = corner(start && top),
        topEnd = corner(end && top),
        bottomEnd = corner(end && bottom),
        bottomStart = corner(start && bottom)
    )
}

/**
 * Tiles in a connected grid: two columns on a phone, more on wide screens. A short last row stretches its tiles
 * across the full width, so the grid never shows a hole. Tiles in a row share one height, and every title on the grid uses one
 * size, the largest that keeps the longest title on one line beside its icon.
 */
@Composable
fun TrailSenseTileGrid(tiles: List<TrailSenseTile>, modifier: Modifier = Modifier, titleSize: TextUnit? = null) {
    if (tiles.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 600.dp) 3 else 2
        val size = titleSize ?: rememberTileTitleSize(tiles.map { it.title }, maxWidth, columns)
        val rows = tiles.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                ) {
                    row.forEachIndexed { column, tile ->
                        // A short last row stretches across the width, so the grid never shows a hole.
                        val last = rowIndex == rows.lastIndex
                        val cellColumns = if (last) row.size else columns
                        Tile(
                            tile = tile,
                            shape = trailSenseGridCellShape(column, cellColumns, rowIndex, rows.size),
                            titleSize = size,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

/** The title size for tiles across [width] in [columns]: full size unless the longest title wouldn't fit. */
@Composable
fun rememberTileTitleSize(titles: List<String>, width: Dp, columns: Int = 2): TextUnit {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
    val cell = (width - TrailSenseSpacing.xxs * (columns - 1)) / columns
    val besideIcon = cell - TrailSenseSpacing.lg * 2 - 40.dp - TrailSenseSpacing.md
    val widest = remember(titles, style) {
        titles.maxOfOrNull { with(density) { measurer.measure(it, style, maxLines = 1).size.width.toDp() } }
    }
    if (widest == null || widest <= besideIcon) return style.fontSize
    return (style.fontSize.value * (besideIcon / widest))
        .coerceAtLeast(MaterialTheme.typography.labelLarge.fontSize.value).sp
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(tile: TrailSenseTile, shape: RoundedCornerShape, titleSize: TextUnit, modifier: Modifier) {
    val wells = trailSenseWellColors(tile.accent)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.clip(shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    role = Role.Button,
                    onClick = tile.onClick,
                    onLongClick = tile.onLongClick,
                    onLongClickLabel = tile.title
                )
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            TrailSenseIconWell(wells) { tile.icon() }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tile.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = titleSize),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                tile.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Material color harmonization: rotate [design]'s hue toward [source]'s by half the distance, at most 15°, keeping
 * saturation and value, so category accents stay distinct while sitting inside the user's palette.
 */
internal fun harmonize(design: Color, source: Color): Color {
    val (designHue, saturation, value) = design.toHsv()
    val sourceHue = source.toHsv().first
    val delta = ((sourceHue - designHue + 540f) % 360f) - 180f
    val rotation = minOf(kotlin.math.abs(delta) * 0.5f, 15f) * kotlin.math.sign(delta)
    return Color.hsv((designHue + rotation + 360f) % 360f, saturation, value, design.alpha)
}

private fun Color.toHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val chroma = max - min
    val hue = when {
        chroma == 0f -> 0f
        max == red -> 60f * (((green - blue) / chroma) % 6f)
        max == green -> 60f * (((blue - red) / chroma) + 2f)
        else -> 60f * (((red - green) / chroma) + 4f)
    }
    return Triple((hue + 360f) % 360f, if (max == 0f) 0f else chroma / max, max)
}
