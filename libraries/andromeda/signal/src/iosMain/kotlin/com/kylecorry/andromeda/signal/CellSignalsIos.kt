package com.kylecorry.andromeda.signal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public actual val cellularSignalSupported: Boolean = false
public actual fun cellSignals(options: CellSignalOptions): Flow<List<CellSignal>> = flow {
    throw UnsupportedOperationException("Cellular signal details are unavailable on this platform")
}
