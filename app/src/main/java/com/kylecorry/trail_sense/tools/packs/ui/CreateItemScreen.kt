@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui


import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.graphics.vector.rememberVectorPainter

import android.text.InputType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import com.kylecorry.trail_sense.shared.views.compose.UnsavedChangesBackHandler
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.andromeda.preferences.IPreferences
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryIconMapper
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryStringMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor

@Composable
fun CreateItemToolScreen(route: Route.CreatePackItem) {
    val context = LocalContext.current.applicationContext
    val navigator = LocalNavigator.current
    val viewModel = viewModel {
        CreateItemViewModel(
            route = route,
            repo = PackRepo.getInstance(context),
            preferences = DependencyRegistry.get<PreferencesSubsystem>().preferences
        )
    }
    val nameMapper = remember(context) { ItemCategoryStringMapper(context) }
    val formatService = remember(context) { FormatService.getInstance(context) }
    val categories = remember(nameMapper) {
        ItemCategory.entries.map { it to nameMapper.getString(it) }.sortedBy { it.second }
    }

    UnsavedChangesBackHandler(hasChanges = viewModel.hasChanges)
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navigator.popBackStack() }
    }

    CreateItemScreen(
        title = stringResource(if (route.editItemId == 0L) R.string.create_item_title else R.string.edit_item_title),
        name = viewModel.name,
        amount = viewModel.amountText,
        desiredAmount = viewModel.desiredAmountText,
        category = viewModel.category,
        categories = categories,
        weight = viewModel.weight,
        weightUnit = viewModel.weightUnit,
        weightUnits = formatService.sortWeightUnits(WeightUnits.entries),
        isOptional = viewModel.isOptional,
        onNameChange = { viewModel.name = it },
        onAmountChange = { viewModel.amountText = it },
        onDesiredAmountChange = { viewModel.desiredAmountText = it },
        onCategoryChange = { viewModel.category = it },
        onWeightChange = { newWeight, unit ->
            viewModel.weight = newWeight
            viewModel.weightUnit = unit
        },
        onOptionalChange = { viewModel.isOptional = it },
        onSave = viewModel::save,
        onBack = { navigator.popBackStack() }
    )
}

/**
 * The item form's state: plain Kotlin over the pack repository and preferences, no Android UI types. It loads the
 * item being edited, remembers the last category and weight unit, and reports [saved] once the item is stored.
 */
internal class CreateItemViewModel(
    private val route: Route.CreatePackItem,
    private val repo: PackRepo,
    private val preferences: IPreferences
) : ViewModel() {

    private val defaultCategory =
        ItemCategory.entries.firstOrNull { it.id == preferences.getInt(KEY_LAST_CATEGORY) } ?: ItemCategory.Other
    private val defaultWeightUnit =
        WeightUnits.entries.firstOrNull { it.id == preferences.getInt(KEY_LAST_WEIGHT_UNIT) }

    private var editingItem by mutableStateOf<PackItem?>(null)
    var name by mutableStateOf("")
    var amountText by mutableStateOf("")
    var desiredAmountText by mutableStateOf("")
    var category by mutableStateOf(defaultCategory)
    var weight by mutableStateOf<Weight?>(null)
    var weightUnit by mutableStateOf(defaultWeightUnit)
    var isOptional by mutableStateOf(false)

    private val _saved = Channel<Unit>(Channel.CONFLATED)
    /** One event per save, after the item is stored. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (route.editItemId != 0L) {
            viewModelScope.launch {
                val item = withContext(Dispatchers.IO) { repo.getItem(route.editItemId) } ?: return@launch
                editingItem = item
                name = item.name
                amountText = DecimalFormatter.format(item.amount, 4, false)
                desiredAmountText = DecimalFormatter.format(item.desiredAmount, 4, false)
                category = item.category
                weight = item.weight
                weightUnit = item.weight?.units ?: defaultWeightUnit
                isOptional = item.isOptional
            }
        }
    }

    val hasChanges: Boolean
        get() {
            val item = editingItem
            if (item == null) {
                return name.isNotBlank() || amountText.isNotBlank() || desiredAmountText.isNotBlank() ||
                    category != defaultCategory || weight != null
            }
            return name != item.name ||
                amount() != item.amount ||
                desiredAmount() != item.desiredAmount ||
                category != item.category ||
                weight != item.weight ||
                isOptional != item.isOptional
        }

    fun save() {
        val item = PackItem(
            editingItem?.id ?: 0,
            route.packId,
            name,
            category,
            amount(),
            desiredAmount(),
            weight,
            isOptional
        )
        val unit = weightUnit
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                preferences.putInt(KEY_LAST_CATEGORY, item.category.id)
                unit?.let { preferences.putInt(KEY_LAST_WEIGHT_UNIT, it.id) }
                repo.addItem(item)
            }
            _saved.send(Unit)
        }
    }

    private fun amount() = amountText.toDoubleCompat() ?: 0.0
    private fun desiredAmount() = desiredAmountText.toDoubleCompat() ?: 0.0

    private companion object {
        const val KEY_LAST_CATEGORY = "cache_packing_lists_last_category"
        const val KEY_LAST_WEIGHT_UNIT = "cache_packing_lists_last_weight_unit"
    }
}

/**
 * Create or edit an item: what it is (name, category, optional) in one group, how many in a second, its weight in a
 * third, and Save at the bottom. The category is a row that opens the category menu, not an outlined button.
 */
@Composable
private fun CreateItemScreen(
    title: String,
    name: String,
    amount: String,
    desiredAmount: String,
    category: ItemCategory,
    categories: List<Pair<ItemCategory, String>>,
    weight: Weight?,
    weightUnit: WeightUnits?,
    weightUnits: List<WeightUnits>,
    isOptional: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDesiredAmountChange: (String) -> Unit,
    onCategoryChange: (ItemCategory) -> Unit,
    onWeightChange: (Weight?, WeightUnits?) -> Unit,
    onOptionalChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val weightTitle = stringResource(R.string.weight_per_item)
    TrailSensePage(
        title = title,
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("create_item_title")
    ) {
        trailSenseSection(key = "details") {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    PackTextInput(
                        text = name,
                        label = stringResource(R.string.name),
                        inputType = InputType.TYPE_CLASS_TEXT,
                        tag = "name_edit",
                        onTextChange = onNameChange
                    )
                }
            }
            row { position ->
                CategoryInput(
                    category = category,
                    categories = categories,
                    position = position,
                    onCategoryChange = onCategoryChange
                )
            }
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.packing_list_optional),
                    checked = isOptional,
                    onCheckedChange = onOptionalChange,
                    position = position,
                    modifier = Modifier.testTag("optional_switch")
                )
            }
        }

        trailSenseSection(key = "amount") {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                        PackTextInput(
                            text = amount,
                            label = stringResource(R.string.packed_amount),
                            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                            tag = "count_edit",
                            onTextChange = onAmountChange,
                            modifier = Modifier.weight(1f)
                        )
                        PackTextInput(
                            text = desiredAmount,
                            label = stringResource(R.string.desired_amount),
                            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                            tag = "desired_amount_edit",
                            onTextChange = onDesiredAmountChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        trailSenseSection(key = "weight", title = weightTitle) {
            row { position ->
                TrailSenseGroupCard(position = position) {
                    WeightInput(
                        weight = weight,
                        selectedUnit = weightUnit,
                        units = weightUnits,
                        onWeightChange = onWeightChange
                    )
                }
            }
        }

        item(key = "save") {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                text = stringResource(R.string.save),
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_btn")
            )
        }
    }
}

@Composable
private fun PackTextInput(
    text: String,
    label: String,
    inputType: Int,
    tag: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardType = when {
        inputType and InputType.TYPE_CLASS_NUMBER == InputType.TYPE_CLASS_NUMBER -> {
            androidx.compose.ui.text.input.KeyboardType.Decimal
        }

        else -> androidx.compose.ui.text.input.KeyboardType.Text
    }
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier.testTag(tag),
        label = label,
        keyboardType = keyboardType,
        inCard = true
    )
}

@Composable
private fun CategoryInput(
    category: ItemCategory,
    categories: List<Pair<ItemCategory, String>>,
    position: TrailSenseGroupPosition,
    onCategoryChange: (ItemCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.first == category }?.second.orEmpty()
    val iconMapper = remember { ItemCategoryIconMapper() }

    Box(modifier = modifier.fillMaxWidth()) {
        PacksGroupRow(
            position = position,
            modifier = Modifier.testTag("category_spinner"),
            onClick = { isOpen = true },
            leading = { com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(rememberVectorPainter(iconMapper.getIconVector(category))) }
        ) {
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selected.isNotBlank()) {
                Text(
                    text = selected,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false }
        ) {
            categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    leadingIcon = {
                        Icon(
                            painter = rememberVectorPainter(iconMapper.getIconVector(option.first)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        isOpen = false
                        onCategoryChange(option.first)
                    }
                )
            }
        }
    }
}

@Composable
private fun WeightInput(
    weight: Weight?,
    selectedUnit: WeightUnits?,
    units: List<WeightUnits>,
    onWeightChange: (Weight?, WeightUnits?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatService = remember { FormatService.getInstance(context) }
    var unit by remember(units) {
        mutableStateOf(selectedUnit ?: weight?.units ?: units.firstOrNull())
    }
    var amountText by remember {
        mutableStateOf(formatWeightAmount(weight, unit, false))
    }
    var ouncesText by remember {
        mutableStateOf(formatWeightAmount(weight, unit, true))
    }

    fun publish(valueText: String = amountText, secondaryText: String = ouncesText, newUnit: WeightUnits? = unit) {
        onWeightChange(parseWeight(valueText, secondaryText, newUnit), newUnit)
    }

    LaunchedEffect(weight, selectedUnit) {
        val newUnit = selectedUnit ?: weight?.units ?: unit ?: units.firstOrNull()
        val currentWeight = parseWeight(amountText, ouncesText, unit)
        if (newUnit != unit) {
            unit = newUnit
        }
        if (weight != currentWeight) {
            amountText = formatWeightAmount(weight, newUnit, false)
            ouncesText = formatWeightAmount(weight, newUnit, true)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
            value = amountText,
            onValueChange = {
                amountText = it
                publish(valueText = it)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("item_weight_input"),
            // The unit picker beside the fields names the unit, so this field is always just "Weight".
            label = stringResource(R.string.weight),
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
            inCard = true
        )

        if (unit == WeightUnits.Pounds) {
            Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                value = ouncesText,
                onValueChange = {
                    ouncesText = it
                    publish(secondaryText = it)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("item_weight_ounces_input"),
                label = stringResource(R.string.ounces_weight),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                inCard = true
            )
        }

        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        Box(modifier = Modifier.weight(1f)) {
            var isOpen by remember { mutableStateOf(false) }
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                text = unit?.let { formatService.getWeightUnitName(it) }.orEmpty(),
                onClick = { isOpen = true },
                style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_weight_unit")
            )
            DropdownMenu(
                expanded = isOpen,
                onDismissRequest = { isOpen = false }
            ) {
                units.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(formatService.getWeightUnitName(option)) },
                        onClick = {
                            isOpen = false
                            unit = option
                            publish(newUnit = option)
                        }
                    )
                }
            }
        }
    }
}

private fun parseWeight(
    amountText: String,
    ouncesText: String,
    unit: WeightUnits?
): Weight? {
    unit ?: return null
    val amount = amountText.toDoubleCompat()
    val ounces = ouncesText.toDoubleCompat()
    val value = if (unit == WeightUnits.Pounds) {
        val pounds = amount ?: 0.0
        if (amount == null && ounces == null) {
            null
        } else {
            pounds + (ounces ?: 0.0) / 16.0
        }
    } else {
        amount
    } ?: return null
    return Weight.from(value.coerceAtLeast(0.0).toFloat(), unit)
}

private fun formatWeightAmount(weight: Weight?, unit: WeightUnits?, ounces: Boolean): String {
    weight ?: return ""
    if (weight.units != unit) {
        return ""
    }
    if (unit == WeightUnits.Pounds) {
        return if (ounces) {
            DecimalFormatter.format((weight.value % 1f) * 16f, 4, false)
        } else {
            DecimalFormatter.format(floor(weight.value), 4, false)
        }
    }
    return if (ounces) {
        ""
    } else {
        DecimalFormatter.format(weight.value, 4, false)
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateItemScreenPreview() {
    TrailSenseTheme {
        CreateItemScreen(
            title = "Create item",
            name = "Water bottle",
            amount = "1",
            desiredAmount = "2",
            category = ItemCategory.Hydration,
            categories = listOf(
                ItemCategory.Food to "Food",
                ItemCategory.Hydration to "Hydration",
                ItemCategory.Clothing to "Clothing"
            ),
            weight = null,
            weightUnit = WeightUnits.Pounds,
            weightUnits = WeightUnits.entries,
            isOptional = false,
            onNameChange = {},
            onAmountChange = {},
            onDesiredAmountChange = {},
            onCategoryChange = {},
            onWeightChange = { _, _ -> },
            onOptionalChange = {},
            onSave = {}
        )
    }
}
