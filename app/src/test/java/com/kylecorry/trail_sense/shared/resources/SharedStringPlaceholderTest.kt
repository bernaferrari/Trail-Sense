package com.kylecorry.trail_sense.shared.resources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Compose's own stringResource/pluralStringResource only fill positional placeholders (%1$s), so an app string
 * with %d or %s read through them shows the raw placeholder. Shared code must read those through appString or
 * appPluralString, which format like Android.
 */
class SharedStringPlaceholderTest {

    private val composeRead = Regex("""(?<![\w.])(stringResource|pluralStringResource)\(\s*Res\.(string|plurals)\.(\w+)""")

    @Test
    fun composeResourceReadsHaveNoAndroidPlaceholders() {
        val withPlaceholders = placeholderNames()
        assertTrue(withPlaceholders.isNotEmpty())

        val offenders = File("../shared/src/commonMain").walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                val importsCompose = text.contains("import org.jetbrains.compose.resources.stringResource") ||
                    text.contains("import org.jetbrains.compose.resources.pluralStringResource")
                if (!importsCompose) emptySequence() else composeRead.findAll(text)
                    .map { it.groupValues[3] }
                    .filter { it in withPlaceholders }
                    .map { "${file.name}: $it" }
            }
            .toList()

        assertEquals(emptyList<String>(), offenders)
    }

    private fun placeholderNames(): Set<String> {
        val xml = File("src/main/res/values/strings.xml").readText()
        val strings = Regex("""<string name="(\w+)"[^>]*>([\s\S]*?)</string>""").findAll(xml)
            .filter { it.groupValues[2].contains('%') }
            .map { it.groupValues[1] }
        val plurals = Regex("""<plurals name="(\w+)"[^>]*>([\s\S]*?)</plurals>""").findAll(xml)
            .filter { it.groupValues[2].contains('%') }
            .map { it.groupValues[1] }
        return (strings + plurals).toSet()
    }
}
