package com.kylecorry.trail_sense.main.persistence

import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.Worker
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

fun wasmAppDatabase(): AppDatabase = wasmAppDatabase(createFallbackWorker())

fun wasmAppDatabase(worker: Worker): AppDatabase {
    return Room.inMemoryDatabaseBuilder<AppDatabase>(
        factory = AppDatabaseConstructor::initialize
    )
        .setDriver(WebWorkerSQLiteDriver(worker))
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun createFallbackWorker(): Worker = js(
    """
    (() => {
        const code = `
            let nextStmt = 1;
            self.onmessage = (e) => {
                const msg = e.data || {};
                const d = msg.data || {};
                let res = {};
                if (d.fileName !== undefined) {
                    res = { databaseId: 1 };
                } else if (d.sql !== undefined) {
                    res = { statementId: nextStmt++, parameterCount: 0, columnNames: [] };
                } else if (d.bindings !== undefined) {
                    res = { rows: [], columnTypes: [] };
                }
                self.postMessage({ id: msg.id, data: res });
            };
        `;
        const blob = new Blob([code], { type: 'application/javascript' });
        return new Worker(URL.createObjectURL(blob));
    })()
    """
)
