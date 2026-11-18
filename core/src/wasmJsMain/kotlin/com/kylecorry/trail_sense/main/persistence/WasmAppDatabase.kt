package com.kylecorry.trail_sense.main.persistence

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.w3c.dom.Worker
import kotlin.js.ExperimentalWasmJsInterop

/** Open the database before exposing tools that read or write user data. */
suspend fun openWasmDatabase(): AppDatabase {
    val worker = createWorker()
    val database = Room.databaseBuilder<AppDatabase>(
        name = "trail-sense.db",
        factory = AppDatabaseConstructor::initialize
    )
        .setDriver(WebWorkerSQLiteDriver(worker))
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
    try {
        check(withTimeoutOrNull(20_000) { database.noteDao().getAll().first() } != null) {
            "Browser storage did not respond"
        }
    } catch (failure: Throwable) {
        database.close()
        worker.terminate()
        throw failure
    }
    return database
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker =
    js("""new Worker(new URL("trail-sense-sqlite-worker", import.meta.url))""")
