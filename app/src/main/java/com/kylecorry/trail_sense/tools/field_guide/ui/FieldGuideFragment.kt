package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTagRow
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

@Composable
fun Fragment.FieldGuideContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val navController = findNavController()

    val repo = remember { FieldGuideRepo.getInstance(context) }

    var pages by remember { mutableStateOf<List<FieldGuidePage>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        pages = repo.getPages().sortedBy { it.name }
    }

    val filteredPages = remember(pages, searchQuery, selectedCategory) {
        pages.filter { page ->
            val matchesSearch = searchQuery.isBlank() ||
                page.name.contains(searchQuery, ignoreCase = true) ||
                page.description.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null ||
                page.tags.any { it.name.equals(selectedCategory, ignoreCase = true) }

            matchesSearch && matchesCategory
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TrailSenseSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search field guide..."
        )

        val categories = listOf("Plant", "Animal", "Rock", "Weather", "Other")
        TrailSenseTagRow(tags = categories)

        if (filteredPages.isEmpty()) {
            Text(
                "No pages found",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(filteredPages) { page ->
                    TrailSenseCard(
                        onClick = {
                            val bundle = Bundle().apply {
                                putLong("page_id", page.id)
                            }
                            navController.navigate(R.id.fieldGuidePageFragment, bundle)
                        }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(page.name, style = MaterialTheme.typography.titleMedium)
                            if (page.description.isNotBlank()) {
                                Text(
                                    page.description.take(120) + if (page.description.length > 120) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    FloatingActionButton(
        onClick = {
            navController.navigate(R.id.createFieldGuidePageFragment)
        },
        modifier = Modifier.padding(16.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add page")
    }
}

class FieldGuideFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        FieldGuideContent()
    }
}

        // One time setup
        useShowDisclaimer(
            getString(R.string.disclaimer_message_title),
            getString(R.string.field_guide_disclaimer),
            "field_guide_disclaimer",
            cancelText = null
        )
        useCleanupPages()

        // List
        val handlePageAction = useCallback<FieldGuidePageListItemActionType, FieldGuidePage, Unit>(
            service,
            viewPage,
            editPage,
            deletePage,
            hidePage,
            resetOnResume
        ) { action, page ->
            when (action) {
                FieldGuidePageListItemActionType.View -> viewPage(page)
                FieldGuidePageListItemActionType.Edit -> editPage(page)
                FieldGuidePageListItemActionType.Delete -> deletePage(page)
                FieldGuidePageListItemActionType.Hide -> hidePage(page)
            }
        }

        val (tagFilter, reload, clearFilters) = useFieldGuidePageList(
            listView,
            searchView,
            handlePageAction
        )
        useResumeEffect(reload, reloadKey) {
            reload()
        }

        // Handle back press
        useBackPressedCallback(clearFilters, searchView) {
            val hadFilters = clearFilters()
            searchView.query = ""
            // If the filter was not set, don't consume the event
            hadFilters
        }

        // Create button
        useClickCallback(addButtonView, createPage, tagFilter) {
            createPage(tagFilter)
        }
    }

    private fun useCleanupPages() {
        val context = useAndroidContext()
        useBackgroundEffect(context, resetOnResume) {
            FieldGuideCleanupCommand(context).execute()
        }
    }

    private fun useDeletePage(triggerReload: () -> Unit): (page: FieldGuidePage) -> Unit {
        val service = useService<FieldGuideService>()
        val formatter = useService<FieldGuideFormatService>()
        return useCallback(service, triggerReload, formatter) { page ->
            dialog(getString(R.string.delete), formatter.formatName(page)) { cancelled ->
                if (!cancelled) {
                    inBackground {
                        service.deletePage(page)
                        triggerReload()
                    }
                }
            }
        }
    }

    private fun useHidePage(triggerReload: () -> Unit): (page: FieldGuidePage) -> Unit {
        val service = useService<FieldGuideService>()
        val formatter = useService<FieldGuideFormatService>()
        return useCallback(service, triggerReload, formatter) { page ->
            val message = getString(
                R.string.hide_field_guide_page_message,
                formatter.formatName(page)
            )
            dialog(getString(R.string.hide), message) { cancelled ->
                if (!cancelled) {
                    inBackground {
                        val success = service.setPageHidden(page.id, true)
                        if (success) {
                            triggerReload()
                        }
                    }
                }
            }
        }
    }

    private fun useCreatePage(): (tag: FieldGuidePageTag?) -> Unit {
        val navController = useNavController()
        return useCallback { tag ->
            navController.navigate(
                R.id.createFieldGuidePageFragment,
                Bundle().apply {
                    putLong("classification_id", (tag?.id ?: 0L))
                }
            )
        }
    }

    private fun useEditPage(): (page: FieldGuidePage) -> Unit {
        val navController = useNavController()
        return useCallback(navController) { page ->
            navController.navigate(
                R.id.createFieldGuidePageFragment,
                Bundle().apply {
                    putLong("page_id", page.id)
                }
            )
        }
    }

    private fun useViewPage(): (page: FieldGuidePage) -> Unit {
        val navController = useNavController()
        return useCallback(navController) { page ->
            navController.navigate(
                R.id.fieldGuidePageFragment,
                Bundle().apply {
                    putLong("page_id", page.id)
                }
            )
        }
    }

}
