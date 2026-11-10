package com.kylecorry.andromeda.files

import android.content.Context

public class LocalFileSystem(context: Context) : BaseFileSystem(context, context.filesDir.path)
public class CacheFileSystem(context: Context) : BaseFileSystem(context, context.cacheDir.path)
public class DataFileSystem(context: Context) : BaseFileSystem(context, context.dataDir.path)
