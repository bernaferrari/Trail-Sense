package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import com.kylecorry.trail_sense.shared.ProguardIgnore
import com.kylecorry.trail_sense.shared.text.RawText
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The read-only pages bundled with the app. Built-in pages have negative ids: page n of the index is -(n + 1).
 */
class BuiltInFieldGuide(
    private val reader: RawTextReader,
    private val assetPath: (String) -> String
) {

    @Serializable
    private class Pages : ProguardIgnore {
        var pages: List<BuiltInFieldGuidePage> = emptyList()
    }

    @Serializable
    private class BuiltInFieldGuidePage : ProguardIgnore {
        var content: String = ""
        var scientificName: String? = null
        var image: String = ""
        var tags: List<String> = emptyList()
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Mutex()
    private var pages: List<BuiltInFieldGuidePage>? = null

    private suspend fun getPages(): List<BuiltInFieldGuidePage> = lock.withLock {
        pages ?: run {
            val rawJson = reader.read(RawTexts.field_guide_pages)
            val parsed = runCatching { json.decodeFromString<Pages>(rawJson).pages }.getOrDefault(emptyList())
            pages = parsed
            parsed
        }
    }

    suspend fun getFieldGuidePage(id: Long): FieldGuidePage? {
        val index = -id.toInt() - 1
        return getPages().getOrNull(index)?.let { loadPage(it, index) }
    }

    suspend fun getFieldGuide(): List<FieldGuidePage> {
        return getPages().mapIndexed { index, page -> loadPage(page, index) }
    }

    private suspend fun loadPage(
        page: BuiltInFieldGuidePage,
        index: Int
    ): FieldGuidePage {
        val text = reader.read(RawText(page.content.removePrefix("raw/")))
        val lines = text.split("\n")
        val name = lines.first()
        val notes = lines.drop(1).joinToString("\n").trim()
        return FieldGuidePage(
            id = -(index.toLong() + 1),
            name = name,
            scientificName = page.scientificName,
            images = listOf(assetPath(page.image)),
            directTags = page.tags.map { FieldGuidePageTag.valueOf(it.split("_").last()) },
            notes = notes,
            isReadOnly = true
        )
    }
}
