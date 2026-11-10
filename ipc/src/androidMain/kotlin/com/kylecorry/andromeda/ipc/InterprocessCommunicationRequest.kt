package com.kylecorry.andromeda.ipc

import android.os.Bundle
import androidx.core.os.bundleOf

public class InterprocessCommunicationRequest(
    public val headers: Bundle = bundleOf(),
    public val payload: ByteArray? = null
)