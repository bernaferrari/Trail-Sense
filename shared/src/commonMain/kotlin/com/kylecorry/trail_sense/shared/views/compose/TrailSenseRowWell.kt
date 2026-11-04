@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * The family's leading well for a row: a 40 dp Cookie6 in 12% primary with a primary glyph. Every list and setting
 * row uses this (the same well [TrailSenseListItem] draws); tappable when given [onClick]. User data that has its
 * own mark (a beacon's colour and shape, a path's colour) keeps that mark instead.
 */
@Composable
fun TrailSenseRowWell(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = MaterialShapes.Cookie6Sided.toShape()
    val inner: @Composable () -> Unit = { Box(contentAlignment = Alignment.Center) { content() } }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(40.dp),
            shape = shape,
            color = primary.copy(alpha = 0.12f),
            contentColor = primary,
            content = inner
        )
    } else {
        Surface(
            modifier = modifier.size(40.dp),
            shape = shape,
            color = primary.copy(alpha = 0.12f),
            contentColor = primary,
            content = inner
        )
    }
}

/** [TrailSenseRowWell] holding one 22 dp icon, optionally turned by [rotation] degrees (a direction). */
@Composable
fun TrailSenseRowWell(
    icon: Painter,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    onClick: (() -> Unit)? = null
) {
    TrailSenseRowWell(modifier = modifier, onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp).rotate(rotation))
    }
}
