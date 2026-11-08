package com.kylecorry.andromeda.qr

/** Decodes QR codes and UPC/EAN barcodes from ARGB pixels. */
expect object CodeDecoder {
    fun qr(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean = false): String?
    fun barcode(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean = false): String?
}
