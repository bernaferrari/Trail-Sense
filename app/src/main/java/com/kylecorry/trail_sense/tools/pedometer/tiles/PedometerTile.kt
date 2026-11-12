package com.kylecorry.trail_sense.tools.pedometer.tiles

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.tiles.TopicTile
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class PedometerTile : TopicTile() {

    private val pedometer by lazy { PedometerSubsystem.getInstance(this) }
    private val formatter by lazy { FormatService.getInstance(this) }
    private val prefs by lazy { UserPreferences(this) }

    override val icon: ImageVector
        get() = TrailIcons.Steps

    override val stateTopic: StateFlow<FeatureState>
        get() = pedometer.state

    override val subtitleTopic: Flow<String>
        get() = pedometer.distance.map {
            val converted = it.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            formatter.formatDistance(converted, Units.getDecimalPlaces(converted.units))
        }

    override fun stop() {
        pedometer.disable()
    }

    override fun start() {
        startForegroundService {
            pedometer.enable()
        }
    }

}
