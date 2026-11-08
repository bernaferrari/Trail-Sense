package com.kylecorry.andromeda.connection

import kotlin.time.Instant

class NearbyDeviceMessage(val time: Instant, val message: ByteArray)