package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.tools.clouds.domain.classification.ICloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class CloudResultsViewModelTest {

    @Test
    fun manualEntryListsEveryCloudUnselected() {
        val viewModel = CloudResultsViewModel(
            imageUri = null,
            repo = mock<CloudRepo>(),
            classifier = mock<ICloudClassifier>(),
            loadImage = { null },
            clearTemp = {}
        )

        assertEquals(CloudGenus.entries.size + 1, viewModel.selection.size)
        assertTrue(viewModel.selection.none { it.isSelected })
        assertFalse(viewModel.isLoading)

        viewModel.setSelection(CloudGenus.Cumulus, true)
        assertEquals(listOf(CloudGenus.Cumulus), viewModel.selection.filter { it.isSelected }.map { it.genus })

        viewModel.setSelection(CloudGenus.Cumulus, false)
        assertTrue(viewModel.selection.none { it.isSelected })
    }
}
