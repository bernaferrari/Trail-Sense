package com.kylecorry.andromeda.print

import com.kylecorry.andromeda.bitmaps.PixelImage

/** Opens the system print dialog for an image, fitting it to the printable page. */
fun interface ImagePrinter {
    fun print(image: PixelImage, jobName: String)
}
