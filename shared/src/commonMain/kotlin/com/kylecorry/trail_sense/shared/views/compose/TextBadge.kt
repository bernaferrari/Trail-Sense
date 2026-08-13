package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.Role

/**
 * Reusable pretty badge component — first shared UI artifact in :shared.
 */
@Composable
fun TextBadge(
    text: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = shape,
        modifier = if (onClick == null) {
            modifier
        } else {
            modifier
                .heightIn(min = TrailSenseSize.minimumTouchTarget)
                .clickable(role = Role.Button) { onClick() }
        }
    ) {
        Text(
            text = text,
            style = textStyle,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.xs)
        )
    }
}
