package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePageTag
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideCleanupCommand
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FieldGuideFragment : Fragment() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private var pages by mutableStateOf(emptyList<FieldGuidePage>())
    private var filter by mutableStateOf("")
    private var tagFilter by mutableStateOf<FieldGuidePageTag?>(null)
    private var backCallback: OnBackPressedCallback? = null

    private val headingTags = listOf(
        FieldGuidePageTag.Plant,
        FieldGuidePageTag.Fungus,
        FieldGuidePageTag.Animal,
        FieldGuidePageTag.Mammal,
        FieldGuidePageTag.Bird,
        FieldGuidePageTag.Reptile,
        FieldGuidePageTag.Amphibian,
        FieldGuidePageTag.Fish,
        FieldGuidePageTag.Invertebrate,
        FieldGuidePageTag.Rock,
        FieldGuidePageTag.Weather,
        FieldGuidePageTag.Other
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    FieldGuideScreen(
                        query = filter,
                        items = getListItems(),
                        onQueryChange = { filter = it },
                        onCreate = { createPage(tagFilter) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.disclaimer_message_title),
            getString(R.string.field_guide_disclaimer),
            "field_guide_disclaimer",
            cancelText = null
        )
        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (tagFilter != null || filter.isNotBlank()) {
                    tagFilter = null
                    filter = ""
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback!!)
    }

    override fun onResume() {
        super.onResume()
        reloadPages()
    }

    private fun reloadPages() {
        viewLifecycleOwner.lifecycleScope.launch {
            pages = withContext(Dispatchers.IO) {
                FieldGuideCleanupCommand(requireContext()).execute()
                repo.getAllPages().sortedBy { it.name }
            }
        }
    }

    private fun getListItems(): FieldGuideListItems {
        val filteredPages = getFilteredPages()
        return if (tagFilter == null && filter.isBlank()) {
            val mapper = FieldGuidePageTagListItemMapper(requireContext()) {
                tagFilter = it
            }
            FieldGuideListItems(
                headingTags.map { tag ->
                    tag to filteredPages.count { it.tags.contains(tag) }
                },
                mapper
            )
        } else {
            val mapper = FieldGuidePageListItemMapper(
                requireContext(),
                this
            ) { action, page ->
                when (action) {
                    FieldGuidePageListItemActionType.View -> viewPage(page)
                    FieldGuidePageListItemActionType.Edit -> editPage(page)
                    FieldGuidePageListItemActionType.Delete -> deletePage(page)
                }
            }
            FieldGuideListItems(filteredPages, mapper)
        }
    }

    private fun getFilteredPages(): List<FieldGuidePage> {
        val mapper = FieldGuideTagNameMapper(requireContext())
        return TextUtils.search(filter, pages) { page ->
            listOf(
                page.name,
                page.notes ?: "",
                page.tags.joinToString { mapper.getName(it) }
            )
        }.filter {
            tagFilter == null || it.tags.contains(tagFilter)
        }
    }

    private fun createPage(tag: FieldGuidePageTag?) {
        findNavController().navigate(
            R.id.createFieldGuidePageFragment,
            Bundle().apply {
                putLong("classification_id", tag?.id ?: 0L)
            }
        )
    }

    private fun editPage(page: FieldGuidePage) {
        findNavController().navigate(
            R.id.createFieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", page.id)
            }
        )
    }

    private fun viewPage(page: FieldGuidePage) {
        findNavController().navigate(
            R.id.fieldGuidePageFragment,
            Bundle().apply {
                putLong("page_id", page.id)
            }
        )
    }

    private fun deletePage(page: FieldGuidePage) {
        dialog(getString(R.string.delete), page.name) { cancelled ->
            if (!cancelled) {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        repo.delete(page)
                    }
                    reloadPages()
                }
            }
        }
    }
}

@Composable
private fun FieldGuideScreen(
    query: String,
    items: FieldGuideListItems,
    onQueryChange: (String) -> Unit,
    onCreate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                FieldGuideSearch(
                    query = query,
                    onQueryChange = onQueryChange
                )
                FieldGuideList(
                    items = items,
                    modifier = Modifier.fillMaxSize()
                )
            }
            FloatingActionButton(
                onClick = onCreate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun FieldGuideSearch(
    query: String,
    onQueryChange: (String) -> Unit
) {
    AndroidView(
        modifier = Modifier.testTag("search"),
        factory = { context ->
            SearchView(context, null).apply {
                id = R.id.search
                val padding = Resources.dp(context, 16f).toInt()
                setPadding(padding, padding, padding, padding)
            }
        },
        update = {
            it.setOnSearchListener(onQueryChange)
            if (it.query != query) {
                it.query = query
                it.setCursorPosition(query.length)
            }
        }
    )
}

@Composable
private fun FieldGuideList(
    items: FieldGuideListItems,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.testTag("list"),
        factory = { context ->
            FrameLayout(context).apply {
                val list = AndromedaListView(context, null).apply {
                    id = R.id.list
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val empty = TextView(context).apply {
                    text = context.getString(R.string.no_pages)
                    gravity = Gravity.CENTER
                    visibility = View.GONE
                }
                list.emptyView = empty
                addView(list)
                addView(
                    empty,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER
                    )
                )
            }
        },
        update = { container ->
            val list = container.findViewById<AndromedaListView>(R.id.list)
            @Suppress("UNCHECKED_CAST")
            when (items.mapper) {
                is FieldGuidePageTagListItemMapper -> list.setItems(
                    items.items as List<Pair<FieldGuidePageTag, Int>>,
                    items.mapper
                )

                is FieldGuidePageListItemMapper -> list.setItems(
                    items.items as List<FieldGuidePage>,
                    items.mapper
                )
            }
        }
    )
}

private data class FieldGuideListItems(
    val items: List<Any>,
    val mapper: Any
)
