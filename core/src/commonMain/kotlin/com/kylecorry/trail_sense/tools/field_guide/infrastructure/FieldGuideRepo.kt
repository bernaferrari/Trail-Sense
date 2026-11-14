package com.kylecorry.trail_sense.tools.field_guide.infrastructure

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.getUpsertedId
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting

class FieldGuideRepo(
    private val pageDao: FieldGuidePageDao,
    private val sightingDao: FieldGuideSightingDao,
    private val preferences: FieldGuidePreferences,
    private val builtInPages: suspend () -> List<FieldGuidePage>,
    private val builtInPage: suspend (Long) -> FieldGuidePage?,
    private val deleteFile: (String) -> Unit
) : IFieldGuideRepo {

    override suspend fun getAllPages(): List<FieldGuidePage> = onIO {
        val saved = pageDao.getAllPages().map { it.toFieldGuidePage() }
        val sightings = sightingDao.getAllSightings().map { it.toSighting() }
        val all = builtInPages() + saved
        val hidden = preferences.hiddenPageIds.toSet()
        all.map {
            it.copy(
                sightings = sightings.filter { s -> s.fieldGuidePageId == it.id },
                isHidden = hidden.contains(it.id)
            )
        }
    }

    override suspend fun getPage(id: Long): FieldGuidePage? = onIO {
        val page = if (id < 0) {
            builtInPage(id)
        } else {
            pageDao.getPage(id)?.toFieldGuidePage()
        }

        val sightings = sightingDao.getSightingsForPage(id).map { it.toSighting() }
        page?.copy(sightings = sightings, isHidden = preferences.hiddenPageIds.contains(id))
    }

    override suspend fun delete(page: FieldGuidePage) = onIO {
        if (page.isReadOnly) {
            return@onIO
        }
        page.images.forEach { deleteFile(it) }
        sightingDao.deleteAllSightingsForPage(page.id)
        pageDao.delete(FieldGuidePageEntity.fromFieldGuidePage(page))
    }

    override suspend fun add(page: FieldGuidePage): Long = onIO {
        if (page.isReadOnly) {
            return@onIO -1
        }
        // Delete photos if they've changed
        if (page.id != 0L) {
            val existing = pageDao.getPage(page.id)?.toFieldGuidePage()
            existing?.images?.filter { it !in page.images }?.forEach { deleteFile(it) }
        }

        val entity = FieldGuidePageEntity.fromFieldGuidePage(page)
        pageDao.upsert(entity).getUpsertedId(page.id)
    }

    override suspend fun addSighting(sighting: Sighting): Long = onIO {
        sightingDao.upsert(FieldGuideSightingEntity.fromSighting(sighting)).getUpsertedId(sighting.id)
    }

    override suspend fun getSighting(id: Long): Sighting? = onIO {
        sightingDao.getSighting(id)?.toSighting()
    }

    override suspend fun deleteSighting(sighting: Sighting) = onIO {
        sightingDao.delete(FieldGuideSightingEntity.fromSighting(sighting))
    }

    /**
     * Only built-in pages (negative ids) can be hidden. Returns false otherwise.
     */
    override suspend fun setPageHidden(pageId: Long, isHidden: Boolean): Boolean = onIO {
        if (pageId >= 0) {
            return@onIO false
        }
        val hiddenPageIds = preferences.hiddenPageIds.toMutableSet()
        if (isHidden) {
            hiddenPageIds.add(pageId)
        } else {
            hiddenPageIds.remove(pageId)
        }
        preferences.hiddenPageIds = hiddenPageIds.toList()
        true
    }

}
