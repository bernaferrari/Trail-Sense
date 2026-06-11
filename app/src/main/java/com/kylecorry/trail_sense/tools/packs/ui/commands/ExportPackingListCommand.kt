package com.kylecorry.trail_sense.tools.packs.ui.commands

import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.commands.generic.Command
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.LighterPackIOService
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo

class ExportPackingListCommand(private val host: TrailSenseDestinationHost) : Command<Pack> {

    private val exportService = LighterPackIOService.create(
        host.requireContext(),
        host.intentResultRetriever
    )

    private val repo = PackRepo.getInstance(host.requireContext())

    override fun execute(value: Pack) {
        host.inBackground(BackgroundMinimumState.Created) {
            var items: List<PackItem> = emptyList()
            Alerts.withLoading(host.requireContext(), host.getString(R.string.loading)) {
                items = repo.getItemsFromPackAsync(value.id)
            }
            exportService.export(items, "${value.name.slugify()}.csv")
            host.toast(host.getString(R.string.exported))
        }
    }
}
