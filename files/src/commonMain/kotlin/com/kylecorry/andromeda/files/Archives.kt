package com.kylecorry.andromeda.files

import no.synth.kmpzip.okio.unzipFrom
import no.synth.kmpzip.okio.zipTo

/** Creates a ZIP from files or directories in this store. */
suspend fun FileStorage.zip(archive: String, sources: List<String>, password: String? = null) {
    fileSystem.zipTo(
        target = root.resolve(archive),
        sources = sources.map(root::resolve),
        password = password,
    )
}

/** Extracts a ZIP into this store, rejecting archive entries that escape the destination. */
suspend fun FileStorage.unzip(archive: String, destination: String, password: String? = null) {
    fileSystem.unzipFrom(
        archive = root.resolve(archive),
        target = root.resolve(destination),
        password = password,
    )
}
