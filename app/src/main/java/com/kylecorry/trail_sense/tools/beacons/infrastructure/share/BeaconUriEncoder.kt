package com.kylecorry.trail_sense.tools.beacons.infrastructure.share

import com.kylecorry.andromeda.core.system.from

import com.kylecorry.andromeda.core.system.uri

import android.net.Uri
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.toBeacon
import com.kylecorry.trail_sense.shared.uri.IUriEncoder
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon

class BeaconUriEncoder : IUriEncoder<Beacon> {

    override fun encode(value: Beacon): Uri {
        val geo = GeoUri.from(value)
        return geo.uri
    }

    override fun decode(uri: Uri): Beacon? = GeoUri.from(uri)?.toBeacon()
}
