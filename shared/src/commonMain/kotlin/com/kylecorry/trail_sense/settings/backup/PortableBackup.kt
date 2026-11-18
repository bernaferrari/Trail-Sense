package com.kylecorry.trail_sense.settings.backup

interface PortableBackup {
    suspend fun export(): ByteArray
    suspend fun restore(bytes: ByteArray)
}
