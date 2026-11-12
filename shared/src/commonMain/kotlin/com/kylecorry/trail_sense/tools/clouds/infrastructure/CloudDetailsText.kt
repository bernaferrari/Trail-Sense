package com.kylecorry.trail_sense.tools.clouds.infrastructure

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.images.AppImages
import com.kylecorry.trail_sense.shared.text.AppStrings

/** A cloud genus's name, description, forecast and picture. */
class CloudDetailsText(private val strings: AppStrings = getAppService()) {

    fun getCloudDescription(type: CloudGenus?): String {
        return when (type) {
            CloudGenus.Cirrocumulus -> strings.get(Res.string.cirrocumulus_desc)
            CloudGenus.Cirrostratus -> strings.get(Res.string.cirrostratus_desc)
            CloudGenus.Altocumulus -> strings.get(Res.string.altocumulus_desc)
            CloudGenus.Altostratus -> strings.get(Res.string.altostratus_desc)
            CloudGenus.Nimbostratus -> strings.get(Res.string.nimbostratus_desc)
            CloudGenus.Stratus -> strings.get(Res.string.stratus_desc)
            CloudGenus.Stratocumulus -> strings.get(Res.string.stratocumulus_desc)
            CloudGenus.Cumulus -> strings.get(Res.string.cumulus_desc)
            CloudGenus.Cumulonimbus -> strings.get(Res.string.cumulonimbus_desc)
            CloudGenus.Cirrus -> strings.get(Res.string.cirrus_desc)
            null -> strings.get(Res.string.no_clouds)
        }
    }

    fun getCloudForecast(type: CloudGenus?): String {
        return when (type) {
            CloudGenus.Cirrocumulus -> strings.get(Res.string.cloud_precipitation_forecast_hour_range,
                8,
                12
            )

            CloudGenus.Cirrostratus -> strings.get(Res.string.cloud_precipitation_forecast_hour_range,
                10,
                15
            )

            CloudGenus.Altocumulus -> strings.get(Res.string.altocumulus_forecast, 12)
            CloudGenus.Altostratus -> strings.get(Res.string.altostratus_forecast, 8)
            CloudGenus.Nimbostratus -> strings.get(Res.string.nimbostratus_forecast, 4)
            CloudGenus.Stratus -> strings.get(Res.string.cloud_stratus_forecast, 3)
            CloudGenus.Stratocumulus -> strings.get(Res.string.cloud_fair_forecast_hours, 3)
            CloudGenus.Cumulus -> strings.get(Res.string.cumulus_forecast, 3)
            CloudGenus.Cumulonimbus -> strings.get(Res.string.cumulonimbus_forecast)
            CloudGenus.Cirrus -> strings.get(Res.string.cloud_precipitation_forecast_hour_range,
                12,
                24
            )

            null -> "-"
        }
    }

    fun getCloudName(type: CloudGenus?): String {
        return when (type) {
            CloudGenus.Cirrus -> strings.get(Res.string.cirrus)
            CloudGenus.Cirrocumulus -> strings.get(Res.string.cirrocumulus)
            CloudGenus.Cirrostratus -> strings.get(Res.string.cirrostratus)
            CloudGenus.Altocumulus -> strings.get(Res.string.altocumulus)
            CloudGenus.Altostratus -> strings.get(Res.string.altostratus)
            CloudGenus.Nimbostratus -> strings.get(Res.string.nimbostratus)
            CloudGenus.Stratus -> strings.get(Res.string.stratus)
            CloudGenus.Stratocumulus -> strings.get(Res.string.stratocumulus)
            CloudGenus.Cumulus -> strings.get(Res.string.cumulus)
            CloudGenus.Cumulonimbus -> strings.get(Res.string.cumulonimbus)
            null -> strings.get(Res.string.clouds_clear)
        }
    }

    /** The bundled photo of [type] (as an [AppImages] path), or null for a clear sky. */
    fun getCloudImagePath(type: CloudGenus?, images: AppImages = getAppService()): String? {
        val file = when (type) {
            CloudGenus.Cirrus -> "cirrus"
            CloudGenus.Cirrocumulus -> "cirrocumulus"
            CloudGenus.Cirrostratus -> "cirrostratus"
            CloudGenus.Altocumulus -> "altocumulus"
            CloudGenus.Altostratus -> "altostratus"
            CloudGenus.Nimbostratus -> "nimbostratus"
            CloudGenus.Stratus -> "stratus"
            CloudGenus.Stratocumulus -> "stratocumulus"
            CloudGenus.Cumulus -> "cumulus"
            CloudGenus.Cumulonimbus -> "cumulonimbus"
            null -> return null
        }
        return images.asset("survival_guide/$file.webp")
    }

    companion object {
        /** The sky shown in place of a photo when there are no clouds. */
        const val CLEAR_SKY_COLOR = 0xFF84BFDF
    }
}
