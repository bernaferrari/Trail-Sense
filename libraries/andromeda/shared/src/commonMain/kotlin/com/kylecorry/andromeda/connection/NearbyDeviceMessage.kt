package com.kylecorry.andromeda.connection

import kotlin.time.Instant

public class NearbyDeviceMessage(public val time: Instant, public val message: ByteArray)