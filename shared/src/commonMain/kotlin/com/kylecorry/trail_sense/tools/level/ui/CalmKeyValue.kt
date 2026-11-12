package com.kylecorry.trail_sense.tools.level.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * The key value of a calm screen: emphasised type at the top of the page (displaySmall SemiBold in onSurface) with
 * one verdict line under it. No card, no badge, no eyebrow.
 */
@Composable
fun CalmKeyValue(
    value: String,
    modifier: Modifier = Modifier,
    verdict: String? = null,
    valueTag: String? = null,
    verdictTag: String? = null,
    verdictColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        Text(
            text = value,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier,
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!verdict.isNullOrEmpty()) {
            Text(
                text = verdict,
                modifier = if (verdictTag != null) Modifier.testTag(verdictTag) else Modifier,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = verdictColor
            )
        }
        content?.invoke()
    }
}
