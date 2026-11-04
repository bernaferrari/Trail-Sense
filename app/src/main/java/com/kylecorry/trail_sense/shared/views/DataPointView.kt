package com.kylecorry.trail_sense.shared.views

import androidx.compose.ui.graphics.vector.ImageVector
import android.view.Gravity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun DataPointContent(
    title: String,
    description: String,
    icon: ImageVector?,
    tint: Color,
    showDescription: Boolean,
    gravity: Int,
    onDescriptionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .wrapContentSize(gravity.toAlignment())
            .testTag("data_point"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                tint = tint.copy(alpha = 0.75f),
                modifier = Modifier.testTag("data_point_image")
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("data_point_title")
            )
            if (showDescription) {
                val descriptionModifier = if (onDescriptionClick != null) {
                    Modifier.clickable { onDescriptionClick() }
                } else {
                    Modifier
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = descriptionModifier.testTag("data_point_desc")
                )
            }
        }
    }
}

private fun Int.toAlignment(): Alignment {
    return when {
        this and Gravity.CENTER_HORIZONTAL == Gravity.CENTER_HORIZONTAL -> Alignment.Center
        this and Gravity.RIGHT == Gravity.RIGHT || this and Gravity.END == Gravity.END -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
}
