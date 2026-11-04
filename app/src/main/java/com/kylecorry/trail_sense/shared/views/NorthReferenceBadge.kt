package com.kylecorry.trail_sense.shared.views

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

        TrailSenseTheme {
            Row(
                modifier = Modifier.clickable(
                    enabled = showDetailsOnClick,
                    onClick = { onSettingsClick?.invoke() ?: showDescription() }
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(formatter.getCompassReferenceIcon(useTrueNorth)),
                    contentDescription = text,
                    tint = Color(Resources.androidTextColorSecondary(context)),
                    modifier = Modifier.size(16.dp)
                )
                if (text != null) {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
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
