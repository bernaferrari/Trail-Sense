package com.kylecorry.trail_sense.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals

class SystemStringsIosTest {
    @Test
    fun readsUiKitTitlesInEnglish() {
        assertEquals("OK", iosSystemString(SystemString.Ok))
        assertEquals("Cancel", iosSystemString(SystemString.Cancel))
        assertEquals("Copy", iosSystemString(SystemString.Copy))
    }
}
