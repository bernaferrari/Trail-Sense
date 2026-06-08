package com.kylecorry.trail_sense.shared.io

import android.content.Context
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.fragments.AndromedaActivity
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

    fun createCsvService(activity: AndromedaActivity): IOService<List<List<String>>> {
        return CsvIOService(
            IntentUriPicker(activity, activity),
            ExternalUriService(activity)
        )
    }

}
