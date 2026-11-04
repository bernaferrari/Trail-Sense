@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.goBack
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
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

    val nameTitle = stringResource(R.string.field_guide_name_display)
    val nameOptions = listOf(
        TrailSenseChoice(FieldGuideNameDisplay.Common, stringResource(R.string.field_guide_name_common)),
        TrailSenseChoice(FieldGuideNameDisplay.Scientific, stringResource(R.string.field_guide_name_scientific)),
        TrailSenseChoice(FieldGuideNameDisplay.Both, stringResource(R.string.field_guide_name_both))
    )

    TrailSensePage(
        title = stringResource(R.string.field_guide),
        onBack = host::goBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("name_display", title = nameTitle) {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    TrailSenseConnectedChoice(
                        options = nameOptions,
                        selected = nameDisplay,
                        label = nameTitle,
                        onSelect = {
                            prefs.fieldGuide.nameDisplay = it
                            nameDisplay = it
                        }
                    )
                }
            }
        }
        trailSenseSection("pages") {
            row { position ->
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
                    },
                    position = position
                )
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
