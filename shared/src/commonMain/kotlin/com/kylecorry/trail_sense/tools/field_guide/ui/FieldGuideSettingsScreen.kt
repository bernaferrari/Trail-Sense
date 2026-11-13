@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.tools.field_guide.infrastructure.IFieldGuideRepo

import kotlinx.coroutines.IO
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideNameDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FieldGuideSettingsToolScreen() {
    val dispatcher = rememberSystemBack()
    val host = AppHost.current()
    val scope = rememberCoroutineScope()
    val prefs = remember { getAppService<UserPreferences>() }
    val repo = remember { getAppService<IFieldGuideRepo>() }
    val formatter = remember { FieldGuideFormatService(prefs) }
    var nameDisplay by remember { mutableStateOf(prefs.fieldGuide.nameDisplay) }

    val nameTitle = appString(Res.string.field_guide_name_display)
    val nameOptions = listOf(
        TrailSenseChoice(FieldGuideNameDisplay.Common, appString(Res.string.field_guide_name_common)),
        TrailSenseChoice(FieldGuideNameDisplay.Scientific, appString(Res.string.field_guide_name_scientific)),
        TrailSenseChoice(FieldGuideNameDisplay.Both, appString(Res.string.field_guide_name_both))
    )

    TrailSensePage(
        title = appString(Res.string.field_guide),
        onBack = { dispatcher?.invoke() },
        backLabel = appString(Res.string.back)
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
                    label = appString(Res.string.built_in_pages),
                    onClick = {
                        scope.launch {
                            val pages = withContext(Dispatchers.IO) {
                                repo.getAllPages()
                                    .filter { it.isReadOnly }
                                    .sortedBy { formatter.formatName(it).lowercase() }
                            }
                            showBuiltInPagesPicker(host, formatter, pages) { changed ->
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
    host: AppHost,
    formatter: FieldGuideFormatService,
    pages: List<FieldGuidePage>,
    onChanged: (List<Pair<FieldGuidePage, Boolean>>) -> Unit
) {
    val selectedIndices = pages.mapIndexedNotNull { index, page ->
        if (!page.isHidden) index else null
    }
    Pickers.items(
        host,
        getAppService<AppStrings>().get(Res.string.built_in_pages),
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
