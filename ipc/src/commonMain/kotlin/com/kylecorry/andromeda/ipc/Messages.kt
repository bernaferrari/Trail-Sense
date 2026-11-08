package com.kylecorry.andromeda.ipc

/** Portable header values supported by the IPC transport. */
sealed interface IpcValue {
    data object Null : IpcValue
    data class Text(val value: String) : IpcValue
    data class Bool(val value: Boolean) : IpcValue
    data class Int32(val value: Int) : IpcValue
    data class Int64(val value: Long) : IpcValue
    data class Float32(val value: Float) : IpcValue
    data class Float64(val value: Double) : IpcValue
    class Bytes(val value: ByteArray) : IpcValue {
        override fun equals(other: Any?): Boolean = other is Bytes && value.contentEquals(other.value)
        override fun hashCode(): Int = value.contentHashCode()
    }
}

class InterprocessCommunicationRequest(
    val headers: Map<String, IpcValue> = emptyMap(),
    val payload: ByteArray? = null,
)

class InterprocessCommunicationResponse(
    val code: Int,
    val headers: Map<String, IpcValue> = emptyMap(),
    val payload: ByteArray? = null,
)
