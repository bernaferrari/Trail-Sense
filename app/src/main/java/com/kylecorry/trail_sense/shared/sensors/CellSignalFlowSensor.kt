package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.andromeda.signal.CellSignalOptions
import com.kylecorry.andromeda.signal.cellSignals
import com.kylecorry.andromeda.signal.CellSignalSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Adapts andromeda's multiplatform [cellSignals] flow to the app's sensor interface.
 */
class CellSignalFlowSensor(
    private val options: CellSignalOptions,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : SharedSensor(), CellSignalSensor {

    @Volatile
    override var signals: List<CellSignal> = emptyList()
        private set

    @Volatile
    private var gotReading = false

    override val hasValidReading: Boolean
        get() = gotReading

    private var job: Job? = null

    override fun startImpl() {
        job?.cancel()
        job = scope.launch {
            cellSignals(options)
                .catch { /* Missing permission or unsupported radio: keep the last reading */ }
                .collect {
                    signals = it
                    gotReading = true
                    setEventTimeToNow()
                    notifyListeners()
                }
        }
    }

    override fun stopImpl() {
        job?.cancel()
        job = null
    }
}
