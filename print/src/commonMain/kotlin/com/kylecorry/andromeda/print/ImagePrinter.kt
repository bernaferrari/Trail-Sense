package com.kylecorry.andromeda.print

import com.kylecorry.andromeda.bitmaps.PixelImage
import kotlin.math.max
import kotlin.math.min

enum class PrintScaleMode { Fit, Fill }
enum class PrintColorMode { Color, Monochrome }
enum class PrintOrientation { Auto, Portrait, Landscape }

data class ImagePrintOptions(
    val scale: PrintScaleMode = PrintScaleMode.Fit,
    val color: PrintColorMode = PrintColorMode.Color,
    val orientation: PrintOrientation = PrintOrientation.Auto,
)

/** Opens the system print dialog for an image. Invoke on the UI thread. */
fun interface ImagePrinter {
    fun print(image: PixelImage, jobName: String, options: ImagePrintOptions)
    fun print(image: PixelImage, jobName: String) = print(image, jobName, ImagePrintOptions())
}

internal data class ImagePrintLayout(val x: Double, val y: Double, val width: Double, val height: Double)

internal fun imagePrintLayout(imageWidth: Int, imageHeight: Int, pageWidth: Double, pageHeight: Double, mode: PrintScaleMode): ImagePrintLayout {
    val horizontal = pageWidth / imageWidth
    val vertical = pageHeight / imageHeight
    val scale = if (mode == PrintScaleMode.Fit) min(horizontal, vertical) else max(horizontal, vertical)
    val width = imageWidth * scale
    val height = imageHeight * scale
    return ImagePrintLayout((pageWidth - width) / 2, (pageHeight - height) / 2, width, height)
}
