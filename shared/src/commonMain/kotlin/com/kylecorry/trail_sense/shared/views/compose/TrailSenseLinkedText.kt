package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

/** Text whose links (from rendered markdown) take the theme's primary color and open when tapped. */
@Composable
fun TrailSenseLinkedText(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textAlign: TextAlign? = null
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val displayStyle = if (textAlign == null) {
        style.copy(color = textColor)
    } else {
        style.copy(color = textColor, textAlign = textAlign)
    }
    val annotated = remember(text, linkColor) { text.toAnnotatedString(linkColor) }
    Text(text = annotated, modifier = modifier, style = displayStyle)
}
