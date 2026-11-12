package com.kylecorry.trail_sense.tools.clouds.infrastructure

import androidx.core.graphics.drawable.toDrawable
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.io.FileSubsystem

/** [CloudDetailsText] plus the cloud photos as Android drawables, for the cloud screens still on Android. */
class CloudDetailsService(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val text = CloudDetailsText()

    fun getCloudDescription(type: CloudGenus?): String = text.getCloudDescription(type)

    fun getCloudForecast(type: CloudGenus?): String = text.getCloudForecast(type)

    fun getCloudName(type: CloudGenus?): String = text.getCloudName(type)

    fun getCloudImage(context: Context, type: CloudGenus?): Drawable? {
        val files = DependencyRegistry.get<FileSubsystem>()
        return when (type) {
            CloudGenus.Cirrus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cirrus.webp")
            CloudGenus.Cirrocumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cirrocumulus.webp")
            CloudGenus.Cirrostratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cirrostratus.webp")
            CloudGenus.Altocumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/altocumulus.webp")
            CloudGenus.Altostratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/altostratus.webp")
            CloudGenus.Nimbostratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/nimbostratus.webp")
            CloudGenus.Stratus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/stratus.webp")
            CloudGenus.Stratocumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/stratocumulus.webp")
            CloudGenus.Cumulus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cumulus.webp")
            CloudGenus.Cumulonimbus -> files.drawable("${files.SCHEME_ASSETS}survival_guide/cumulonimbus.webp")
            null -> "#84bfdf".toColorInt().toDrawable()
        }
    }

}
