package com.kylecorry.trail_sense.main.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteEntity
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class AndroidAppDatabaseMigrationTest {
    @Test
    fun version61UpgradeKeepsBeaconsAndNotes() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-${UUID.randomUUID()}.db"
        try {
            val fixture = AndroidAppDatabase.buildDatabase(context, name)
            val (beaconId, noteId) = try {
                fixture.beaconDao().insert(BeaconEntity.from(Beacon(0, "Trailhead", Coordinate(42.0, -71.0)))) to
                    fixture.noteDao().insert(NoteEntity("Hike", "Bring water", 123L))
            } finally {
                fixture.close()
            }

            // Version 62 added only the shape column. Recreate the version 61 schema around the seeded rows.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("ALTER TABLE beacons DROP COLUMN shape")
                it.version = 61
            }

            val upgraded = AndroidAppDatabase.buildDatabase(context, name)
            try {
                val beacon = requireNotNull(upgraded.beaconDao().get(beaconId))
                assertEquals("Trailhead", beacon.name)
                assertEquals(0, beacon.shape)
                assertEquals("Bring water", upgraded.noteDao().get(noteId)?.contents)
            } finally {
                upgraded.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
