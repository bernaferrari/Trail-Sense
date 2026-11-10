package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.commands

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.text.slugify
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap

class ExportVectorMapCommand(
    private val exporter: ExportService<VectorMap>,
    private val loading: ILoadingIndicator
) {

    suspend fun execute(map: VectorMap): Boolean = onIO {
        onMain { loading.show() }
        try {
            exporter.export(map, "${map.name.slugify()}.map")
        } finally {
            onMain { loading.hide() }
        }
    }
}
