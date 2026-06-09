package com.kylecorry.trail_sense.shared.io

import android.content.Context
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.gpx.GPXData

class IOFactory {

    fun createGpxService(
        context: Context,
        resultRetriever: IntentResultRetriever
    ): IOService<GPXData> {
        return GpxIOService(
            IntentUriPicker(resultRetriever, context),
            ExternalUriService(context)
        )
    }

    fun createCsvService(
        context: Context,
        resultRetriever: IntentResultRetriever
    ): IOService<List<List<String>>> {
        return CsvIOService(
            IntentUriPicker(resultRetriever, context),
            ExternalUriService(context)
        )
    }

}
