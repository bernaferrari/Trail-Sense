package com.kylecorry.trail_sense.shared.sharing

import android.content.DialogInterface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseBottomSheetComposeFragment

class ActionSheet(
    private val title: String,
    private val subtitle: String?,
    private val actions: List<ActionItem>,
    private val onAction: (action: ActionItem?, sheet: ActionSheet) -> Unit
) : TrailSenseBottomSheetComposeFragment() {

    @Composable
    override fun FragmentContent() {
        ActionSheetContent(
            title = title,
            subtitle = subtitle,
            actions = actions,
            onAction = { onAction(it, this@ActionSheet) },
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onAction(null, this)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionSheetContent(
    title: String,
    subtitle: String?,
    actions: List<ActionItem>,
    onAction: (ActionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center,
            maxItemsInEachRow = 4
        ) {
            actions.forEach { action ->
                ActionTile(
                    action = action,
                    onClick = { onAction(action) },
                    modifier = Modifier.padding(8.dp)
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
            .size(90.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(action.icon),
                contentDescription = action.name,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = action.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionSheetContentPreview() {
    TrailSenseTheme {
        ActionSheetContent(
            title = "Share",
            subtitle = "Choose a format",
            actions = listOf(
                ActionItem("Text", R.drawable.ic_altitude, {}),
                ActionItem("QR", R.drawable.ic_qr_code, {}),
                ActionItem("Map", R.drawable.ic_map_group, {})
            ),
            onAction = {}
        )
    }
}
