package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
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
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

class NorthReferenceBadge(
    context: Context,
    attrs: AttributeSet? = null
) : AbstractComposeView(context, attrs) {

    private val formatter = FormatService.getInstance(context)

    var useTrueNorth by mutableStateOf(false)

    var showLabel by mutableStateOf(true)

    var showDetailsOnClick by mutableStateOf(false)

    @Composable
    override fun Content() {
        val text = when {
            !showLabel -> null
            useTrueNorth -> context.getString(R.string.true_north)
            else -> context.getString(R.string.magnetic_north)
        }

        TrailSenseTheme {
            Row(
                modifier = Modifier.clickable(
                    enabled = showDetailsOnClick,
                    onClick = ::showDescription
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(formatter.getCompassReferenceIcon(useTrueNorth)),
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

        val controller = tryOrDefault(null) { findNavController() }
        Alerts.dialog(
            context,
            title,
            message,
            okText = if (controller != null) context.getString(R.string.settings) else context.getString(
                android.R.string.ok
            ),
            cancelText = if (controller != null) context.getString(android.R.string.cancel) else null,
        ) { cancelled ->
            if (!cancelled) {
                controller?.navigate(R.id.calibrateCompassFragment)
            }
        }
    }

}
