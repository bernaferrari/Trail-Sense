package com.kylecorry.andromeda.files

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BundledAssetsTest {
    @Test fun readsAndListsAssets(): Unit = runBlocking {
        assertEquals("shared asset", readAssetText("bundled-test/note.txt"))
        assertTrue("note.txt" in listAssets("bundled-test"))
        assertFailsWith<IllegalArgumentException> { openAsset("../outside") }
    }
}
