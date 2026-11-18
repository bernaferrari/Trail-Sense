package com.kylecorry.trail_sense.main.persistence

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.w3c.dom.Worker
import kotlin.js.ExperimentalWasmJsInterop

/** Open the database before exposing tools that read or write user data. */
suspend fun openWasmDatabase(name: String = "trail-sense.db"): OpenWasmDatabase {
    val worker = createWorker()
    val database = Room.databaseBuilder<AppDatabase>(
        name = name,
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
    return OpenWasmDatabase(database, worker)
}

class OpenWasmDatabase internal constructor(val database: AppDatabase, private val worker: Worker) {
    fun close() {
        database.close()
        worker.terminate()
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker =
    js("""new Worker(new URL("trail-sense-sqlite-worker", import.meta.url))""")
