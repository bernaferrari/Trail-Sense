package com.kylecorry.andromeda.qr

/** Decodes QR codes and UPC/EAN barcodes from ARGB pixels. */
public expect object CodeDecoder {
    public fun qr(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean = false): String?
    public fun barcode(pixels: IntArray, width: Int, height: Int, highAccuracy: Boolean = false): String?
}
