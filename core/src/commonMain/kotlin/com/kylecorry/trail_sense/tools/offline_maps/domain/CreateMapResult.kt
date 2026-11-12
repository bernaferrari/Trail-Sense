package com.kylecorry.trail_sense.tools.offline_maps.domain

enum class CreateOfflineMapError {
    InvalidMapFile,
    AccessDenied,
    UnableToCopy,
    Cancelled
}

/**
 * The outcome of creating an offline map. Each failure says why, so the UI can stay quiet when the
 * user cancelled and show a specific message otherwise.
 */
sealed interface CreateMapResult {
    data class Created(val map: IMap) : CreateMapResult
    data class Failed(val error: CreateOfflineMapError) : CreateMapResult

    fun mapOrNull(): IMap? = (this as? Created)?.map

    companion object {
        val cancelled: CreateMapResult = Failed(CreateOfflineMapError.Cancelled)
    }
}
