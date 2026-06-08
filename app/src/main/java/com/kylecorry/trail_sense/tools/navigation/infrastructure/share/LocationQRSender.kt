package com.kylecorry.trail_sense.tools.navigation.infrastructure.share

import android.content.Context
import androidx.navigation.NavController
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.tools.qr.infrastructure.LocationQREncoder

class LocationQRSender(
    private val context: Context,
    private val navController: NavController
) : ILocationSender {
    override fun send(location: Coordinate, format: BuiltInCoordinateFormat?) {
        CustomUiUtils.showQR(
            context,
            navController,
            context.getString(R.string.location),
            LocationQREncoder().encode(location)
        )
    }
}
