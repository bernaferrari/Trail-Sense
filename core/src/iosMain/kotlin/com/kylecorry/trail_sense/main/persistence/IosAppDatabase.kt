package com.kylecorry.trail_sense.main.persistence

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Opens the shared database in the app's writable container. */
fun iosAppDatabase(path: String): AppDatabase = Room.databaseBuilder<AppDatabase>(name = path)
    .setDriver(BundledSQLiteDriver())
    .addMigrations(NotesToSharedSchema)
    .build()
