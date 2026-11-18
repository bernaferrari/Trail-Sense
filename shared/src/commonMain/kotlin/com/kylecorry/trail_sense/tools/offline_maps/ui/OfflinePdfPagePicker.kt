package com.kylecorry.trail_sense.tools.offline_maps.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.import_btn
import com.kylecorry.trail_sense.shared.generated.resources.offline_map_pdf_page_number
import com.kylecorry.trail_sense.shared.generated.resources.offline_map_pdf_page_title
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost

/** Prompts only when a PDF has a choice of pages. Returns a zero-based page index. */
suspend fun chooseOfflinePdfPage(host: AppHost, pageCount: Int): Int? {
    if (pageCount <= 0) return null
    if (pageCount == 1) return 0
    val strings = getAppService<AppStrings>()
    while (true) {
        val page = CoroutinePickers.number(
            host,
            title = strings.get(Res.string.offline_map_pdf_page_title),
            default = 1,
            allowDecimals = false,
            hint = strings.get(Res.string.offline_map_pdf_page_number, pageCount),
            okText = strings.get(Res.string.import_btn)
        )?.toInt() ?: return null
        if (page in 1..pageCount) return page - 1
    }
}
