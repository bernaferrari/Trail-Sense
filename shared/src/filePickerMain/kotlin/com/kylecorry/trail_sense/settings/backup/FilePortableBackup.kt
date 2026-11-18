package com.kylecorry.trail_sense.settings.backup

import androidx.room3.PooledConnection
import androidx.room3.Transactor
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.andromeda.files.zip
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import no.synth.kmpzip.okio.ZipFile
import okio.buffer
import okio.use
import kotlin.uuid.Uuid

/** Stores a Room snapshot and app-owned media in a versioned ZIP. */
class FilePortableBackup(private val database: AppDatabase, private val files: FileStorage) : PortableBackup {
    private val operation = Mutex()

    override suspend fun export(): ByteArray = withWorkspace { workspace ->
        val snapshot = "$workspace/$DATABASE"
        val archive = "$workspace/backup.zip"
        exportDatabase(files.root.resolve(snapshot).toString())
        files.writeText("$workspace/$MANIFEST", VERSION)
        val sources = listOf(snapshot, "$workspace/$MANIFEST") + mediaFolders.filter(files::exists)
        files.zip(archive, sources)
        require((files.metadata(archive)?.size ?: 0L) in 1L..MAX_BACKUP_BYTES)
        files.read(archive)
    }

    override suspend fun restore(bytes: ByteArray): Unit = withWorkspace { workspace ->
        require(bytes.size.toLong() in 1L..MAX_BACKUP_BYTES)
        val archive = "$workspace/incoming.zip"
        files.write(archive, bytes)
        val entries = extract(archive, workspace)
        require(files.readText("$workspace/$MANIFEST") == VERSION)
        val media = entries.filter { it.substringBefore('/') in mediaFolders }
        val installed = mutableListOf<String>()
        try {
            for (name in media) {
                val staged = "$workspace/$name"
                if (files.exists(name)) {
                    require(files.source(name).buffer().use { existing ->
                        files.source(staged).buffer().use { source -> sameContents(existing, source) }
                    }) { "A media file with this name already exists" }
                    continue
                }
                files.move(staged, name)
                installed.add(name)
            }
            withContext(NonCancellable) {
                restoreDatabase(files.root.resolve("$workspace/$DATABASE").toString())
            }
        } catch (exception: Exception) {
            installed.forEach { runCatching { files.delete(it) } }
            throw exception
        }
    }

    private fun extract(archive: String, workspace: String): Set<String> {
        val paths = mutableSetOf<String>()
        files.fileSystem.openReadOnly(files.root.resolve(archive)).use { handle ->
            ZipFile(handle).use { zip ->
                require(zip.entries.size <= MAX_ENTRIES)
                var total = 0L
                val buffer = ByteArray(32_768)
                for (entry in zip.entries) {
                    val name = entry.name
                    if (entry.isDirectory) continue
                    require(name == MANIFEST || name == DATABASE || isMediaEntry(name))
                    require(paths.add(name))
                    require(entry.size in 0..(MAX_BACKUP_BYTES - total))
                    zip.getInputStream(entry).use { input ->
                        files.sink("$workspace/$name").buffer().use { output ->
                            var remaining = entry.size
                            while (true) {
                                val count = input.read(buffer, 0, buffer.size)
                                if (count == -1) break
                                require(count > 0 && count <= remaining)
                                remaining -= count
                                output.write(buffer, 0, count)
                            }
                            require(remaining == 0L)
                        }
                    }
                    total += entry.size
                }
            }
        }
        require(MANIFEST in paths && DATABASE in paths)
        return paths
    }

    private suspend fun exportDatabase(snapshot: String) = withSnapshot(snapshot) { connection ->
        connection.immediateTransaction {
            val tables = connection.portableTables() + "room_master_table"
            for (name in tables) {
                val table = name.sqlIdentifier()
                connection.execute("CREATE TABLE portable_backup.$table AS SELECT * FROM main.$table")
            }
        }
    }

    private suspend fun restoreDatabase(snapshot: String) = withSnapshot(snapshot) { connection ->
        val identity = connection.queryStrings("SELECT identity_hash FROM main.room_master_table WHERE id = 42")
        require(identity.isNotEmpty() && identity == connection.queryStrings(
            "SELECT identity_hash FROM portable_backup.room_master_table WHERE id = 42"
        ))
        require(connection.queryStrings("PRAGMA portable_backup.quick_check(1)") == listOf("ok"))

        val tables = connection.portableTables()
        val backupTables = connection.queryStrings(
            "SELECT name FROM portable_backup.sqlite_master WHERE type = 'table' AND sql LIKE 'CREATE TABLE%'"
        )
        require(tables.all { it in backupTables })
        connection.immediateTransaction {
            connection.execute("PRAGMA defer_foreign_keys = ON")
            tables.forEach { connection.execute("DELETE FROM main.${it.sqlIdentifier()}") }
            for (name in tables) {
                val table = name.sqlIdentifier()
                // Match by column name even if a platform migration changed the original column order.
                val columns = connection.queryStrings("PRAGMA main.table_info($table)", 1)
                    .joinToString(", ") { it.sqlIdentifier() }
                connection.execute("INSERT INTO main.$table ($columns) SELECT $columns FROM portable_backup.$table")
            }
            connection.execute("UPDATE main.paths SET temporary = 0 WHERE temporary = 1")
        }
    }

    private suspend fun <T> withSnapshot(snapshot: String, block: suspend (Transactor) -> T): T =
        database.useWriterConnection { connection ->
            connection.usePrepared("ATTACH DATABASE ? AS portable_backup") {
                it.bindText(1, snapshot)
                it.step()
            }
            try {
                block(connection)
            } finally {
                withContext(NonCancellable) { connection.execute("DETACH DATABASE portable_backup") }
            }
        }

    private suspend fun PooledConnection.portableTables(): List<String> =
        queryStrings(
            "SELECT name FROM main.sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
        ).filter { it !in deviceTables }

    private suspend fun <T> withWorkspace(block: suspend (String) -> T): T = withContext(ioDispatcher) {
        operation.withLock {
            val workspace = ".portable-backup/${Uuid.random()}"
            files.createDirectory(workspace)
            try {
                block(workspace)
            } finally {
                runCatching { files.delete(workspace, recursive = true) }
            }
        }
    }

    private fun isMediaEntry(name: String): Boolean {
        val parts = name.split('/')
        return parts.size in 2..5 && parts.first() in mediaFolders && parts.all(mediaSegment::matches)
    }

    private fun sameContents(first: okio.BufferedSource, second: okio.BufferedSource): Boolean {
        while (!first.exhausted()) {
            val count = minOf(32_768, first.buffer.size).toLong()
            if (!second.request(count) || first.readByteString(count) != second.readByteString(count)) return false
        }
        return second.exhausted()
    }

    private suspend fun PooledConnection.queryStrings(sql: String, column: Int = 0): List<String> =
        usePrepared(sql) { statement -> buildList { while (statement.step()) add(statement.getText(column)) } }

    private suspend fun PooledConnection.execute(sql: String) {
        usePrepared(sql) { it.step() }
    }

    private fun String.sqlIdentifier(): String = "\"${replace("\"", "\"\"")}\""

    companion object {
        private const val MANIFEST = "trail-sense-backup.txt"
        private const val VERSION = "Trail Sense Portable Backup 1"
        private const val DATABASE = "trail-sense.sqlite"
        private const val MAX_BACKUP_BYTES = 256L * 1024 * 1024
        private const val MAX_ENTRIES = 5000
        private val mediaFolders = setOf("offline_maps", "field_guide", "maps")
        private val mediaSegment = Regex("[A-Za-z0-9_][A-Za-z0-9_. -]{0,127}")
        private val deviceTables = setOf(
            "room_master_table", "android_metadata", "cached_tiles", "dem", "plugins", "plugin_registrations"
        )
    }
}
