package com.kylecorry.andromeda.qr

import io.github.yuroyami.kiteqr.BinaryBitmap
import io.github.yuroyami.kiteqr.DecodeHintType
import io.github.yuroyami.kiteqr.RGBLuminanceSource
import io.github.yuroyami.kiteqr.common.HybridBinarizer
import io.github.yuroyami.kiteqr.oned.MultiFormatUPCEANReader
import io.github.yuroyami.kiteqr.qrcode.QRCodeReader

actual object CodeDecoder {
    actual fun qr(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean): String? =
        runCatching { QRCodeReader().decode(bitmap(pixels, width, height), hints(highAccuracy)).getText() }.getOrNull()

    actual fun barcode(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean): String? =
        runCatching { MultiFormatUPCEANReader(null).decode(bitmap(pixels, width, height), hints(highAccuracy)).getText() }.getOrNull()

    private fun bitmap(pixels: IntArray, width: Int, height: Int) =
        BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
    private fun hints(highAccuracy: Boolean): Map<DecodeHintType, *>? =
        if (highAccuracy) mapOf(DecodeHintType.TRY_HARDER to true) else null
}
