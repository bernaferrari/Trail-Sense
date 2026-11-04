package com.kylecorry.trail_sense.main.errors

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.dialogs.showComposeDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/**
 * After a crash: one calm sentence, one main action. An apology and what sending does (it opens the email app, and
 * nothing leaves the phone until the person sends it); the technical log stays folded under "Details" with a copy
 * button, never dumped into the message.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
internal fun showCrashReportDialog(
    context: Context,
    log: String,
    onCopy: () -> Unit,
    onSend: () -> Unit
) {
    showComposeDialog(context) { dismiss ->
        var expanded by remember { mutableStateOf(false) }
        val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
        val scheme = MaterialTheme.colorScheme
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = scheme.errorContainer,
                contentColor = scheme.onErrorContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.SentimentDissatisfied, contentDescription = null, modifier = Modifier.size(32.dp))
                }
            }
            Text(
                stringResource(R.string.crash_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = TrailSenseSpacing.lg)
            )
            Text(
                stringResource(R.string.crash_message),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = TrailSenseSpacing.sm)
            )
            Row(
                modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(stringResource(R.string.crash_details))
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.rotate(chevron))
                }
                AnimatedVisibility(expanded) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(android.R.string.copy), tint = scheme.primary)
                    }
                }
            }
            AnimatedVisibility(expanded) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.surfaceContainerHighest)
                ) {
                    Text(
                        log.lineSequence().take(60).joinToString("\n"),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                        color = scheme.onSurfaceVariant,
                        softWrap = false,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(TrailSenseSpacing.md)
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm, Alignment.End)
        ) {
            TextButton(onClick = dismiss) { Text(stringResource(R.string.crash_not_now)) }
            Button(onClick = {
                dismiss()
                onSend()
            }) { Text(stringResource(R.string.crash_send_report)) }
        }
    }
}
