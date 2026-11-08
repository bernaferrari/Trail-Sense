package com.kylecorry.andromeda.signal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual val cellularSignalSupported: Boolean = false
actual fun cellSignals(options: CellSignalOptions): Flow<List<CellSignal>> = flow {
    throw UnsupportedOperationException("Cellular signal details are unavailable on this platform")
}
