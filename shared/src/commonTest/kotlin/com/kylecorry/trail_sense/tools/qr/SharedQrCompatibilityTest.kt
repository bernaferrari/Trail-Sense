package com.kylecorry.trail_sense.tools.qr

import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.from
import com.kylecorry.trail_sense.shared.toBeacon
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.notes.domain.Note
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedQrCompatibilityTest {
    @Test
    fun androidGeoUriFormatRoundTrips() {
        val beacon = Beacon(0, "Água & café", Coordinate(-25.5, -49.5), elevation = 372.25f, color = AppColor.Orange.color)
        val decoded = requireNotNull(GeoUri.parse(GeoUri.from(beacon).toString())).toBeacon()
        assertEquals(beacon, decoded)
    }

    @Test
    fun titledAndLegacyUntitledNotesRemainReadable() {
        val encoder = NoteQREncoder()
        val titled = Note("Trail", "Made it\n\n\nSafely", 0L)
        val payload = encoder.encode(titled)
        assertEquals(titled.title, encoder.decodeTitledNote(payload)?.title)
        assertEquals(titled.contents, encoder.decodeTitledNote(payload)?.contents)
        assertNull(encoder.decodeTitledNote("https://example.com/"))
        assertNull(encoder.decodeTitledNote("Plain text"))
        assertEquals("Plain text", encoder.decode("Plain text").contents)
    }
}
