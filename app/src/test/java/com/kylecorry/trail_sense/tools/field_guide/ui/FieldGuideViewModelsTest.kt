package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime

class FieldGuideViewModelsTest {

    private val repo = mock<FieldGuideRepo>()

    @Test
    fun listFiltersToggleAndClear() {
        val viewModel = FieldGuideListViewModel(repo, {}, { it.name }, { it.name })
        assertFalse(viewModel.isFiltering)

        viewModel.toggleTag(FieldGuidePageTag.Bird)
        assertEquals(FieldGuidePageTag.Bird, viewModel.tagFilter)
        assertTrue(viewModel.isFiltering)

        viewModel.toggleTag(FieldGuidePageTag.Bird)
        assertNull(viewModel.tagFilter)

        viewModel.filter = "oak"
        viewModel.tagFilter = FieldGuidePageTag.Plant
        viewModel.clearFilters()
        assertFalse(viewModel.isFiltering)
        assertEquals("", viewModel.filter)
    }

    @Test
    fun newSightingChangesOnlyWhenEdited() {
        val now = LocalDateTime.of(2026, 11, 5, 12, 0)
        val viewModel = CreateFieldGuideSightingViewModel(
            Route.CreateFieldGuideSighting(pageId = 1L),
            repo,
            DistanceUnits.Meters,
            now
        )
        assertFalse(viewModel.hasChanges)

        viewModel.location = Coordinate(1.0, 2.0)
        assertTrue(viewModel.hasChanges)

        viewModel.location = null
        viewModel.showOnMap = false
        assertTrue(viewModel.hasChanges)
    }
}
