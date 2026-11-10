package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.mapsforge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.kylecorry.andromeda.canvas.CanvasDrawer
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.shared.icons.toBitmap
import com.kylecorry.trail_sense.shared.icons.trail.*
import org.mapsforge.map.rendertheme.XmlThemeResourceProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class DrawableResourceProvider(private val context: Context) : XmlThemeResourceProvider {

    private val sourcePattern = Regex("@drawable/(\\w+)(#[\\da-zA-Z]+)?(!)?")

    override fun createInputStream(relativePath: String?, source: String?): InputStream? {
        val bitmap = getDrawable(source) ?: return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        bitmap.recycle()
        return ByteArrayInputStream(outputStream.toByteArray())
    }

    private fun getDrawable(source: String?): Bitmap? {
        val (icon, color, isIconOnly) = getIcon(source) ?: return null
        val size = Resources.dp(context, 24f).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val drawer = CanvasDrawer(context, canvas)
        val padding = if (isIconOnly) 0f else drawer.dp(1.5f)
        drawer.smooth()
        if (!isIconOnly) {
            drawer.noStroke()
            drawer.fill(color ?: Color.BLACK)
            drawer.rect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), drawer.dp(4f))
            drawer.tint(Color.WHITE)
        } else {
            color?.let { drawer.tint(it) }
        }
        drawer.image(icon.toBitmap(size, size), padding, padding, canvas.width - 2 * padding, canvas.height - 2 * padding)
        return bitmap
    }

    private fun getIcon(source: String?): Triple<ImageVector, Int?, Boolean>? {
        val matches = sourcePattern.find(source ?: "") ?: return null
        val resourceName = matches.groupValues[1]
        val color = tryOrDefault(null) {
            matches.groupValues[2].toColorInt()
        }
        val iconOnly = matches.groupValues[3].isNotEmpty()
        val icon = icons[resourceName] ?: return null
        return Triple(icon, color, iconOnly)
    }

    companion object {
        // The render theme (assets/mapsforge/trail_sense_outdoors.xml) names its symbols by their old drawable names
        private val icons by lazy {
            mapOf(
                "swamp_pattern" to TrailIcons.SwampPattern,
                "cave" to TrailIcons.Cave,
                "triangle" to TrailIcons.Triangle,
                "volcano" to TrailIcons.Volcano,
                "shelter" to TrailIcons.Shelter,
                "ic_cabin" to TrailIcons.Cabin,
                "ic_category_shelter" to TrailIcons.CategoryShelter,
                "ic_restrooms" to TrailIcons.Restrooms,
                "ic_picnic" to TrailIcons.Picnic,
                "ic_phone" to TrailIcons.Phone,
                "ic_category_water" to TrailIcons.CategoryWater,
                "ic_car" to TrailIcons.Car,
                "ranger_station" to TrailIcons.RangerStation,
                "ic_binoculars" to TrailIcons.Binoculars,
                "ic_info" to TrailIcons.Info
            )
        }
    }
}
