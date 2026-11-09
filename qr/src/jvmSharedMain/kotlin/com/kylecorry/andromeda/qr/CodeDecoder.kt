package com.kylecorry.andromeda.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.oned.MultiFormatUPCEANReader
import com.google.zxing.qrcode.QRCodeReader

public actual object CodeDecoder {
    public actual fun qr(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean): String? =
        runCatching { QRCodeReader().decode(bitmap(pixels, width, height), hints(highAccuracy)).text }.getOrNull()

    public actual fun barcode(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean): String? =
        runCatching { MultiFormatUPCEANReader(null).decode(bitmap(pixels, width, height), hints(highAccuracy)).text }.getOrNull()

    private fun bitmap(pixels: IntArray, width: Int, height: Int) =
        BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
    private fun hints(highAccuracy: Boolean): Map<DecodeHintType, *>? =
        if (highAccuracy) mapOf(DecodeHintType.TRY_HARDER to true) else null
}
