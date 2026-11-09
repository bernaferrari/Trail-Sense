package com.kylecorry.trail_sense.shared.andromeda_temp.ipc

import android.os.Bundle
import androidx.core.os.bundleOf

class InterprocessCommunicationRequest(
    val headers: Bundle = bundleOf(),
    val payload: ByteArray? = null
)