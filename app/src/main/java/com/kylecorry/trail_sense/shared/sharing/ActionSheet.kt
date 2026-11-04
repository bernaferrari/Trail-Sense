package com.kylecorry.trail_sense.shared.sharing

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.compose.foundation.layout.fillMaxWidth

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionSheetContent(
    subtitle: String?,
    actions: List<ActionItem>,
    onAction: (ActionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // The sheet supplies the title and the padding
    Column(modifier = modifier) {
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.sm),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center,
            maxItemsInEachRow = 4
        ) {
            actions.forEach { action ->
                ActionTile(
                    action = action,
                    onClick = { onAction(action) },
                    modifier = Modifier.padding(TrailSenseSpacing.sm)
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    action: ActionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(96.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = rememberVectorPainter(action.icon),
                contentDescription = action.name,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = action.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = TrailSenseSpacing.sm)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionSheetContentPreview() {
    TrailSenseTheme {
        ActionSheetContent(
            subtitle = "Choose a format",
            actions = listOf(
                ActionItem("Text", TrailIcons.Altitude, {}),
                ActionItem("QR", TrailIcons.QrCode, {}),
                ActionItem("Map", TrailIcons.MapGroup, {})
            ),
            onAction = {}
        )
    }
}
