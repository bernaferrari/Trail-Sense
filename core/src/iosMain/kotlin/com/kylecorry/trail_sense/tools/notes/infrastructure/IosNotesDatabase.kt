package com.kylecorry.trail_sense.tools.notes.infrastructure

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@Database(entities = [NoteEntity::class], version = 1, exportSchema = false)
@ConstructedBy(IosNotesDatabaseConstructor::class)
internal abstract class IosNotesDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
}

@Suppress("KotlinNoActualForExpect")
internal expect object IosNotesDatabaseConstructor : RoomDatabaseConstructor<IosNotesDatabase> {
    override fun initialize(): IosNotesDatabase
}

/** Opens a persistent notes database at an absolute path in the app's writable container. */
fun iosNoteRepository(path: String): INoteRepo {
    val database = Room.databaseBuilder<IosNotesDatabase>(name = path)
        .setDriver(BundledSQLiteDriver())
        .build()
    return NoteRepo(database.notes())
}
