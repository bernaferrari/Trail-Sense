package com.kylecorry.andromeda.background

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class DailyScheduleTest {
    @Test fun plansEarlyDueAndAlreadyRunDeliveries() {
        val zone = TimeZone.of("America/New_York")
        val time = LocalTime(9, 0)
        val early = planDailyTask(Instant.parse("2026-04-10T12:00:00Z"), time, zone)
        assertFalse(early.shouldRun)
        assertEquals(Instant.parse("2026-04-10T13:00:00Z"), early.nextRun)
        val due = planDailyTask(Instant.parse("2026-04-10T13:00:00Z"), time, zone)
        assertTrue(due.shouldRun)
        assertEquals(Instant.parse("2026-04-11T13:00:00Z"), due.nextRun)
        assertFalse(planDailyTask(Instant.parse("2026-04-10T13:00:00Z"), time, zone, LocalDate(2026, 4, 10)).shouldRun)
    }
}
