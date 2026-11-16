package com.kylecorry.trail_sense.tools.packs.ui

import com.kylecorry.andromeda.csv.CSVConvert
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sharing.pickFile
import com.kylecorry.trail_sense.shared.sharing.saveExportFile
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.domain.PackItem
import com.kylecorry.trail_sense.tools.packs.infrastructure.IPackRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.max

class SharedPackingListFiles(
    private val scope: CoroutineScope,
    private val navigator: TrailSenseNavigator,
    private val effects: ScreenEffects,
    private val onImported: () -> Unit = {}
) : PackingListFiles {

    private val repo by lazy { getAppService<IPackRepo>() }

    override fun import() {
        scope.launch {
            val file = pickFile(listOf("csv")) ?: return@launch
            val items = runCatching {
                fromCsv(CSVConvert.parse(file.decodeToString()))
            }.getOrNull()

            if (items.isNullOrEmpty()) {
                effects.showMessage("Could not import packing list")
                return@launch
            }

            val packName = file.name.substringBeforeLast('.').ifBlank { "Imported Pack" }
            val packId = onIO {
                val id = repo.addPack(Pack(0, packName))
                items.forEach { item ->
                    repo.addItem(item.copy(packId = id))
                }
                id
            }

            onMain {
                onImported()
                navigator.navigate(Route.PackItemList(packId))
            }
        }
    }

    override fun export(pack: Pack) {
        scope.launch {
            val items = onIO { repo.getItemsFromPack(pack.id).first() }
            val csv = CSVConvert.toCSV(toCsv(items))
            val sanitizedName = pack.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "pack" }
            val saved = saveExportFile(
                suggestedName = sanitizedName,
                extension = "csv",
                bytes = csv.encodeToByteArray()
            )
            if (saved) {
                effects.showMessage("Packing list exported")
            }
        }
    }

    private fun toCsv(items: List<PackItem>): List<List<String>> {
        val headers = listOf(
            "Item Name",
            "Category",
            "desc",
            "qty",
            "weight",
            "unit",
            "Packed Qty",
            "Desired Qty",
            "Optional"
        )
        val rows = items.map { item ->
            listOf(
                item.name,
                "${item.category.id} - ${item.category.name}",
                "",
                max(item.amount, item.desiredAmount).toString(),
                (item.weight?.value ?: 0f).toString(),
                formatWeightUnit(item.weight?.units ?: WeightUnits.Grams),
                item.amount.toString(),
                item.desiredAmount.toString(),
                item.isOptional.toString()
            )
        }
        return listOf(headers) + rows
    }

    private fun fromCsv(data: List<List<String>>): List<PackItem> {
        if (data.size < 2) return emptyList()
        val header = data.first().map { it.lowercase().trim() }
        val nameIdx = header.indexOf("item name")
        val categoryIdx = header.indexOf("category")
        val weightIdx = header.indexOf("weight")
        val unitIdx = header.indexOf("unit")
        val qtyIdx = header.indexOf("qty")
        val packedIdx = header.indexOf("packed qty")
        val desiredIdx = header.indexOf("desired qty")
        val optionalIdx = header.indexOf("optional")

        return data.drop(1).mapNotNull { row ->
            val name = row.getOrNull(nameIdx)?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            val category = parseCategory(row.getOrNull(categoryIdx).orEmpty())
            val weightVal = row.getOrNull(weightIdx)?.toFloatOrNull()
            val weightUnit = row.getOrNull(unitIdx)?.let(::parseWeightUnit) ?: WeightUnits.Grams
            val packedQty = row.getOrNull(packedIdx)?.toDoubleCompat() ?: 0.0
            val desiredQty = row.getOrNull(desiredIdx)?.toDoubleCompat()
                ?: row.getOrNull(qtyIdx)?.toDoubleCompat()
                ?: 0.0
            val optional = parseOptional(row.getOrNull(optionalIdx))
            PackItem(
                id = 0,
                packId = 0,
                name = name,
                category = category,
                amount = packedQty,
                desiredAmount = desiredQty,
                isOptional = optional,
                weight = if (weightVal != null && weightVal > 0f) Weight.from(weightVal, weightUnit) else null
            )
        }
    }

    private fun parseCategory(value: String): ItemCategory {
        val id = value.split(" - ").firstOrNull()?.toIntOrNull()
        return ItemCategory.entries.firstOrNull { it.id == id }
            ?: ItemCategory.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
            ?: ItemCategory.Other
    }

    private fun parseWeightUnit(value: String): WeightUnits {
        return when (value.trim().lowercase()) {
            "gram", "grams", "g" -> WeightUnits.Grams
            "kilogram", "kilograms", "kg" -> WeightUnits.Kilograms
            "ounce", "ounces", "oz" -> WeightUnits.Ounces
            "pound", "pounds", "lb", "lbs" -> WeightUnits.Pounds
            else -> WeightUnits.Grams
        }
    }

    private fun formatWeightUnit(units: WeightUnits): String {
        return when (units) {
            WeightUnits.Grams -> "gram"
            WeightUnits.Kilograms -> "kilogram"
            WeightUnits.Ounces -> "ounce"
            WeightUnits.Pounds -> "pound"
            else -> "gram"
        }
    }

    private fun parseOptional(value: String?): Boolean {
        return when (value?.trim()?.lowercase()) {
            "optional", "true", "yes", "1" -> true
            else -> false
        }
    }
}
