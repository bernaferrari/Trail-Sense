package com.kylecorry.trail_sense.web

import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapBlob
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapStorage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.WebFile
import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.files.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.js.toDouble

@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserOfflineMapStorage : OfflineMapStorage {
    override suspend fun copy(path: String, file: PlatformFile) {
        val selected = file.webFile as WebFile.FileWrapper
        browserMapsRequest("put", path, selected.file).await()
    }

    override suspend fun write(path: String, bytes: ByteArray) {
        val data = Uint8Array(bytes.size)
        for (index in bytes.indices) data[index] = bytes[index]
        browserMapsRequest("put", path, asBlob(data)).await()
    }

    override suspend fun open(path: String): OfflineMapBlob? {
        val blob = browserMapsRequest("get", path, null).await() ?: return null
        return object : OfflineMapBlob {
            override val size = blob.size.toDouble().toLong()
            override suspend fun read(offset: Long, length: Int): ByteArray {
                require(offset >= 0 && length >= 0 && offset <= size - length)
                val data = readBlob(blob, offset.toDouble(), length).await()
                return ByteArray(data.length) { data[it] }
            }
        }
    }

    override suspend fun delete(path: String) {
        browserMapsRequest("delete", path, null).await()
    }
}

@JsFun("(bytes) => new Blob([bytes])")
private external fun asBlob(bytes: Uint8Array): Blob

@JsFun("(blob, start, length) => blob.slice(start, start + length).arrayBuffer().then(bytes => new Uint8Array(bytes))")
private external fun readBlob(blob: Blob, start: Double, length: Int): Promise<Uint8Array>

@JsFun("""
    (action, path, blob) => new Promise((resolve, reject) => {
        const opening = indexedDB.open('trail-sense-offline-maps', 1);
        opening.onupgradeneeded = () => opening.result.createObjectStore('files');
        opening.onerror = () => reject(opening.error);
        opening.onblocked = () => reject(new Error('Map storage is busy'));
        opening.onsuccess = () => {
            const database = opening.result;
            const transaction = database.transaction('files', action === 'get' ? 'readonly' : 'readwrite');
            const store = transaction.objectStore('files');
            const request = action === 'put' ? store.put(blob, path) :
                action === 'get' ? store.get(path) : store.delete(path);
            transaction.oncomplete = () => {
                database.close();
                resolve(action === 'get' ? request.result ?? null : null);
            };
            transaction.onabort = () => {
                database.close();
                reject(transaction.error || request.error);
            };
        };
    })
""")
private external fun browserMapsRequest(action: String, path: String, blob: Blob?): Promise<Blob?>
