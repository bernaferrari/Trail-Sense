package com.kylecorry.andromeda.signal

import kotlinx.coroutines.flow.Flow

data class CellSignalOptions(
    val updateCellCache: Boolean = true,
    val includeUnregistered: Boolean = false,
    val pathLossFactor: Float = 1f,
) {
    init { require(pathLossFactor.isFinite() && pathLossFactor > 0f) }
}

/** Whether the platform exposes cellular signal details; permissions and radio availability vary. */
expect val cellularSignalSupported: Boolean

/** Observes cellular signal details while collected. */
expect fun cellSignals(options: CellSignalOptions = CellSignalOptions()): Flow<List<CellSignal>>
