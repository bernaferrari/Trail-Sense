package com.kylecorry.andromeda.ipc

/** Portable header values supported by the IPC transport. */
public sealed interface IpcValue {
    public data object Null : IpcValue
    public data class Text(val value: String) : IpcValue
    public data class Bool(val value: Boolean) : IpcValue
    public data class Int32(val value: Int) : IpcValue
    public data class Int64(val value: Long) : IpcValue
    public data class Float32(val value: Float) : IpcValue
    public data class Float64(val value: Double) : IpcValue
    public class Bytes(public val value: ByteArray) : IpcValue {
        override fun equals(other: Any?): Boolean = other is Bytes && value.contentEquals(other.value)
        override fun hashCode(): Int = value.contentHashCode()
    }
}

public class IpcRequest(
    public val headers: Map<String, IpcValue> = emptyMap(),
    public val payload: ByteArray? = null,
)

public class IpcResponse(
    public val code: Int,
    public val headers: Map<String, IpcValue> = emptyMap(),
    public val payload: ByteArray? = null,
)
