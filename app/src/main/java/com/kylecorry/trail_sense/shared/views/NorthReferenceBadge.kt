package com.kylecorry.trail_sense.shared.views

import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class NorthReferenceBadgeState(
    private val context: Context
) {

    private val formatter = FormatService.getInstance(context)

    var isVisible by mutableStateOf(true)

    var useTrueNorth by mutableStateOf(false)

    var showLabel by mutableStateOf(true)

    var showDetailsOnClick by mutableStateOf(false)

    var onSettingsClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    fun Content() {
        if (!isVisible) {
            return
        }
        val text = when {
            !showLabel -> null
            useTrueNorth -> context.getString(R.string.true_north)
            else -> context.getString(R.string.magnetic_north)
        }

        // A tonal pill (the app's own theme, not a fixed one), so the reference reads as a chip, not a stray glyph.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .animateContentSize()
                .heightIn(min = 32.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = showDetailsOnClick,
                    onClick = { onSettingsClick?.invoke() ?: showDescription() }
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (text != null) 12.dp else 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = formatter.getCompassReferenceIcon(useTrueNorth),
                    contentDescription = text,
                    modifier = Modifier.size(16.dp)
                )
                if (text != null) {
                    Text(text = text, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    private fun showDescription() {
        val title = if (useTrueNorth) {
            context.getString(R.string.true_north)
        } else {
            context.getString(R.string.magnetic_north)
        }

        val message = if (useTrueNorth) {
            context.getString(R.string.true_north_description)
        } else {
            context.getString(R.string.magnetic_north_description)
        }

        val settingsClick = onSettingsClick
        Alerts.dialog(
            context,
            title,
            message,
            okText = if (settingsClick != null) context.getString(R.string.settings) else context.getString(
                android.R.string.ok
            ),
            cancelText = if (settingsClick != null) context.getString(android.R.string.cancel) else null,
        ) { cancelled ->
            if (!cancelled) {
                settingsClick?.invoke()
            }
        }
    }

}
