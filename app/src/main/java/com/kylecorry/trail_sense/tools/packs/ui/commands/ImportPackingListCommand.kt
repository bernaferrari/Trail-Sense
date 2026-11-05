package com.kylecorry.trail_sense.tools.packs.ui.commands

import com.kylecorry.trail_sense.shared.navigation.Route

import android.os.Bundle
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.LighterPackIOService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo

class ImportPackingListCommand(private val host: TrailSenseDestinationHost) : Command {

    private val importService = LighterPackIOService.create(
        host.requireContext(),
        host.intentResultRetriever
    )
    private val repo = PackRepo.getInstance(host.requireContext())

    override fun execute() {
        host.inBackground(BackgroundMinimumState.Created) {
            var items: List<PackItem>? = null
            Alerts.withLoading(host.requireContext(), host.getString(R.string.loading)) {
                items = importService.import()
            }

            // If items are null or empty, show a toast and return
            if (items.isNullOrEmpty()) {
                host.toast(host.getString(R.string.no_items_found))
                return@inBackground
            }

            // Ask the user for the pack name
            // TODO: Default to the file name
            val name = CoroutinePickers.text(
                host.requireContext(),
                host.getString(R.string.name)
            ) ?: return@inBackground

            // Create the pack
            var packId = 0L
            Alerts.withLoading(host.requireContext(), host.getString(R.string.loading)) {
                packId = repo.addPack(Pack(0, name))
                for (item in items) {
                    val newItem = item.copy(packId = packId)
                    repo.addItem(newItem)
                }
            }

            // Open the pack
            host.findNavigator().navigate(Route.PackItemList(packId))
        }
    }
}
