@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideNameDisplay
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FieldGuideSettingsToolScreen(host: TrailSenseDestinationHost) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPreferences(context) }
    val repo = remember { FieldGuideRepo.getInstance(context) }
    val formatter = remember { FieldGuideFormatService(prefs) }
    var nameDisplay by remember { mutableStateOf(prefs.fieldGuide.nameDisplay) }

    val nameOptions = listOf(
        FieldGuideNameDisplay.Common to stringResource(R.string.common_name),
        FieldGuideNameDisplay.Scientific to stringResource(R.string.scientific_name),
        FieldGuideNameDisplay.Both to stringResource(R.string.both_names)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = TrailSenseSize.maximumContentWidth),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                item("title") {
                    Text(
                        text = stringResource(R.string.field_guide),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                item("name_display") {
                    SettingsPickerRow(
                        label = stringResource(R.string.field_guide_name_display),
                        selected = nameOptions.first { it.first == nameDisplay }.second,
                        onClick = {
                            Pickers.item(
                                context,
                                context.getString(R.string.field_guide_name_display),
                                nameOptions.map { it.second },
                                nameOptions.indexOfFirst { it.first == nameDisplay }.coerceAtLeast(0)
                            ) { index ->
                                if (index != null) {
                                    val selected = nameOptions[index].first
                                    prefs.fieldGuide.nameDisplay = selected
                                    nameDisplay = selected
                                }
                            }
                        }
                    )
                }
                item("built_in_pages") {
                    SettingsActionRow(
                        label = stringResource(R.string.built_in_pages),
                        onClick = {
                            scope.launch {
                                val pages = withContext(Dispatchers.IO) {
                                    repo.getAllPages()
                                        .filter { it.isReadOnly }
                                        .sortedBy { formatter.formatName(it).lowercase() }
                                }
                                showBuiltInPagesPicker(context, formatter, pages) { changed ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            changed.forEach { (page, hidden) ->
                                                repo.setPageHidden(page.id, hidden)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun showBuiltInPagesPicker(
    context: Context,
    formatter: FieldGuideFormatService,
    pages: List<FieldGuidePage>,
    onChanged: (List<Pair<FieldGuidePage, Boolean>>) -> Unit
) {
    val selectedIndices = pages.mapIndexedNotNull { index, page ->
        if (!page.isHidden) index else null
    }
    Pickers.items(
        context,
        context.getString(R.string.built_in_pages),
        pages.map { formatter.formatName(it) },
        selectedIndices
    ) { selection ->
        if (selection == null) {
            return@items
        }
        val visiblePageIds = selection.map { pages[it].id }.toSet()
        val changed = pages.mapNotNull { page ->
            val shouldBeHidden = !visiblePageIds.contains(page.id)
            if (page.isHidden == shouldBeHidden) {
                null
            } else {
                page to shouldBeHidden
            }
        }
        if (changed.isNotEmpty()) {
            onChanged(changed)
        }
    }
}
