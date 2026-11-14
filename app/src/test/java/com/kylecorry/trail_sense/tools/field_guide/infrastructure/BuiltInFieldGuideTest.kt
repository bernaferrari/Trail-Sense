package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import com.kylecorry.trail_sense.shared.text.RawText
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolKeywordLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Reads the app's real bundled texts the way the Android reader does, by resource name. */
class BuiltInFieldGuideTest {

    private val reader = object : RawTextReader {
        override suspend fun read(text: RawText): String {
            val dir = File("src/main/res/raw")
            val file = dir.listFiles()!!.single { it.nameWithoutExtension == text.name }
            return file.readText()
        }
    }

    @Test
    fun loadsEveryBundledPage() = runBlocking {
        val guide = BuiltInFieldGuide(reader) { "android-assets://$it" }
        val pages = guide.getFieldGuide()
        val expected = Regex("\"content\"").findAll(File("src/main/res/raw/field_guide_pages.json").readText()).count()
        assertEquals(expected, pages.size)
        pages.forEachIndexed { index, page ->
            assertEquals(-(index + 1L), page.id)
            assertTrue(page.name.isNotBlank())
            assertTrue(page.isReadOnly)
            val asset = page.images.single().removePrefix("android-assets://")
            assertTrue(File("src/main/assets/$asset").exists(), asset)
            assertFalse(page.directTags.isEmpty())
        }
        val squirrel = guide.getFieldGuidePage(-1)
        assertNotNull(squirrel)
        assertEquals(pages.first(), squirrel)
    }

    @Test
    fun loadsToolKeywords() = runBlocking {
        val keywords = ToolKeywordLoader.load(reader)
        assertTrue(keywords.size > 20)
        assertTrue(keywords.values.all { it.isNotEmpty() })
    }

    @Test
    fun everyGeneratedTextHasAFile() = runBlocking {
        assertTrue(reader.read(RawTexts.guide_tool_tools).isNotBlank())
    }
}
