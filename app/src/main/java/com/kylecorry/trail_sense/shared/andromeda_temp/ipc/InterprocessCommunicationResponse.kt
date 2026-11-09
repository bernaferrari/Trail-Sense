package com.kylecorry.trail_sense.shared.andromeda_temp.ipc

import android.os.Bundle

class InterprocessCommunicationResponse(
    val code: Int,
    val headers: Bundle,
    val payload: ByteArray?
)