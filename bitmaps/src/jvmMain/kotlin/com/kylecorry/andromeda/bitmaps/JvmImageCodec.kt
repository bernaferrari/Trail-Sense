package com.kylecorry.andromeda.bitmaps

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

actual fun decodeImage(bytes: ByteArray): PixelImage {
    val bitmap = requireNotNull(ImageIO.read(ByteArrayInputStream(bytes))) { "Unable to decode image" }
    return PixelImage(bitmap.width, bitmap.height, bitmap.getRGB(0, 0, bitmap.width, bitmap.height, null, 0, bitmap.width))
}

actual fun PixelImage.encodePng(): ByteArray {
    val bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    bitmap.setRGB(0, 0, width, height, pixels, 0, width)
    return ByteArrayOutputStream().use { output ->
        check(ImageIO.write(bitmap, "png", output)) { "No PNG encoder available" }
        output.toByteArray()
    }
}

actual fun PixelImage.encodeJpeg(quality: Int): ByteArray {
    require(quality in 0..100)
    val bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    bitmap.setRGB(0, 0, width, height, pixels, 0, width)
    val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull() ?: error("No JPEG encoder available")
    return try {
        ByteArrayOutputStream().use { output ->
            ImageIO.createImageOutputStream(output).use { stream ->
                writer.output = stream
                val parameters = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = quality / 100f
                }
                writer.write(null, IIOImage(bitmap, null, null), parameters)
            }
            output.toByteArray()
        }
    } finally { writer.dispose() }
}
