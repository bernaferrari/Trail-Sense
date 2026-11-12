package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawText
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.ToolDiagnostic
import com.kylecorry.trail_sense.tools.tools.ui.sort.AlphabeticalToolSort
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolSearchTest {

    private data class FakeTool(
        override val id: Long,
        override val name: String,
        override val category: ToolCategory = ToolCategory.Other,
        override val route: Route = Route.Tools,
        override val additionalRoutes: List<KClass<out Route>> = emptyList()
    ) : ToolDescriptor {
        override val icon: ImageVector = Icons.Default.Search
        override val description: String? = null
        override val guideId: RawText? = null
        override val isExperimental: Boolean = false
        override val settingsRoute: Route? = null
        override val diagnostics: List<ToolDiagnostic> = emptyList()
    }

    @Test
    fun parsesKeywordLines() {
        val keywords = ToolKeywordLoader.parse(
            """
            1 (Flashlight): light, torch ,  lamp
            not a tool: ignored
            2: compass

            3 (Empty):
            """.trimIndent()
        )

        assertEquals(setOf("light", "torch", "lamp"), keywords[1])
        assertEquals(setOf("compass"), keywords[2])
        assertEquals(emptySet(), keywords[3])
        assertEquals(3, keywords.size)
    }

    @Test
    fun findsToolsByNameAndKeyword() {
        val flashlight = FakeTool(1, "Flashlight")
        val navigation = FakeTool(2, "Navigation")
        val search = ToolSearch(listOf(flashlight, navigation), mapOf(1L to setOf("torch")))

        assertEquals(flashlight, search.search("flash").first())
        assertEquals(flashlight, search.search("torch").first())
        assertEquals(navigation, search.search("Navig").first())
        assertTrue(search.search("  ").isEmpty())
    }

    @Test
    fun knowsWhenItsScreenIsOpen() {
        val tool = FakeTool(1, "Weather", route = Route.Weather, additionalRoutes = listOf(Route.Settings::class))
        assertTrue(tool.isOpen(Route.Weather))
        assertTrue(tool.isOpen(Route.Settings))
        assertFalse(tool.isOpen(Route.Astronomy))
        assertFalse(tool.isOpen(null))
    }

    @Test
    fun sortsAlphabetically() {
        val sorted = AlphabeticalToolSort().sort(listOf(FakeTool(1, "b"), FakeTool(2, "a")))
        assertEquals(listOf("a", "b"), sorted.single().tools.map { it.name })
    }
}
