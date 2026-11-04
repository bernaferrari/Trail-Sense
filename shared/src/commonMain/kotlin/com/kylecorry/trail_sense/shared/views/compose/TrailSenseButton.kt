package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Which Material button: the one main action, a secondary one, a quiet or destructive one, or a link-like one. */
enum class TrailSenseButtonStyle { Filled, Tonal, Outlined, Text }

/**
 * The app's one button. A plain Material 3 button with Material's own padding and type, at least 56 dp tall
 * (it grows with large text; it never clips), an optional 20 dp icon before the label. Every screen uses this
 * instead of hand-tuned heights, paddings or text styles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrailSenseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    style: TrailSenseButtonStyle = TrailSenseButtonStyle.Filled
) {
    val sized = modifier.heightIn(min = 56.dp)
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, maxLines = 1)
    }
    when (style) {
        TrailSenseButtonStyle.Filled -> Button(onClick = onClick, modifier = sized, enabled = enabled, shapes = ButtonDefaults.shapes(), content = content)
        TrailSenseButtonStyle.Tonal -> FilledTonalButton(onClick = onClick, modifier = sized, enabled = enabled, shapes = ButtonDefaults.shapes(), content = content)
        TrailSenseButtonStyle.Outlined -> OutlinedButton(onClick = onClick, modifier = sized, enabled = enabled, shapes = ButtonDefaults.shapes(), content = content)
        TrailSenseButtonStyle.Text -> TextButton(onClick = onClick, modifier = sized, enabled = enabled, shapes = ButtonDefaults.shapes(), content = content)
    }
}
