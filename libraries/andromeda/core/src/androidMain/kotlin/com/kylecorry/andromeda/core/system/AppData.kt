package com.kylecorry.andromeda.core.system

import android.content.Context
import androidx.core.content.ContextCompat
import java.io.File

public object AppData {

    public fun getDataDirectory(context: Context): File {
        return ContextCompat.getDataDir(context) ?: getFilesDirectory(context).parentFile!!
    }

    public fun getFilesDirectory(context: Context): File {
        return context.filesDir
    }

    public fun getCacheDirectory(context: Context): File {
        return context.cacheDir
    }

    public fun getDatabaseDirectory(context: Context, databaseName: String? = null): File {
        val lookedUp = if (databaseName != null) {
            context.getDatabasePath(databaseName)?.parentFile
        } else {
            val databases = context.databaseList()
            databases.firstOrNull()?.let {
                context.getDatabasePath(it)?.parentFile
            }
        }

        return lookedUp ?: File(getDataDirectory(context), "databases")
    }

    public fun getSharedPrefsDirectory(context: Context): File {
        return File(getDataDirectory(context), "shared_prefs")
    }

    public fun getSharedPrefsFile(
        context: Context,
        name: String = "${context.packageName}_preferences"
    ): File {
        return File(getSharedPrefsDirectory(context), "${name}.xml")
    }

    public fun getSharedPrefsFiles(context: Context): List<File> {
        return getSharedPrefsDirectory(context).listFiles()?.toList()
            ?.filter { it.extension == "xml" } ?: listOf()
    }

}