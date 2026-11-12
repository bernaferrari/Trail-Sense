package com.kylecorry.trail_sense.tools.convert.ui

import com.kylecorry.trail_sense.shared.navigation.SystemBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.shared.text.toDecimalInputOrNull
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.abs
import kotlin.math.floor

abstract class SimpleConverterPage<T : Enum<T>>(private val defaultFrom: T, private val defaultTo: T) :
    ConverterPage {

    abstract val units: List<T>

    @Composable
    override fun Content() {
        SimpleConvertScreen(
            units = units,
            defaultFrom = defaultFrom,
            defaultTo = defaultTo,
            getUnitName = ::getUnitName,
            convert = ::convertValue,
            format = ::format
        )
    }

    /** [amount] in [from], expressed in [to]. */
    abstract fun convertValue(amount: Float, from: T, to: T): Float

    /** [amount] of [unit], formatted with its symbol (e.g. "3.2808 ft"). */
    abstract fun format(amount: Float, unit: T): String

    fun convert(amount: Float, from: T, to: T): String = format(convertValue(amount, from, to), to)

    abstract fun getUnitName(unit: T): String
}

/** One converter, drawn as a plain column: the screen around it owns scrolling and gutters. */
interface ConverterPage {
    @Composable
    fun Content()
}

/**
 * Calcula's converter on the system keyboard: what is this amount in the other unit? The top row starts as the
 * input with 1 in it, so there's an answer from the start; tapping the answer makes it the input.
 */
@Composable
fun <T : Enum<T>> SimpleConvertScreen(
    units: List<T>,
    defaultFrom: T,
    defaultTo: T,
    getUnitName: (T) -> String,
    convert: (Float, T, T) -> Float,
    format: (Float, T) -> String
) {
    var input by rememberSaveable { mutableStateOf("1") }
    var top by rememberSaveable { mutableStateOf(defaultFrom) }
    var bottom by rememberSaveable { mutableStateOf(defaultTo) }
    var topActive by rememberSaveable { mutableStateOf(true) }
    // Which row's units are showing in place of the keyboard: true = top, false = bottom.
    var picking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val amount = input.toDecimalInputOrNull()
    val from = if (topActive) top else bottom
    val to = if (topActive) bottom else top
    val converted = amount?.let { convert(it, from, to) }
    val output = converted?.let { DecimalFormatter.format(it, 4, false) }.orEmpty()

    fun symbolOf(unit: T): String = symbol(format(1f, unit), DecimalFormatter.format(1f, 4, false))

    // Opening Convert doesn't pop the keyboard; it comes up once you tap a row or come back from the units.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(picking, topActive) {
        if (!settled) {
            settled = true
        } else if (picking == null) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    SystemBackHandler(enabled = picking != null) { picking = null }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
    ) {
        ConvertCard(
            onSwap = {
                val previous = top
                top = bottom
                bottom = previous
            },
            swapLabel = convertSwapLabel()
        ) {
            listOf(true, false).forEach { isTop ->
                val unit = if (isTop) top else bottom
                val isActive = topActive == isTop
                ConvertRow(
                    active = isActive,
                    picking = picking == isTop,
                    dimmed = picking != null && picking != isTop,
                    unit = getUnitName(unit),
                    onPickUnit = { picking = if (picking == isTop) null else isTop },
                    onActivate = {
                        picking = null
                        if (!isActive) {
                            // The answer becomes the input, carrying its value over.
                            input = converted?.let(::plain).orEmpty()
                            topActive = isTop
                        }
                    },
                    copyText = (if (isActive) input else output).ifEmpty { null },
                    position = com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition(
                        isFirst = isTop,
                        isLast = !isTop
                    )
                ) {
                    if (isActive) {
                        ConvertInput(
                            value = input,
                            onValueChange = { text ->
                                input = text.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
                            },
                            focusRequester = focusRequester,
                            keyboardType = KeyboardType.Decimal,
                            placeholder = "0",
                            onDone = { keyboard?.hide() }
                        )
                    } else {
                        ConvertValue(output)
                    }
                    ConvertSymbol(symbolOf(unit))
                }
            }
        }

        // The grid keeps the row it was opened for while it animates away, and ignores taps then: otherwise a tap
        // during the exit would land on the top row's units (and could swap the rows).
        val lastTarget = remember { mutableListOf(true) }
        picking?.let { lastTarget[0] = it }
        AnimatedVisibility(visible = picking != null) {
            val target = picking ?: lastTarget[0]
            ConvertUnitGrid(
                options = units.map { ConvertUnitOption(it, getUnitName(it), symbolOf(it).ifBlank { null }) },
                selected = if (target) top else bottom,
                onPick = onPick@{ unit ->
                    if (picking == null) return@onPick
                    val other = if (target) bottom else top
                    if (unit == other) {
                        // Picking the unit the other row shows just swaps them.
                        val previous = top
                        top = bottom
                        bottom = previous
                    } else if (target) {
                        top = unit
                    } else {
                        bottom = unit
                    }
                    picking = null
                }
            )
        }
    }
}

/** A value as plain, editable digits: at most 4 decimals (rounded half up), no grouping, '.' as the separator. */
internal fun plain(value: Float): String {
    if (!value.isFinite()) return ""
    val magnitude = abs(value.toDouble())
    val text = if (magnitude < 1e14) {
        val scaled = floor(magnitude * 10_000 + 0.5).toLong()
        val fraction = (scaled % 10_000).toString().padStart(4, '0').trimEnd('0')
        val whole = (scaled / 10_000).toString()
        if (fraction.isEmpty()) whole else "$whole.$fraction"
    } else {
        // A float this large has no fractional part; write out its digits instead of an exponent.
        expandExponent(magnitude.toString())
    }
    return if (value < 0 && text != "0") "-$text" else text
}

/** "1.25E15" -> "1250000000000000"; text without an exponent is returned as is. */
private fun expandExponent(text: String): String {
    val parts = text.uppercase().split('E')
    if (parts.size != 2) return text.removeSuffix(".0")
    val mantissa = parts[0]
    val exponent = parts[1].toInt()
    val whole = mantissa.substringBefore('.')
    val fraction = mantissa.substringAfter('.', "").trimEnd('0')
    val digits = whole + fraction
    val pointAt = whole.length + exponent
    return if (pointAt >= digits.length) {
        (digits + "0".repeat(pointAt - digits.length)).trimStart('0').ifEmpty { "0" }
    } else {
        digits.substring(0, pointAt).trimStart('0').ifEmpty { "0" }
    }
}

/** The unit's symbol, from a formatted amount: what's left once the number is taken out ("1 ft" -> "ft"). */
internal fun symbol(formatted: String, number: String): String {
    return if (formatted.contains(number)) formatted.replaceFirst(number, "").trim() else ""
}
