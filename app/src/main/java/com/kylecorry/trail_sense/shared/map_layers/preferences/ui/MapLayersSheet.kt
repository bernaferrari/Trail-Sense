package com.kylecorry.trail_sense.shared.map_layers.preferences.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.content.Context
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreference
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceType
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullDependencyPreferenceKey
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.getFullPreferenceKey
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.settings.ui.SettingsSliderRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLoadingIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

class MapLayersSheetState(
    private val mapId: String,
    private val alwaysEnabledLayerIds: List<String> = emptyList()
) {

    private var onDismissListener: (() -> Unit)? = null
    private val repo by lazy { DependencyRegistry.get<MapLayerPreferenceRepo>() }
    private var isVisible by mutableStateOf(false)

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismissListener = listener
    }

    fun show() {
        isVisible = true
    }

    fun dismiss() {
        if (!isVisible) {
            return
        }
        isVisible = false
        onDismissListener?.invoke()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(
        context: Context,
        onNavigate: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        if (!isVisible) {
            return
        }

        val markdown = remember { DependencyRegistry.get<MarkdownService>() }
        val loader = remember { DependencyRegistry.get<MapLayerLoader>() }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var layers by remember { mutableStateOf<List<MapLayerDefinition>?>(null) }
        var selectedLayerIds by remember { mutableStateOf(repo.getActiveLayerIds(mapId)) }
        var expandedLayerIds by remember { mutableStateOf(emptySet<String>()) }
        var refreshKey by remember { mutableIntStateOf(0) }

        fun refresh() {
            selectedLayerIds = repo.getActiveLayerIds(mapId)
            refreshKey++
        }

        LaunchedEffect(loader) {
            layers = onDefault {
                loader.getDefinitions().values.filter { it.isConfigurable }
            }
        }

        TrailSenseSheet(
            onDismissRequest = { dismissAndUpdateWidgets(context) },
            title = stringResource(R.string.layers),
            // The list brings its own gutters
            contentPadding = PaddingValues(0.dp)
        ) {
            MapLayersContent(
                mapId = mapId,
                layers = layers,
                selectedLayerIds = selectedLayerIds,
                expandedLayerIds = expandedLayerIds,
                alwaysEnabledLayerIds = alwaysEnabledLayerIds,
                refreshKey = refreshKey,
                listState = listState,
                onToggleLayerExpansion = { layer ->
                    expandedLayerIds = if (expandedLayerIds.contains(layer.id)) {
                        expandedLayerIds - layer.id
                    } else {
                        expandedLayerIds + layer.id
                    }
                },
                onMoveLayerUp = { layer ->
                    moveLayer(layer, moveTowardTop = true, selectedLayerIds)
                    expandedLayerIds = if (expandedLayerIds.contains(layer.id)) {
                        setOf(layer.id)
                    } else {
                        emptySet()
                    }
                    refresh()
                },
                onMoveLayerDown = { layer ->
                    moveLayer(layer, moveTowardTop = false, selectedLayerIds)
                    expandedLayerIds = if (expandedLayerIds.contains(layer.id)) {
                        setOf(layer.id)
                    } else {
                        emptySet()
                    }
                    refresh()
                },
                onPreferenceChanged = {
                    refreshKey++
                },
                onNavigate = onNavigate,
                onCopySettings = { layer ->
                    pickDestinationMaps(context, layer)
                },
                onRemoveLayer = { layer ->
                    removeLayer(context, layer, selectedLayerIds) {
                        expandedLayerIds = emptySet()
                        refresh()
                    }
                },
                onAddLayers = {
                    pickAdditionalLayers(context, layers.orEmpty(), selectedLayerIds) {
                        expandedLayerIds = emptySet()
                        refresh()
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                markdown = markdown,
                modifier = modifier.fillMaxSize()
            )
        }
    }

    fun dismissAndUpdateWidgets(context: Context) {
        val wasVisible = isVisible
        dismiss()
        if (wasVisible) {
            if (mapId == MapToolRegistration.MAP_ID) {
                Tools.triggerWidgetUpdate(context, MapToolRegistration.WIDGET_MAP)
            }
        }
    }

    private fun moveLayer(
        layer: MapLayerDefinition,
        moveTowardTop: Boolean,
        selectedLayerIds: List<String>
    ) {
        val newLayerIds = selectedLayerIds.toMutableList()
        val index = newLayerIds.indexOf(layer.id)
        val target = if (moveTowardTop) index + 1 else index - 1
        if (index == -1 || target !in newLayerIds.indices) {
            return
        }

        val temp = newLayerIds[target]
        newLayerIds[target] = layer.id
        newLayerIds[index] = temp
        repo.setActiveLayerIds(mapId, newLayerIds)
    }

    private fun pickDestinationMaps(context: Context, layer: MapLayerDefinition) {
        val otherMaps = getOtherMapIds()
        Pickers.items(
            context,
            context.getString(R.string.copy_settings_to_other_maps),
            otherMaps.mapNotNull { getMapName(context, it) },
            otherMaps.indices.toList()
        ) { indices ->
            if (indices == null || indices.isEmpty()) {
                return@items
            }

            val destinationIds = indices.map { otherMaps[it] }
            repo.copyLayerPreferences(layer.id, mapId, destinationIds)
            Alerts.toast(context, context.getString(R.string.settings_copied))
        }
    }

    private fun removeLayer(
        context: Context,
        layer: MapLayerDefinition,
        selectedLayerIds: List<String>,
        onRemoved: () -> Unit
    ) {
        Alerts.dialog(
            context,
            context.getString(R.string.remove_layer),
            layer.name
        ) { cancelled ->
            if (!cancelled) {
                val newLayerIds = selectedLayerIds.toMutableList()
                newLayerIds.remove(layer.id)
                repo.setActiveLayerIds(mapId, newLayerIds)
                repo.removeLayerPreferences(mapId, layer.id)
                onRemoved()
            }
        }
    }

    private fun pickAdditionalLayers(
        context: Context,
        layers: List<MapLayerDefinition>,
        selectedLayerIds: List<String>,
        onAdded: () -> Unit
    ) {
        val availableLayers = layers
            .sortedBy { it.name }
            .filter { !selectedLayerIds.contains(it.id) }

        Pickers.items(
            context,
            context.getString(R.string.additional_layers),
            availableLayers.map { it.name }
        ) { selection ->
            if (selection == null) {
                return@items
            }

            val newLayerIds = selection.map { availableLayers[it].id }
            newLayerIds.forEach {
                repo.removeLayerPreferences(mapId, it)
            }

            repo.setActiveLayerIds(mapId, selectedLayerIds + newLayerIds)
            onAdded()
        }
    }

    private fun getOtherMapIds(): List<String> {
        val mapIds = listOf(
            MapToolRegistration.MAP_ID,
            NavigationToolRegistration.MAP_ID,
            OfflineMapsToolRegistration.PHOTO_MAPS_ID
        )
        return mapIds.filter { it != mapId }
    }

    private fun getMapName(context: Context, mapId: String): String? {
        return when (mapId) {
            MapToolRegistration.MAP_ID -> context.getString(R.string.map)
            NavigationToolRegistration.MAP_ID -> context.getString(R.string.navigation)
            OfflineMapsToolRegistration.PHOTO_MAPS_ID -> context.getString(R.string.photo_maps)
            else -> null
        }
    }
}

@Composable
private fun MapLayersContent(
    mapId: String,
    layers: List<MapLayerDefinition>?,
    selectedLayerIds: List<String>,
    expandedLayerIds: Set<String>,
    alwaysEnabledLayerIds: List<String>,
    refreshKey: Int,
    listState: LazyListState,
    onToggleLayerExpansion: (MapLayerDefinition) -> Unit,
    onMoveLayerUp: (MapLayerDefinition) -> Unit,
    onMoveLayerDown: (MapLayerDefinition) -> Unit,
    onPreferenceChanged: () -> Unit,
    onNavigate: (Int) -> Unit,
    onCopySettings: (MapLayerDefinition) -> Unit,
    onRemoveLayer: (MapLayerDefinition) -> Unit,
    onAddLayers: () -> Unit,
    markdown: MarkdownService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeLayers = remember(layers, selectedLayerIds) {
        layers.orEmpty()
            .filter { selectedLayerIds.contains(it.id) }
            .sortedBy { selectedLayerIds.indexOf(it.id) }
            .reversed()
    }

    // The sheet supplies the "Layers" title and the surface
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(bottom = TrailSenseSpacing.xxxl)
    ) {
        if (layers == null) {
            item("loading") {
                TrailSenseLoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = TrailSenseSpacing.xxxl)
                )
            }
            return@LazyColumn
        }

        activeLayers.forEachIndexed { index, layer ->
            if (index > 0) {
                item("divider_${layer.id}") {
                    TrailSenseDivider(
                        modifier = Modifier.padding(vertical = TrailSenseSpacing.sm)
                    )
                }
            }

            val currentIndex = selectedLayerIds.indexOf(layer.id)
            val isExpanded = expandedLayerIds.contains(layer.id)
            val isAlwaysEnabled = alwaysEnabledLayerIds.contains(layer.id)
            val isLayerEnabled = isAlwaysEnabled || getBooleanPreference(
                context,
                MapLayerPreferenceRepo.getPreferenceKey(
                    mapId,
                    layer.id,
                    DefaultMapLayerDefinitions.ENABLED
                ),
                true
            )
            item("header_${layer.id}") {
                MapLayerHeader(
                    layer = layer,
                    isExpanded = isExpanded,
                    isLayerEnabled = isLayerEnabled,
                    canMoveUp = currentIndex < selectedLayerIds.size - 1,
                    canMoveDown = currentIndex > 0,
                    onToggle = { onToggleLayerExpansion(layer) },
                    onMoveUp = { onMoveLayerUp(layer) },
                    onMoveDown = { onMoveLayerDown(layer) },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.sm)
                )
            }

            if (isExpanded) {
                val preferences = DefaultMapLayerDefinitions.getBasePreferences(
                    context,
                    context.getString(R.string.visible)
                ) + layer.preferences

                preferences.forEach { preference ->
                    if (isAlwaysEnabled && preference.id == DefaultMapLayerDefinitions.ENABLED) {
                        return@forEach
                    }

                    item("preference_${layer.id}_${preference.id}_$refreshKey") {
                        MapLayerPreferenceRow(
                            mapId = mapId,
                            layerId = layer.id,
                            preference = preference,
                            enabled = isMapLayerPreferenceEnabled(
                                context = context,
                                mapId = mapId,
                                layerId = layer.id,
                                preference = preference
                            ),
                            refreshKey = refreshKey,
                            onPreferenceChanged = onPreferenceChanged,
                            onNavigate = onNavigate,
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                }

                layer.attribution?.let { attribution ->
                    item("attribution_${layer.id}") {
                        MapLayerAttributionRow(
                            attribution = attribution,
                            markdown = markdown,
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                }

                item("copy_${layer.id}") {
                    SettingsActionRow(
                        label = stringResource(R.string.copy_settings_to_other_maps),
                        onClick = { onCopySettings(layer) },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }

                if (!isAlwaysEnabled) {
                    item("remove_${layer.id}") {
                        SettingsActionRow(
                            label = stringResource(R.string.remove_layer),
                            onClick = { onRemoveLayer(layer) },
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                }
            }
        }

        if (activeLayers.isNotEmpty()) {
            item("additional_divider") {
                TrailSenseDivider(
                    modifier = Modifier.padding(vertical = TrailSenseSpacing.sm)
                )
            }
        }

        item("additional_layers") {
            TrailSenseListItem(
                title = stringResource(R.string.additional_layers),
                icon = rememberVectorPainter(TrailIcons.Add),
                onClick = onAddLayers,
                animateEntrance = false,
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
            )
            TrailSenseDivider()
        }
    }
}

@Composable
private fun MapLayerHeader(
    layer: MapLayerDefinition,
    isExpanded: Boolean,
    isLayerEnabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MapLayerHeaderIconButton(
                icon = TrailIcons.ArrowUpWidget,
                contentDescription = stringResource(R.string.move_layer_up),
                enabled = canMoveUp,
                onClick = onMoveUp
            )
            MapLayerHeaderIconButton(
                icon = TrailIcons.ArrowDownWidget,
                contentDescription = stringResource(R.string.move_layer_down),
                enabled = canMoveDown,
                onClick = onMoveDown
            )
        }

        Spacer(Modifier.width(TrailSenseSpacing.md))

        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (isLayerEnabled) 1f else 0.4f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = layer.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (layer.description != null) {
                Text(
                    text = layer.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(TrailSenseSpacing.md))

        Icon(
            painter = rememberVectorPainter(
                if (isExpanded) TrailIcons.DropDownExpanded else TrailIcons.DropDown
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MapLayerHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(if (enabled) 1f else 0.4f)
        )
    }
}

@Composable
private fun MapLayerPreferenceRow(
    mapId: String,
    layerId: String,
    preference: MapLayerPreference,
    enabled: Boolean,
    refreshKey: Int,
    onPreferenceChanged: () -> Unit,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (preference.type) {
        MapLayerPreferenceType.Label -> MapLayerLabelPreferenceRow(
            mapId = mapId,
            layerId = layerId,
            preference = preference,
            enabled = enabled,
            onNavigate = onNavigate,
            modifier = modifier
        )

        MapLayerPreferenceType.Enum -> MapLayerEnumPreferenceRow(
            mapId = mapId,
            layerId = layerId,
            preference = preference,
            enabled = enabled,
            refreshKey = refreshKey,
            onPreferenceChanged = onPreferenceChanged,
            modifier = modifier
        )

        MapLayerPreferenceType.Seekbar -> MapLayerSliderPreferenceRow(
            mapId = mapId,
            layerId = layerId,
            preference = preference,
            enabled = enabled,
            refreshKey = refreshKey,
            onPreferenceChanged = onPreferenceChanged,
            modifier = modifier
        )

        MapLayerPreferenceType.Switch -> MapLayerSwitchPreferenceRow(
            mapId = mapId,
            layerId = layerId,
            preference = preference,
            enabled = enabled,
            refreshKey = refreshKey,
            onPreferenceChanged = onPreferenceChanged,
            modifier = modifier
        )
    }
}

@Composable
private fun MapLayerLabelPreferenceRow(
    mapId: String,
    layerId: String,
    preference: MapLayerPreference,
    enabled: Boolean,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsActionRow(
        label = preference.title?.toString().orEmpty(),
        summary = preference.summary?.toString(),
        onClick = {
            preference.navActionOnClick?.let(onNavigate)
        },
        enabled = enabled,
        modifier = modifier
    )
}

@Composable
private fun MapLayerEnumPreferenceRow(
    mapId: String,
    layerId: String,
    preference: MapLayerPreference,
    enabled: Boolean,
    refreshKey: Int,
    onPreferenceChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    val key = remember(mapId, layerId, preference.id) {
        preference.getFullPreferenceKey(mapId, layerId)
    }
    val values = preference.values.orEmpty()
    var selected by remember(refreshKey, key) {
        mutableStateOf(
            prefs.getString(key)
                ?: preference.defaultValue as? String
                ?: values.firstOrNull()?.second
                ?: ""
        )
    }
    val selectedLabel = values.firstOrNull { it.second == selected }?.first ?: selected

    SettingsActionRow(
        label = preference.title?.toString().orEmpty(),
        summary = selectedLabel,
        onClick = {
            Pickers.item(
                context,
                preference.title?.toString().orEmpty(),
                values.map { it.first },
                values.indexOfFirst { it.second == selected }.coerceAtLeast(0)
            ) { index ->
                if (index != null) {
                    selected = values[index].second
                    prefs.putString(key, selected)
                    onPreferenceChanged()
                }
            }
        },
        enabled = enabled && values.isNotEmpty(),
        modifier = modifier
    )
}

@Composable
private fun MapLayerSliderPreferenceRow(
    mapId: String,
    layerId: String,
    preference: MapLayerPreference,
    enabled: Boolean,
    refreshKey: Int,
    onPreferenceChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    val key = remember(mapId, layerId, preference.id) {
        preference.getFullPreferenceKey(mapId, layerId)
    }
    val min = preference.min?.toInt() ?: 0
    val max = (preference.max?.toInt() ?: 100).coerceAtLeast(min + 1)
    var value by remember(refreshKey, key) {
        mutableIntStateOf(
            (prefs.getInt(key) ?: preference.defaultValue as? Int ?: min)
                .coerceIn(min, max)
        )
    }

    // The settings kit's slider row: the value beside the title, stored once the finger lifts.
    SettingsSliderRow(
        label = preference.title?.toString().orEmpty(),
        value = value.toFloat(),
        valueRange = min.toFloat()..max.toFloat(),
        onValueChange = {
            value = it.roundToInt().coerceIn(min, max)
            prefs.putInt(key, value)
            onPreferenceChanged()
        },
        formatValue = { it.roundToInt().coerceIn(min, max).toString() },
        enabled = enabled,
        modifier = modifier
    )
    TrailSenseDivider()
}

@Composable
private fun MapLayerSwitchPreferenceRow(
    mapId: String,
    layerId: String,
    preference: MapLayerPreference,
    enabled: Boolean,
    refreshKey: Int,
    onPreferenceChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    val key = remember(mapId, layerId, preference.id) {
        preference.getFullPreferenceKey(mapId, layerId)
    }
    var checked by remember(refreshKey, key) {
        mutableStateOf(
            prefs.getBoolean(key) ?: preference.defaultValue as? Boolean ?: true
        )
    }

    SettingsSwitchRow(
        label = preference.title?.toString().orEmpty(),
        summary = preference.summary?.toString(),
        checked = checked,
        enabled = enabled,
        onCheckedChange = {
            checked = it
            prefs.putBoolean(key, it)
            onPreferenceChanged()
        },
        modifier = modifier
    )
    TrailSenseDivider()
}

@Composable
private fun MapLayerAttributionRow(
    attribution: MapLayerAttribution,
    markdown: MarkdownService,
    modifier: Modifier = Modifier
) {
    val text = remember(attribution) {
        markdown.toMarkdown(attribution.longAttribution ?: attribution.attribution)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        Text(
            text = stringResource(R.string.attribution),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        MapLayerMarkdownText(text)
    }
    TrailSenseDivider()
}

@Composable
private fun MapLayerMarkdownText(
    text: CharSequence,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            append(text.toString())
            if (text is Spanned) {
                text.getSpans(0, text.length, URLSpan::class.java).forEach { span ->
                    val start = text.getSpanStart(span)
                    val end = text.getSpanEnd(span)
                    if (start >= 0 && end > start) {
                        addStringAnnotation(UrlAnnotationTag, span.url, start, end)
                        addStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            start,
                            end
                        )
                    }
                }
            }
        }
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier,
        onClick = { offset ->
            annotated
                .getStringAnnotations(UrlAnnotationTag, offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        }
    )
}

private fun isMapLayerPreferenceEnabled(
    context: Context,
    mapId: String,
    layerId: String,
    preference: MapLayerPreference
): Boolean {
    val dependencyKey = preference.getFullDependencyPreferenceKey(mapId, layerId) ?: return true
    return getBooleanPreference(context, dependencyKey, true)
}

private fun getBooleanPreference(
    context: Context,
    key: String,
    defaultValue: Boolean
): Boolean {
    return PreferencesSubsystem.getInstance(context).preferences.getBoolean(key) ?: defaultValue
}

private const val UrlAnnotationTag = "url"

@Preview(showBackground = true)
@Composable
private fun MapLayersContentPreview() {
    TrailSenseTheme {
        val context = LocalContext.current
        MapLayersContent(
            mapId = "map",
            layers = listOf(
                MapLayerDefinition(
                    id = "terrain",
                    name = "Terrain",
                    description = "Contour and hillshade overlays",
                    preferences = listOf(
                        MapLayerPreference(
                            id = "smooth",
                            title = "Smooth",
                            type = MapLayerPreferenceType.Switch,
                            defaultValue = true
                        ),
                        MapLayerPreference(
                            id = "color",
                            title = "Color",
                            type = MapLayerPreferenceType.Enum,
                            values = listOf("Muted" to "muted", "Vibrant" to "vibrant"),
                            defaultValue = "muted"
                        )
                    )
                )
            ),
            selectedLayerIds = listOf("terrain"),
            expandedLayerIds = setOf("terrain"),
            alwaysEnabledLayerIds = emptyList(),
            refreshKey = 0,
            listState = rememberLazyListState(),
            onToggleLayerExpansion = {},
            onMoveLayerUp = {},
            onMoveLayerDown = {},
            onPreferenceChanged = {},
            onNavigate = {},
            onCopySettings = {},
            onRemoveLayer = {},
            onAddLayers = {},
            markdown = MarkdownService(context)
        )
    }
}
