package com.kylecorry.trail_sense.main.persistence

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun jvmAppDatabase(path: String): AppDatabase = Room.databaseBuilder<AppDatabase>(name = path)
    .setDriver(BundledSQLiteDriver())
    .build()
