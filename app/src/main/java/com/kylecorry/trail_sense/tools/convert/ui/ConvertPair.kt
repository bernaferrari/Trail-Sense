@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.convert.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.Copy
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import kotlinx.coroutines.launch

/**
 * A converter as one group of two rows: what you type, then the answer as the screen's one emphasised value, each
 * with its unit as a chip. A tonal badge sits on the seam: it swaps the units where that makes sense, or simply
 * points down. The answer rolls in when it changes and can be copied with one tap. Idle, the answer row shows a
 * plain instruction, and a row of quick starts (paste, or a round amount) sits under the pair.
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
    @Suppress("UNUSED_PARAMETER") emptyValue: String = "0",
    keyboardType: KeyboardType = KeyboardType.Decimal,
    isError: Boolean = false,
    errorText: String? = null,
    onSwap: (() -> Unit)? = null,
    swapLabel: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    var turns by remember { mutableFloatStateOf(0f) }
    val rotation by animateFloatAsState(turns * 180f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "swap")
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val copyPop = remember { Animatable(1f) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)) {
        Box(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                // From
                Surface(
                    shape = trailSenseGroupShape(TrailSenseGroupPosition(isFirst = true, isLast = false)),
                    color = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurface
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 128.dp)
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
                                    color = scheme.onSurfaceVariant
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
                    shape = trailSenseGroupShape(TrailSenseGroupPosition(isFirst = false, isLast = true)),
                    color = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurface
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 136.dp)
                            .padding(TrailSenseSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                    ) {
                        TileHeader(toLabel, toUnit, onToUnitClick, scheme.onSurfaceVariant, scheme.secondaryContainer, scheme.onSecondaryContainer)
                        val enter = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
                        val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                        AnimatedContent(
                            targetState = result,
                            transitionSpec = {
                                // Digits roll: the new answer slides up into place as the old one leaves.
                                (slideInVertically(enter) { it / 2 } + fadeIn(fade)) togetherWith
                                    (slideOutVertically(enter) { -it / 2 } + fadeOut(fade))
                            },
                            label = "result"
                        ) { text ->
                            if (text.isBlank()) {
                                Text(
                                    placeholder,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = scheme.onSurfaceVariant
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text,
                                        style = (if (text.length > 18) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall)
                                            .copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = TrailSenseSpacing.lg)
                                    )
                                    Surface(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(text))
                                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                            scope.launch {
                                                copyPop.snapTo(0.8f)
                                                copyPop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .graphicsLayer {
                                                scaleX = copyPop.value
                                                scaleY = copyPop.value
                                            },
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        contentColor = scheme.onSurfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                TrailIcons.Copy,
                                                contentDescription = stringResource(R.string.convert_copy),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // The seam badge: swap where it makes sense, otherwise a quiet arrow from the question to the answer.
            val badgeModifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = TrailSenseSpacing.xl)
                .size(52.dp)
            if (onSwap != null) {
                Surface(
                    onClick = {
                        turns += 1f
                        onSwap()
                    },
                    modifier = badgeModifier,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SwapVert,
                            contentDescription = swapLabel,
                            modifier = Modifier
                                .size(26.dp)
                                .rotate(rotation)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = badgeModifier,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    color = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Quick starts: paste what's on the clipboard, or try a round amount.
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            QuickChip(
                label = stringResource(R.string.convert_paste),
                icon = true,
                onClick = { clipboard.getText()?.text?.trim()?.let { onValueChange(it) } }
            )
            if (keyboardType != KeyboardType.Text) {
                listOf("1", "10", "100").forEach { amount ->
                    QuickChip(label = amount, icon = false, onClick = { onValueChange(amount) })
                }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, icon: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(TrailSenseSpacing.xs))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
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
