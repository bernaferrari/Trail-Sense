package com.kylecorry.andromeda.ipc

import android.os.Bundle

public class InterprocessCommunicationResponse(
    public val code: Int,
    public val headers: Bundle,
    public val payload: ByteArray?
)