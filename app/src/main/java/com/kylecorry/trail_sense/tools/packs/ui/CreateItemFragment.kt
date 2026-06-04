package com.kylecorry.trail_sense.tools.packs.ui

import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.kylecorry.trail_sense.shared.views.WeightInputView
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.PackRepo
import com.kylecorry.trail_sense.tools.packs.ui.mappers.ItemCategoryStringMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateItemFragment : Fragment() {

    private val itemRepo by lazy { PackRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { AppServiceRegistry.get<PreferencesSubsystem>() }

    private val defaultCategory by lazy {
        val lastCategoryId = prefs.preferences.getInt(KEY_LAST_CATEGORY)
        ItemCategory.entries.firstOrNull { it.id == lastCategoryId } ?: ItemCategory.Other
    }

    private val defaultWeightUnit by lazy {
        val lastUnitId = prefs.preferences.getInt(KEY_LAST_WEIGHT_UNIT)
        WeightUnits.entries.firstOrNull { it.id == lastUnitId }
    }

    private val nameMapper by lazy { ItemCategoryStringMapper(requireContext()) }

    private val sortedCategories by lazy {
        ItemCategory.entries.sortedBy { nameMapper.getString(it) }
    }

    private var editingItem by mutableStateOf<PackItem?>(null)
    private var name by mutableStateOf("")
    private var amountText by mutableStateOf("")
    private var desiredAmountText by mutableStateOf("")
    private var category by mutableStateOf<ItemCategory?>(null)
    private var weight by mutableStateOf<Weight?>(null)
    private var selectedWeightUnit by mutableStateOf<WeightUnits?>(null)
    private var isOptional by mutableStateOf(false)

    private var packId: Long = 0
    private var itemId: Long = 0

    private val selectedCategory: ItemCategory
        get() = category ?: defaultCategory

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getLong("edit_item_id") ?: 0L
        packId = arguments?.getLong("pack_id") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    CreateItemScreen(
                        title = stringResource(
                            if (itemId == 0L) R.string.create_item_title else R.string.edit_item_title
                        ),
                        name = name,
                        amount = amountText,
                        desiredAmount = desiredAmountText,
                        category = selectedCategory,
                        categories = sortedCategories.map { it to nameMapper.getString(it) },
                        weight = weight,
                        weightUnit = selectedWeightUnit,
                        weightUnits = formatService.sortWeightUnits(WeightUnits.entries),
                        isOptional = isOptional,
                        onNameChange = { name = it },
                        onAmountChange = { amountText = it },
                        onDesiredAmountChange = { desiredAmountText = it },
                        onCategoryChange = { category = it },
                        onWeightChange = { newWeight, unit ->
                            weight = newWeight
                            selectedWeightUnit = unit
                        },
                        onOptionalChange = { isOptional = it },
                        onSave = ::saveItem
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: android.os.Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        category = defaultCategory
        selectedWeightUnit = defaultWeightUnit
        promptIfUnsavedChanges(::hasChanges)

        if (itemId != 0L) {
            loadEditingItem(itemId)
        }
    }

    private fun loadEditingItem(id: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
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

    private fun saveItem() {
        val selectedUnit = selectedWeightUnit
        val itemCategory = selectedCategory
        val currentWeight = weight
        val currentName = name
        val amount = amountText.toDoubleCompat() ?: 0.0
        val desiredAmount = desiredAmountText.toDoubleCompat() ?: 0.0
        val optional = isOptional

        viewLifecycleOwner.lifecycleScope.launch {
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

            findNavController().popBackStack()
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
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            WeightInputView(context).apply {
                id = R.id.item_weight_input
                this.units = units
                showPoundsAndOunces = true
                selectedUnit?.let { unit = it }
                setOnValueChangeListener {
                    onWeightChange(it, unit)
                }
            }
        },
        update = { input ->
            input.units = units
            input.showPoundsAndOunces = true
            val fallbackUnit = selectedUnit ?: input.unit ?: units.firstOrNull()

            if (weight == null) {
                if (input.value != null) {
                    input.value = null
                }
                if (fallbackUnit != null && input.unit != fallbackUnit) {
                    input.unit = fallbackUnit
                }
            } else if (input.value != weight) {
                input.value = weight
            }
        }
    )
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
