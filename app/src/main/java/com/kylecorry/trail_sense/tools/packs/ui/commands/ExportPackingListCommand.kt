package com.kylecorry.trail_sense.tools.packs.ui.commands

import android.content.Context
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.LighterPackIOService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Exports a pack as a LighterPack CSV; [context] is the screen's (it shows the loading dialog). */
class ExportPackingListCommand(
    private val context: Context,
    resultRetriever: IntentResultRetriever,
    private val scope: CoroutineScope,
    private val effects: ScreenEffects
) : Command<Pack> {

    private val exportService = LighterPackIOService.create(context, resultRetriever)

    private val repo = PackRepo.getInstance(context)

    override fun execute(value: Pack) {
        scope.launch {
            var items: List<PackItem> = emptyList()
            Alerts.withLoading(context, context.getString(R.string.loading)) {
                items = repo.getItemsFromPackAsync(value.id)
            }
            val exported = exportService.export(items, "${value.name.slugify()}.csv")
            if (exported) {
                effects.showMessage(context.getString(R.string.exported))
            } else {
                effects.showMessage(context.getString(R.string.export_failed))
            }
        }
    }
}
