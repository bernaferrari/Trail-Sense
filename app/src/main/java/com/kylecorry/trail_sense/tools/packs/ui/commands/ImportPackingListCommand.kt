package com.kylecorry.trail_sense.tools.packs.ui.commands

import android.content.Context
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.LighterPackIOService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Imports a LighterPack CSV as a new pack and opens it; [context] is the screen's (it shows dialogs). */
class ImportPackingListCommand(
    private val context: Context,
    resultRetriever: IntentResultRetriever,
    private val scope: CoroutineScope,
    private val effects: ScreenEffects,
    private val navigator: TrailSenseNavigator
) : Command {

    private val importService = LighterPackIOService.create(context, resultRetriever)
    private val repo = PackRepo.getInstance(context)

    override fun execute() {
        scope.launch {
            var items: List<PackItem>? = null
            Alerts.withLoading(context, context.getString(R.string.loading)) {
                items = importService.import()
            }

            // If items are null or empty, show a toast and return
            if (items.isNullOrEmpty()) {
                effects.showMessage(context.getString(R.string.no_items_found))
                return@launch
            }

            // Ask the user for the pack name
            // TODO: Default to the file name
            val name = CoroutinePickers.text(
                context,
                context.getString(R.string.name)
            ) ?: return@launch

            // Create the pack
            var packId = 0L
            Alerts.withLoading(context, context.getString(R.string.loading)) {
                packId = repo.addPack(Pack(0, name))
                for (item in items) {
                    val newItem = item.copy(packId = packId)
                    repo.addItem(newItem)
                }
            }

            // Open the pack
            navigator.navigate(Route.PackItemList(packId))
        }
    }
}
