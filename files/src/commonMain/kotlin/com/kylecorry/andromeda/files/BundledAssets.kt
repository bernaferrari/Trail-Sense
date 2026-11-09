package com.kylecorry.andromeda.files

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Source
import okio.buffer
import okio.use

/** Opens a bundled asset. The caller owns the returned source. */
public expect fun openAsset(path: String): Source

/** Lists immediate child names in a bundled asset directory. */
public expect fun listAssets(path: String = ""): List<String>

/** Reads a bundled asset without blocking the calling coroutine dispatcher. */
public suspend fun readAsset(path: String): ByteArray = withContext(Dispatchers.Default) {
    openAsset(path).buffer().use { it.readByteArray() }
}

public suspend fun readAssetText(path: String): String = readAsset(path).decodeToString()

internal fun assetPath(path: String): String {
    require(!path.startsWith('/') && '\\' !in path) { "Asset path must be relative" }
    val parts = path.split('/').filter { it.isNotEmpty() && it != "." }
    require(parts.none { it == ".." }) { "Asset path cannot leave the bundle" }
    return parts.joinToString("/")
}
