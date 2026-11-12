package com.kylecorry.trail_sense.tools.astronomy.ui.format

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.partial
import com.kylecorry.trail_sense.shared.generated.resources.total
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.astronomy.domain.Eclipse

object EclipseFormatter {

    fun type(eclipse: Eclipse): String {
        val formatService = FormatService.getInstance()
        val strings = getAppService<AppStrings>()
        return if (eclipse.isTotal) strings.get(Res.string.total) else strings.get(Res.string.partial,
            formatService.formatPercentage(eclipse.obscuration * 100)
        )
    }

}
