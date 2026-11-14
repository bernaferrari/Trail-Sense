package com.kylecorry.andromeda.qr

import qrcode.QRCode
import qrcode.raw.ErrorCorrectionLevel

/** Generates a PNG image using the same error correction levels as the Android QR API. */
public object QRCodeGenerator {
    public fun png(text: String, pixelsPerModule: Int = 10, errorCorrection: QRErrorCorrection = QRErrorCorrection.M): ByteArray {
        require(pixelsPerModule > 0)
        val level = when (errorCorrection) {
            QRErrorCorrection.L -> ErrorCorrectionLevel.LOW
            QRErrorCorrection.M -> ErrorCorrectionLevel.MEDIUM
            QRErrorCorrection.Q -> ErrorCorrectionLevel.HIGH
            QRErrorCorrection.H -> ErrorCorrectionLevel.VERY_HIGH
        }
        return QRCode.ofSquares().withSize(pixelsPerModule).withErrorCorrectionLevel(level).build(text).renderToBytes()
    }
}
