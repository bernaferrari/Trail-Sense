package com.kylecorry.trail_sense.shared.views

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.FormatService

class NorthReferenceBadgeState() {

    private val formatter = FormatService.getInstance()

    var isVisible by mutableStateOf(true)

    var useTrueNorth by mutableStateOf(false)

    var showLabel by mutableStateOf(true)

    var showDetailsOnClick by mutableStateOf(false)

    var onSettingsClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) {
            return
        }
        val text = when {
            !showLabel -> null
            useTrueNorth -> getAppService<AppStrings>().get(Res.string.true_north)
            else -> getAppService<AppStrings>().get(Res.string.magnetic_north)
        }

        // A tonal pill (the app's own theme, not a fixed one), so the reference reads as a chip, not a stray glyph.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .testTag("north_reference_indicator")
                .animateContentSize()
                .heightIn(min = 32.dp)
                .clip(CircleShape)
                .clickable(
                    enabled = showDetailsOnClick,
                    // As upstream: the tap explains the reference; its Settings button opens the compass settings
                    onClick = { showDescription() }
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
            getAppService<AppStrings>().get(Res.string.true_north)
        } else {
            getAppService<AppStrings>().get(Res.string.magnetic_north)
        }

        val message = if (useTrueNorth) {
            getAppService<AppStrings>().get(Res.string.true_north_description)
        } else {
            getAppService<AppStrings>().get(Res.string.magnetic_north_description)
        }

        val settingsClick = onSettingsClick
        Alerts.dialog(AppHost.current(),
            title,
            message,
            okText = if (settingsClick != null) getAppService<AppStrings>().get(Res.string.settings) else getAppService<AppStrings>().system(SystemString.Ok),
            cancelText = if (settingsClick != null) getAppService<AppStrings>().system(SystemString.Cancel) else null,
        ) { cancelled ->
            if (!cancelled) {
                settingsClick?.invoke()
            }
        }
    }

}
