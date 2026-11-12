package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DatePickerContent(
    date: LocalDate,
    formatter: FormatService,
    searchEnabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickDate: () -> Unit,
    onCalendarLongPress: () -> Unit,
    onSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(48.dp)
                .testTag("prev_date")
        ) {
            Icon(
                imageVector = TrailIcons.KeyboardArrowRight,
                contentDescription = appString(Res.string.previous),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(180f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = formatter.formatRelativeDate(date),
            modifier = Modifier.testTag("date"),
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .padding(start = TrailSenseSpacing.lg)
                .size(48.dp)
                .combinedClickable(
                    onClick = onPickDate,
                    onLongClick = onCalendarLongPress
                )
                .testTag("date_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = TrailIcons.Date,
                contentDescription = appString(Res.string.date),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (searchEnabled) {
            IconButton(
                onClick = onSearch,
                modifier = Modifier
                    .padding(start = TrailSenseSpacing.lg)
                    .size(48.dp)
                    .testTag("search_btn")
            ) {
                Icon(
                    imageVector = TrailIcons.Search,
                    contentDescription = appString(Res.string.search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(48.dp)
                .testTag("next_date")
        ) {
            Icon(
                imageVector = TrailIcons.KeyboardArrowRight,
                contentDescription = appString(Res.string.next),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}
