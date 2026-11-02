package com.kylecorry.trail_sense.tools.packs

import androidx.test.uiautomator.Direction
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.backUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.click
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.clickOk
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.hasText
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.input
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.isChecked
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.not
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.scrollUntil
import com.kylecorry.trail_sense.test_utils.AutomationLibrary.string
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.ToolTestBase
import com.kylecorry.trail_sense.test_utils.views.id
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import org.junit.Test
import kotlin.math.roundToInt

class ToolPackingListTest : ToolTestBase(Tools.PACKING_LISTS) {

    @Test
    fun verifyBasicFunctionality() {
        hasText(string(R.string.packing_lists))

        shouldHaveNoPacks()
        canCreateAPack()
        canCreateAnItem()
        canCheckAnItem()
        canEditAnItemOnClick()
        canAddASecondItem()

        // Item menu items
        canIncrementItem()
        canDecrementItem()
        canEditAnItemFromMenu()
        canDeleteAnItem()

        // Pack menu items
        canRenamePack()
        canClearPackAmounts()
        canExportPack()
        // TODO: Sort items

        // Pack list
        canShowPackList()
        canCopyPack()
        canDeletePack()
        canImportPack()
    }

    private fun shouldHaveNoPacks() {
        hasText(id("empty_text"), string(R.string.no_packing_lists))
    }

    private fun canCreateAPack() {
        createPackingList("Test Pack 1")
    }

    private fun canCreateAnItem() {
        createItem("Test Item 1", 1, 2, string(R.string.category_food), 1.1f)
        hasItem(
            "Test Item 1",
            1,
            2,
            string(R.string.category_food),
            "1.1 lb",
            false
        )
        hasText(id("total_percent_packed"), "50% packed")
        hasText(id("total_packed_weight"), "1.1 lb")
    }

    private fun canCheckAnItem() {
        click(id("item_checkbox"))
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2.2 lb",
            true
        )
        hasText(id("total_percent_packed"), "100% packed")
        hasText(id("total_packed_weight"), "2.2 lb")
    }

    private fun canEditAnItemOnClick() {
        click(id("item_title"))
        hasText(string(R.string.edit_item_title))
        hasItemForm("Test Item 1", 2, 2, string(R.string.category_food), 1.1f)
        click(id("create_btn"))
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2 lb",
            true
        )
    }

    private fun canEditAnItemFromMenu() {
        clickItemMenu(string(R.string.edit), 1)
        hasText(string(R.string.edit_item_title))
        hasItemForm("Test Item 1", 2, 2, string(R.string.category_food), 1.1f)
        click(id("create_btn"))
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2 lb",
            true,
            1
        )
    }

    private fun canAddASecondItem() {
        hasText(string(R.string.category_food))
        createItem("Test Item 2", 1, 2, string(R.string.category_clothing), 0.5f)
        hasItem(
            "Test Item 2",
            1,
            2,
            string(R.string.category_clothing),
            "0.5 lb",
            false,
            0
        )
        hasItem(
            "Test Item 1",
            2,
            2,
            string(R.string.category_food),
            "2.2 lb",
            true,
            1
        )
        hasText(id("total_percent_packed"), "75% packed")
        hasText(id("total_packed_weight"), "2.7 lb")
    }

    private fun canIncrementItem() {
        clickItemMenu(string(R.string.add), 0)
        input(string(R.string.dialog_item_amount), "1")
        clickOk()
        hasItem(
            "Test Item 2",
            2,
            2,
            string(R.string.category_clothing),
            "1 lb",
            true,
            0
        )
    }

    private fun canDecrementItem() {
        clickItemMenu(string(R.string.subtract), 0)
        input(string(R.string.dialog_item_amount), "2")
        clickOk()
        hasItem(
            "Test Item 2",
            0,
            2,
            string(R.string.category_clothing),
            "0 lb",
            false,
            0
        )
    }

    private fun canDeleteAnItem() {
        clickItemMenu(string(R.string.delete), 0)
        hasText(string(R.string.delete_packing_list_item))
        hasText("Test Item 2")
        click(string(android.R.string.cancel))
        hasItem(
            "Test Item 2",
            0,
            2,
            string(R.string.category_clothing),
            "0 lb",
            false,
            0
        )

        clickItemMenu(string(R.string.delete), 0)
        clickOk()
        not {
            hasItem(
                "Test Item 2",
                0,
                2,
                string(R.string.category_clothing),
                "0 lb",
                false,
                0
            )
        }
    }

    private fun canRenamePack() {
        clickToolbarMenuItem(string(R.string.rename))
        input("Test Pack 1", "Test Pack 2")
        clickOk()
        hasText("Test Pack 2")
    }

    private fun canClearPackAmounts() {
        clickToolbarMenuItem(string(R.string.clear_amounts))
        clickOk()
        hasItem(
            "Test Item 1",
            0,
            2,
            string(R.string.category_food),
            "0 lb",
            false
        )
    }

    private fun canExportPack() {
        clickToolbarMenuItem(string(R.string.export))
        // TODO: Figure out how to click save
//            viewWithText("SAVE").click()
        // Pressing back is needed sometimes to close the drive selector
        backUntil {
            hasText("test-pack-2.csv")
        }
        backUntil {
            hasText("Test Pack 2")
        }
    }

    private fun canShowPackList() {
        waitFor {
            TestUtils.back()
        }
        hasText(string(R.string.packing_lists))
        hasText(id("pack_list_item_title"), "Test Pack 2")
    }

    private fun canCopyPack() {
        click(id("pack_list_item_menu"))
        click(string(android.R.string.copy))
        input(string(R.string.name), "Copied Pack")
        clickOk()
        hasText("Copied Pack")
        hasItem(
            "Test Item 1",
            0,
            2,
            string(R.string.category_food),
            "0 lb",
            false
        )
    }

    private fun canDeletePack() {
        clickToolbarMenuItem(string(R.string.delete))
        clickOk()
        hasText(string(R.string.packing_lists))
        not {
            hasText(id("pack_list_item_title"), "Copied Pack")
        }
    }

    private fun canImportPack() {
        click(id("add_btn"))
        click(string(R.string.import_packing_list))
        // TODO: Actually import a pack and verify it
    }

    // HELPERS
    private fun hasItemForm(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: String,
        weight: Float
    ) {
        hasText(id("name_edit")) { it.startsWith(name) }
        hasText(id("count_edit")) { it.startsWith(amount.toString()) }
        hasText(id("desired_amount_edit")) { it.startsWith(desiredAmount.toString()) }
        hasText(id("category_spinner")) { it.startsWith(category) }

        if (weight % 1 != 0f){
            val pounds = (weight - weight % 1).roundToInt()
            val ounces = (weight - pounds).roundPlaces(1) * 16
            hasText(id("item_weight_input")){
                it.startsWith(pounds.toString())
            }
            hasText(id("item_weight_ounces_input")){
                it.startsWith(ounces.toString())
            }
        } else {
            hasText(id("item_weight_input")) {
                it.startsWith(weight.toString())
            }
        }
    }

    private fun hasItem(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: String,
        weight: String,
        isChecked: Boolean,
        index: Int = 0
    ) {
        hasText(id("item_title", index), name)
        hasText(id("item_category", index), category)
        hasText(id("item_count", index), "$amount / $desiredAmount")
        hasText(id("item_weight", index), weight)
        isChecked(id("item_checkbox", index), isChecked)
    }

    private fun clickToolbarMenuItem(label: String) {
        click(id("inventory_menu_btn"))
        click(label)
    }

    private fun clickItemMenu(label: String, index: Int = 0) {
        click(id("item_menu", index))
        click(label)
    }

    private fun createItem(
        name: String,
        amount: Int,
        desiredAmount: Int,
        category: String,
        weight: Float
    ) {
        click(id("add_btn"))
        hasText(string(R.string.create_item_title))

        input(id("name_edit")(), name)
        input(id("count_edit")(), amount.toString())
        input(id("desired_amount_edit")(), desiredAmount.toString())
        click(id("category_spinner"))

        scrollUntil(direction = Direction.UP) {
            click(category)
        }
        clickOk()
        input(id("item_weight_input")(), weight.toString())
        click(id("create_btn"))
    }

    private fun createPackingList(name: String) {
        // Create a new packing list
        click(id("add_btn"))
        click(string(R.string.new_packing_list))
        input(string(R.string.name), name)
        clickOk()

        // Verify the new pack is created
        hasText(name)
        hasText(id("inventory_empty_text"), string(R.string.inventory_empty_text))
    }
}
