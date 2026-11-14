package com.kylecorry.andromeda.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.*
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.core.graphics.createBitmap

public object QR {
    public fun encode(
        text: String,
        width: Int,
        height: Int,
        errorCorrection: QRErrorCorrection = QRErrorCorrection.M
    ): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to mapErrorCorrectionLevel(errorCorrection)
        )
        val qr = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (qr.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = createBitmap(width, height)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    public fun decode(image: Bitmap, highAccuracy: Boolean = false): String? {
        val pixels = IntArray(image.width * image.height)
        image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
        return CodeDecoder.qr(pixels, image.width, image.height, highAccuracy)
    }

    private fun mapErrorCorrectionLevel(errorCorrection: QRErrorCorrection): String {
        return when (errorCorrection) {
            QRErrorCorrection.L -> ErrorCorrectionLevel.L
            QRErrorCorrection.M -> ErrorCorrectionLevel.M
            QRErrorCorrection.Q -> ErrorCorrectionLevel.Q
            QRErrorCorrection.H -> ErrorCorrectionLevel.H
        }.toString()
    }
}