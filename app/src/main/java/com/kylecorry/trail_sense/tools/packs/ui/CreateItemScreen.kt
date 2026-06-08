@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.packs.ui

import android.text.InputType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryStringMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor

@Composable
fun CreateItemToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { CreateItemController(fragment) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    CreateItemScreen(
        title = stringResource(
            if (controller.itemId == 0L) R.string.create_item_title else R.string.edit_item_title
        ),
        name = controller.name,
        amount = controller.amountText,
        desiredAmount = controller.desiredAmountText,
        category = controller.selectedCategory,
        categories = controller.sortedCategories.map { it to controller.nameMapper.getString(it) },
        weight = controller.weight,
        weightUnit = controller.selectedWeightUnit,
        weightUnits = controller.formatService.sortWeightUnits(WeightUnits.entries),
        isOptional = controller.isOptional,
        onNameChange = { controller.name = it },
        onAmountChange = { controller.amountText = it },
        onDesiredAmountChange = { controller.desiredAmountText = it },
        onCategoryChange = { controller.category = it },
        onWeightChange = { newWeight, unit ->
            controller.weight = newWeight
            controller.selectedWeightUnit = unit
        },
        onOptionalChange = { controller.isOptional = it },
        onSave = controller::saveItem
    )
}

private class CreateItemController(private val fragment: Fragment) {

    private val itemRepo by lazy { PackRepo.getInstance(fragment.requireContext()) }
    val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val prefs by lazy { AppServiceRegistry.get<PreferencesSubsystem>() }
    private var backCallback: androidx.activity.OnBackPressedCallback? = null
    private var isInitialized = false

    private val defaultCategory by lazy {
        val lastCategoryId = prefs.preferences.getInt(KEY_LAST_CATEGORY)
        ItemCategory.entries.firstOrNull { it.id == lastCategoryId } ?: ItemCategory.Other
    }

    private val defaultWeightUnit by lazy {
        val lastUnitId = prefs.preferences.getInt(KEY_LAST_WEIGHT_UNIT)
        WeightUnits.entries.firstOrNull { it.id == lastUnitId }
    }

    val nameMapper by lazy { ItemCategoryStringMapper(fragment.requireContext()) }

    val sortedCategories by lazy {
        ItemCategory.entries.sortedBy { nameMapper.getString(it) }
    }

    private var editingItem by mutableStateOf<PackItem?>(null)
    var name by mutableStateOf("")
    var amountText by mutableStateOf("")
    var desiredAmountText by mutableStateOf("")
    var category by mutableStateOf<ItemCategory?>(null)
    var weight by mutableStateOf<Weight?>(null)
    var selectedWeightUnit by mutableStateOf<WeightUnits?>(null)
    var isOptional by mutableStateOf(false)

    private var packId: Long = 0
    var itemId: Long = 0
        private set

    val selectedCategory: ItemCategory
        get() = category ?: defaultCategory

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        itemId = fragment.arguments?.getLong("edit_item_id") ?: 0L
        packId = fragment.arguments?.getLong("pack_id") ?: 0L
        category = defaultCategory
        selectedWeightUnit = defaultWeightUnit
        backCallback = fragment.promptIfUnsavedChanges(::hasChanges)

        if (itemId != 0L) {
            loadEditingItem(itemId)
        }
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
    }

    private fun loadEditingItem(id: Long) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val item = withContext(Dispatchers.IO) {
                itemRepo.getItem(id)
            } ?: return@launch

            editingItem = item
            name = item.name
            amountText = DecimalFormatter.format(item.amount, 4, false)
            desiredAmountText = DecimalFormatter.format(item.desiredAmount, 4, false)
            category = item.category
            weight = item.weight
            selectedWeightUnit = item.weight?.units ?: defaultWeightUnit
            isOptional = item.isOptional
        }
    }

    fun saveItem() {
        val selectedUnit = selectedWeightUnit
        val itemCategory = selectedCategory
        val currentWeight = weight
        val currentName = name
        val amount = amountText.toDoubleCompat() ?: 0.0
        val desiredAmount = desiredAmountText.toDoubleCompat() ?: 0.0
        val optional = isOptional

        fragment.viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                prefs.preferences.putInt(KEY_LAST_CATEGORY, itemCategory.id)
                selectedUnit?.let { prefs.preferences.putInt(KEY_LAST_WEIGHT_UNIT, it.id) }

                itemRepo.addItem(
                    PackItem(
                        editingItem?.id ?: 0,
                        packId,
                        currentName,
                        itemCategory,
                        amount,
                        desiredAmount,
                        currentWeight,
                        optional
                    )
                )
            }

            backCallback?.remove()
            fragment.findNavController().popBackStack()
        }
    }

    private fun hasChanges(): Boolean {
        val itemCategory = selectedCategory
        val amount = amountText.toDoubleCompat() ?: 0.0
        val desiredAmount = desiredAmountText.toDoubleCompat() ?: 0.0

        return !nothingEntered() && (
            name != editingItem?.name ||
                amount != editingItem?.amount ||
                desiredAmount != editingItem?.desiredAmount ||
                itemCategory != editingItem?.category ||
                weight != editingItem?.weight ||
                isOptional != editingItem?.isOptional
            )
    }

    private fun nothingEntered(): Boolean {
        if (editingItem != null) {
            return false
        }

        return name.isBlank() &&
            amountText.isBlank() &&
            desiredAmountText.isBlank() &&
            selectedCategory == defaultCategory &&
            weight == null
    }

    companion object {
        private const val KEY_LAST_CATEGORY = "cache_packing_lists_last_category"
        private const val KEY_LAST_WEIGHT_UNIT = "cache_packing_lists_last_weight_unit"
    }
}

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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                CreateItemHeader(title = title)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 96.dp)
                ) {
                    PackTextInput(
                        text = name,
                        label = stringResource(R.string.name),
                        inputType = InputType.TYPE_CLASS_TEXT,
                        tag = "name_edit",
                        onTextChange = onNameChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PackTextInput(
                        text = amount,
                        label = stringResource(R.string.packed_amount),
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                        tag = "count_edit",
                        onTextChange = onAmountChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PackTextInput(
                        text = desiredAmount,
                        label = stringResource(R.string.desired_amount),
                        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                        tag = "desired_amount_edit",
                        onTextChange = onDesiredAmountChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CategoryInput(
                        category = category,
                        categories = categories,
                        onCategoryChange = onCategoryChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.weight_per_item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    WeightInput(
                        weight = weight,
                        selectedUnit = weightUnit,
                        units = weightUnits,
                        onWeightChange = onWeightChange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OptionalInput(
                        checked = isOptional,
                        onCheckedChange = onOptionalChange
                    )
                }
            }
            FloatingActionButton(
                onClick = onSave,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("create_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun CreateItemHeader(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .testTag("create_item_title"),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
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
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun CategoryInput(
    category: ItemCategory,
    categories: List<Pair<ItemCategory, String>>,
    onCategoryChange: (ItemCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by androidx.compose.runtime.remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.first == category }?.second.orEmpty()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { isOpen = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("category_spinner")
        ) {
            Text(selected.ifBlank { stringResource(R.string.category) })
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false }
        ) {
            categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
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
        OutlinedTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                publish(valueText = it)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("item_weight_input"),
            label = {
                Text(
                    if (unit == WeightUnits.Pounds) {
                        stringResource(R.string.pounds)
                    } else {
                        stringResource(R.string.weight)
                    }
                )
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
            )
        )

        if (unit == WeightUnits.Pounds) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = ouncesText,
                onValueChange = {
                    ouncesText = it
                    publish(secondaryText = it)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("item_weight_ounces_input"),
                label = { Text(stringResource(R.string.ounces_weight)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            var isOpen by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { isOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("item_weight_unit")
            ) {
                Text(unit?.let { formatService.getWeightUnitName(it) }.orEmpty())
            }
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

@Composable
private fun OptionalInput(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("optional_switch"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.packing_list_optional),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
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
