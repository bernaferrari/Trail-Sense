package com.kylecorry.andromeda.bitmaps

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals

class FileImageRegionDecoderTest {
    @Test fun readsRegionsFromAFile() {
        val image = PixelImage(2, 2, intArrayOf(0xff112233.toInt(), 0xff445566.toInt(), 0xff778899.toInt(), 0xffaabbcc.toInt()))
        val path = Files.createTempFile("andromeda-region-", ".png")
        try {
            Files.write(path, image.encodePng())
            ImageRegionDecoder(path.toString()).use { decoder ->
                assertContentEquals(intArrayOf(image.pixels[1], image.pixels[3]), decoder.decode(1, 0, 1, 2).pixels)
            }
        } finally { Files.deleteIfExists(path) }
    }
}
