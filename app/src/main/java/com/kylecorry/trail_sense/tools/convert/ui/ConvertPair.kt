@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * A converter as two equal, connected tiles: what you type on the quiet top tile, the answer on the saturated bottom
 * one, each with its unit as a chip. A cookie-shaped swap button sits on the seam between them and spins when used.
 * Before anything is typed the answer tile shows a muted hint instead of a lone dash.
 */
@Composable
internal fun ConvertPair(
    value: String,
    onValueChange: (String) -> Unit,
    fromLabel: String,
    fromUnit: String?,
    onFromUnitClick: (() -> Unit)?,
    result: String,
    toLabel: String,
    toUnit: String,
    onToUnitClick: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    emptyValue: String = "0",
    keyboardType: KeyboardType = KeyboardType.Decimal,
    isError: Boolean = false,
    errorText: String? = null,
    onSwap: (() -> Unit)? = null,
    swapLabel: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 180f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "swap")

    // One hero: the amount you type on a light inset at the top, the answer large on the primary card under it.
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            // From
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = scheme.surfaceContainerLowest,
                contentColor = scheme.onSurface
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 132.dp)
                        .padding(TrailSenseSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                ) {
                    TileHeader(fromLabel, fromUnit, onFromUnitClick, scheme.onSurfaceVariant, scheme.secondaryContainer, scheme.onSecondaryContainer)
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isError,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.SemiBold
                        ),
                        placeholder = {
                            Text(
                                if (keyboardType == KeyboardType.Text) "47.6062, -122.3321" else "0",
                                style = MaterialTheme.typography.headlineMedium,
                                color = scheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        supportingText = if (isError && errorText != null) {
                            { Text(errorText) }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            // To
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                contentColor = scheme.onPrimaryContainer
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 132.dp)
                        .padding(TrailSenseSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                ) {
                    TileHeader(toLabel, toUnit, onToUnitClick, scheme.onPrimaryContainer.copy(alpha = 0.8f), scheme.primary, scheme.onPrimary)
                    val fadeInSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                    val fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                    AnimatedContent(
                        targetState = result.isBlank(),
                        transitionSpec = { fadeIn(fadeInSpec) togetherWith fadeOut(fadeOutSpec) },
                        label = "result"
                    ) { blank ->
                        val text = result
                        if (blank) {
                            // A waiting value at full size, so the answer tile reads as ready rather than empty.
                            Column(Modifier.padding(horizontal = TrailSenseSpacing.lg)) {
                                Text(
                                    emptyValue,
                                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onPrimaryContainer.copy(alpha = 0.38f)
                                )
                                Text(
                                    placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            Text(
                                text,
                                style = (if (text.length > 18) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall)
                                    .copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                            )
                        }
                    }
                }
            }
        }
        if (onSwap != null) {
            Surface(
                onClick = {
                    turns += 1f
                    onSwap()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = TrailSenseSpacing.xl)
                    .size(56.dp),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.SwapVert,
                        contentDescription = swapLabel,
                        modifier = Modifier
                            .size(28.dp)
                            .rotate(rotation)
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun TileHeader(
    label: String,
    unit: String?,
    onUnitClick: (() -> Unit)?,
    labelColor: Color,
    chipColor: Color,
    onChipColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = TrailSenseSpacing.lg)
        )
        if (unit != null) {
            if (onUnitClick != null) {
                FilledTonalButton(
                    onClick = onUnitClick,
                    modifier = Modifier.heightIn(min = 40.dp),
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = chipColor, contentColor = onChipColor),
                    contentPadding = PaddingValues(start = TrailSenseSpacing.md, end = TrailSenseSpacing.sm)
                ) {
                    Text(unit, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            } else {
                Text(unit, style = MaterialTheme.typography.labelLarge, color = labelColor)
            }
        }
    }
}
