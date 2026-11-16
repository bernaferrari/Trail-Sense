package com.kylecorry.trail_sense.tools.qr.infrastructure

expect suspend fun decodeQrFromFrame(width: Int, height: Int, argb: IntArray): String?

expect suspend fun decodeQrFromImageBytes(bytes: ByteArray): String?
