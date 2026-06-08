package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class PhotoMapIconButton(
    context: Context,
    icon: Int
) : AbstractComposeView(context) {

    private var iconRes by mutableIntStateOf(icon)
    private var isOn by mutableStateOf(false)

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    fun setState(isOn: Boolean) {
        this.isOn = isOn
    }

    @Composable
    override fun Content() {
        val background = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
        val foreground = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        TrailSenseTheme {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = background,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
