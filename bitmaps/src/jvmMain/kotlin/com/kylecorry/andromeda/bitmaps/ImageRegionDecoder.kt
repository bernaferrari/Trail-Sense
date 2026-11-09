package com.kylecorry.andromeda.bitmaps

import java.awt.Rectangle
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.ImageInputStream

public actual class ImageRegionDecoder private constructor(private val input: ImageInputStream) : AutoCloseable {
    public actual constructor(bytes: ByteArray) : this(requireNotNull(ImageIO.createImageInputStream(ByteArrayInputStream(bytes))) { "Unable to open image" })
    public actual constructor(path: String) : this(requireNotNull(ImageIO.createImageInputStream(File(path))) { "Unable to open image" })
    private val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: run {
        input.close()
        throw IllegalArgumentException("Unable to decode image")
    }
    private var closed = false
    public actual val width: Int
    public actual val height: Int

    init {
        try {
            reader.input = input
            width = reader.getWidth(0)
            height = reader.getHeight(0)
        } catch (error: Exception) {
            reader.dispose()
            input.close()
            throw error
        }
    }

    public actual fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage {
        check(!closed) { "Image decoder is closed" }
        requireImageRegion(left, top, width, height, this.width, this.height)
        val parameters = reader.defaultReadParam.apply { sourceRegion = Rectangle(left, top, width, height) }
        val image = requireNotNull(reader.read(0, parameters)) { "Unable to decode image region" }
        return PixelImage(width, height, image.getRGB(0, 0, width, height, null, 0, width))
    }

    actual override fun close() {
        if (!closed) { closed = true; reader.dispose(); input.close() }
    }
}
