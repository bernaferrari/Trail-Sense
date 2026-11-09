package com.kylecorry.andromeda.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception

public class AssetFileSystem(private val context: Context) {

    public suspend fun read(path: String): String = withContext(Dispatchers.IO) {
        stream(path).use {
            it.bufferedReader().readText()
        }
    }

    public suspend fun stream(path: String): InputStream = withContext(Dispatchers.IO) {
        context.assets.open(path)
    }

    public fun list(path: String): List<String> {
        return (context.assets.list(path) ?: emptyArray()).toList()
    }
}