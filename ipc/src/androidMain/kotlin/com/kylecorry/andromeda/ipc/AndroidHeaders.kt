package com.kylecorry.andromeda.ipc

import android.os.Bundle

fun Map<String, IpcValue>.toBundle(): Bundle = Bundle().also { bundle ->
    for ((key, value) in this) when (value) {
        IpcValue.Null -> bundle.putString(key, null)
        is IpcValue.Text -> bundle.putString(key, value.value)
        is IpcValue.Bool -> bundle.putBoolean(key, value.value)
        is IpcValue.Int32 -> bundle.putInt(key, value.value)
        is IpcValue.Int64 -> bundle.putLong(key, value.value)
        is IpcValue.Float32 -> bundle.putFloat(key, value.value)
        is IpcValue.Float64 -> bundle.putDouble(key, value.value)
        is IpcValue.Bytes -> bundle.putByteArray(key, value.value)
    }
}

@Suppress("DEPRECATION")
fun Bundle.toIpcHeaders(): Map<String, IpcValue> = keySet().associateWith { key ->
    when (val value = get(key)) {
        null -> IpcValue.Null
        is String -> IpcValue.Text(value)
        is Boolean -> IpcValue.Bool(value)
        is Int -> IpcValue.Int32(value)
        is Long -> IpcValue.Int64(value)
        is Float -> IpcValue.Float32(value)
        is Double -> IpcValue.Float64(value)
        is ByteArray -> IpcValue.Bytes(value)
        else -> error("Unsupported IPC header type for $key: ${value::class}")
    }
}
