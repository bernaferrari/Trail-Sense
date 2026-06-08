package com.kylecorry.trail_sense.shared

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.AndromedaFragment
import com.kylecorry.andromeda.fragments.dismissOnPause
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.andromeda.pickers.material.MaterialPickers
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.andromeda_temp.dismissOnPause
import com.kylecorry.trail_sense.shared.camera.PhotoImportBottomSheetFragment
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.ColorPickerContent
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.DurationInput
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.shared.views.PressureInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.TrailSenseTextView
import com.kylecorry.trail_sense.tools.qr.ui.ScanQRBottomSheet
import com.kylecorry.trail_sense.tools.qr.ui.ViewQRBottomSheet
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CustomUiUtils {
    @ColorInt
    fun getQualityColor(quality: Quality): Int {
        return when (quality) {
            Quality.Poor -> AppColor.Red.color
            Quality.Unknown -> AppColor.Gray.color
            Quality.Moderate -> AppColor.Yellow.color
            Quality.Good -> AppColor.Green.color
        }
    }

    fun pickDistance(
        context: Context,
        units: List<DistanceUnits>,
        default: Distance? = null,
        title: String,
        showFeetAndInches: Boolean = false,
        allowNegative: Boolean = false,
        hint: String = context.getString(R.string.distance),
        description: String? = null,
        onDistancePick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        val view = createPromptContainer(context)
        var distance: Distance? = default
        val distanceInput = DistanceInputView(context).apply {
            id = R.id.prompt_distance
        }
        view.addPromptContent(distanceInput)
        distanceInput.defaultHint = hint
        distanceInput.setOnValueChangeListener {
            distance = it
        }
        distanceInput.units = units
        distanceInput.value = default
        if (default == null) {
            distanceInput.unit = units.firstOrNull()
        }

        distanceInput.showFeetAndInches = showFeetAndInches
        distanceInput.allowNegative = allowNegative

        Alerts.dialog(
            context,
            title,
            content = description,
            contentView = view
        ) { cancelled ->
            if (cancelled) {
                onDistancePick.invoke(null, true)
            } else {
                onDistancePick.invoke(distance, false)
            }
        }
    }

    fun pickElevation(
        context: Context,
        default: Distance? = null,
        title: String,
        hint: String = context.getString(R.string.elevation),
        onElevationPick: (distance: Distance?, cancelled: Boolean) -> Unit
    ) {
        val view = createPromptContainer(context)
        var elevation: Distance? = default
        val elevationInputView = ElevationInputView(context).apply {
            id = R.id.prompt_elevation
        }
        view.addPromptContent(elevationInputView)
        elevationInputView.hint = hint
        elevationInputView.setOnElevationChangeListener {
            elevation = it
        }
        elevationInputView.elevation = default

        Alerts.dialog(
            context,
            title,
            contentView = view
        ) { cancelled ->
            if (cancelled) {
                onElevationPick.invoke(null, true)
                elevationInputView.pause()
            } else {
                onElevationPick.invoke(elevation, false)
            }
        }
    }

    fun pickColor(
        context: Context,
        default: AppColor? = null,
        title: String,
        onColorPick: (color: AppColor?) -> Unit
    ) {
        val view = createPromptContainer(context)
        var color = default
        val colorPicker = ComposeView(context).apply {
            id = R.id.prompt_color_picker
            setContent {
                TrailSenseTheme {
                    var selectedColor by remember { mutableStateOf(default) }
                    ColorPickerContent(
                        selectedColor = selectedColor,
                        onColorSelected = {
                            selectedColor = it
                            color = it
                        }
                    )
                }
            }
        }
        view.addPromptContent(colorPicker)

        Alerts.dialog(
            context,
            title,
            contentView = view
        ) { cancelled ->
            if (cancelled) {
                onColorPick.invoke(null)
            } else {
                onColorPick.invoke(color)
            }
        }
    }

    fun pickDuration(
        context: Context,
        default: Duration? = null,
        title: String,
        message: String? = null,
        hint: String? = null,
        showSeconds: Boolean = false,
        onDurationPick: (duration: Duration?) -> Unit
    ) {
        var duration: Duration? = default
        val view = ComposeView(context).apply {
            id = R.id.prompt_duration
            setContent {
                TrailSenseTheme {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (!message.isNullOrBlank()) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        DurationInput(
                            duration = default,
                            onDurationChange = {
                                duration = it
                            },
                            hint = hint?.takeIf { it.isNotBlank() }
                                ?: context.getString(R.string.duration),
                            showSeconds = showSeconds,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Alerts.dialog(
            context,
            title,
            contentView = view
        ) { cancelled ->
            if (cancelled) {
                onDurationPick.invoke(null)
            } else {
                onDurationPick.invoke(duration)
            }
        }
    }

    fun pickDate(
        context: Context,
        date: LocalDate = LocalDate.now(),
        dayViewDecorator: AndromedaDayViewDecorator? = null,
        onDatePick: (date: LocalDate?) -> Unit
    ) {
        if (context is FragmentActivity) {
            MaterialPickers.date(context.supportFragmentManager, date, dayViewDecorator, onDatePick)
        } else {
            Pickers.date(context, date, onDatePick)
        }
    }

    fun pickTime(
        context: Context,
        use24Hours: Boolean,
        default: LocalTime = LocalTime.now(),
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        if (context is FragmentActivity) {
            MaterialPickers.time(context.supportFragmentManager, use24Hours, default, onTimePick)
        } else {
            Pickers.time(context, use24Hours, default, onTimePick)
        }
    }

    fun oneTimeToast(
        context: Context,
        message: String,
        shownKey: String,
        short: Boolean = true
    ) {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        if (prefs.getBoolean(shownKey) != true) {
            Alerts.toast(context, message, short)
            prefs.putBoolean(shownKey, true)
        }
    }

    fun disclaimer(
        context: Context,
        title: String,
        message: CharSequence,
        shownKey: String,
        okText: String = context.getString(android.R.string.ok),
        cancelText: String? = context.getString(android.R.string.cancel),
        considerShownIfCancelled: Boolean = true,
        shownValue: Boolean = true,
        onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
    ) {
        val prefs = PreferencesSubsystem.getInstance(context).preferences
        if (prefs.getBoolean(shownKey) != shownValue) {
            if (considerShownIfCancelled && cancelText == null) {
                Alerts.dialog(context, title, message, okText = okText, cancelText = null, allowLinks = true) {
                    prefs.putBoolean(shownKey, shownValue)
                    onClose(it, true)
                }
            } else {
                Alerts.dialog(
                    context,
                    title,
                    message,
                    okText = okText,
                    cancelText = cancelText,
                    allowLinks = true
                ) { cancelled ->
                    val agreed = !cancelled || considerShownIfCancelled
                    if (agreed) {
                        prefs.putBoolean(shownKey, shownValue)
                    }
                    onClose(cancelled, agreed)
                }
            }
        } else {
            onClose(false, true)
        }
    }

    fun setImageColor(textView: android.widget.TextView, @ColorInt color: Int?) {
        textView.compoundDrawables.forEach {
            it?.let { Colors.setImageColor(it, color) }
        }
    }

    fun snackbar(
        fragment: Fragment,
        text: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        action: String? = null,
        onAction: () -> Unit = {}
    ): Snackbar {

        val playBar =
            fragment.view?.findViewWithTag<View>(fragment.getString(R.string.tag_play_bar))

        val view = playBar ?: fragment.requireActivity().findViewById(R.id.bottom_navigation)

        return Alerts.snackbar(
            fragment,
            view,
            text,
            duration,
            action,
            onAction
        )
    }

    fun showQR(
        fragment: Fragment,
        title: String,
        qr: String
    ): BottomSheetDialogFragment {
        val sheet = ViewQRBottomSheet(title, qr)
        sheet.show(fragment)
        sheet.dismissOnPause(fragment)
        return sheet
    }

    fun scanQR(
        fragment: Fragment,
        title: String,
        onScan: (text: String?) -> Boolean
    ): BottomSheetDialogFragment {
        val sheet = ScanQRBottomSheet(title, onScan)
        sheet.show(fragment)
        sheet.dismissOnPause(fragment)
        return sheet
    }

    suspend fun takePhoto(fragment: AndromedaFragment, size: Size? = null): Uri? =
        suspendCoroutine { cont ->
            takePhoto(fragment, size) {
                cont.resume(it)
            }
        }

    private fun takePhoto(
        fragment: AndromedaFragment,
        size: Size? = null,
        onCapture: (uri: Uri?) -> Unit
    ) {
        fragment.requestCamera {
            if (!it) {
                onCapture(null)
                return@requestCamera
            }

            val prefs = UserPreferences(fragment.requireContext()).camera

            val sheet = PhotoImportBottomSheetFragment(size, prefs.useZeroShutterLag) { uri ->
                onCapture(uri)
            }

            sheet.show(fragment)
        }
    }

    fun pickDatetime(
        context: Context,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        pickDate(context, default.toLocalDate()) { date ->
            if (date != null) {
                Pickers.time(context, use24Hours, default.toLocalTime()) { time ->
                    if (time != null) {
                        onDatetimePick(LocalDateTime.of(date, time))
                    } else {
                        onDatetimePick(null)
                    }
                }
            } else {
                onDatetimePick(null)
            }
        }
    }

    fun pickMaterialDatetime(
        fragmentManager: FragmentManager,
        use24Hours: Boolean,
        default: LocalDateTime = LocalDateTime.now(),
        onDatetimePick: (value: LocalDateTime?) -> Unit
    ) {
        MaterialPickers.date(fragmentManager, default.toLocalDate(), null) { date ->
            if (date != null) {
                MaterialPickers.time(fragmentManager, use24Hours, default.toLocalTime()) { time ->
                    if (time != null) {
                        onDatetimePick(LocalDateTime.of(date, time))
                    } else {
                        onDatetimePick(null)
                    }
                }
            } else {
                onDatetimePick(null)
            }
        }
    }

    fun MaterialPickers.time(
        fragmentManager: FragmentManager,
        use24Hours: Boolean,
        default: LocalTime = LocalTime.now(),
        onTimePick: (time: LocalTime?) -> Unit
    ) {
        val builder = MaterialTimePicker.Builder()
            .setHour(default.hour)
            .setMinute(default.minute)
            .setTimeFormat(if (use24Hours) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)

        val picker = builder.build()

        picker.addOnPositiveButtonClickListener {
            onTimePick(LocalTime.of(picker.hour, picker.minute))
        }

        picker.addOnCancelListener { onTimePick(null) }
        picker.addOnNegativeButtonClickListener { onTimePick(null) }

        picker.show(fragmentManager, picker.toString())
    }

    fun pickPressure(
        context: Context,
        title: String,
        message: CharSequence? = null,
        default: Pressure? = null,
        onPressurePick: (pressure: Pressure?) -> Unit
    ) {
        val view = PressureInputView(context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val padding = Resources.dp(context, 28f).toInt()
        view.setPadding(padding, 0, padding, 0)
        // TODO: Sort the units properly
        view.units = PressureUnits.entries
        view.value = default
        var selected = default
        view.setOnValueChangeListener {
            selected = it
        }

        Alerts.dialog(context, title, message, contentView = view) {
            if (!it) {
                onPressurePick(selected)
            } else {
                onPressurePick(null)
            }
        }
    }

    fun showChart(
        fragment: Fragment,
        title: String,
        content: CharSequence? = null,
        populateFn: (Chart) -> Unit
    ) {
        val chartView = createChartPrompt(fragment.requireContext())

        if (content != null) {
            chartView.findViewById<TrailSenseTextView>(R.id.content).text = content
        } else {
            chartView.findViewById<TrailSenseTextView>(R.id.content).isVisible = false
        }

        populateFn(chartView.findViewById(R.id.chart))
        Alerts.dialog(
            fragment.requireContext(),
            title,
            contentView = chartView,
            cancelText = null
        )
    }

    private fun createPromptContainer(context: Context): ConstraintLayout {
        val padding = Resources.dp(context, 16f).toInt()
        return ConstraintLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(padding, padding, padding, padding)
        }
    }

    private fun ConstraintLayout.addPromptContent(view: View) {
        addView(
            view,
            ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        )
    }

    private fun createChartPrompt(context: Context): ConstraintLayout {
        val padding = Resources.dp(context, 24f).toInt()
        val spacing = Resources.dp(context, 16f).toInt()
        val chartHeight = context.resources.getDimensionPixelSize(R.dimen.chart_height)
        val root = ConstraintLayout(context).apply {
            setPadding(padding, padding, padding, padding)
        }

        root.addView(
            TrailSenseTextView(context).apply {
                id = R.id.content
            },
            ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
        )

        root.addView(
            Chart(context).apply {
                id = R.id.chart
            },
            ConstraintLayout.LayoutParams(
                0,
                chartHeight
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToBottom = R.id.content
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = spacing
            }
        )

        return root
    }

    fun Resources.getPrimaryColor(context: Context): Int {
        return try {
            getAndroidColorAttr(context, androidx.appcompat.R.attr.colorPrimary)
        } catch (e: Exception) {
            e.printStackTrace()
            AppColor.Orange.color
        }
    }

    fun Resources.getColorOnPrimary(context: Context): Int {
        return try {
            getAndroidColorAttr(context, com.google.android.material.R.attr.colorOnPrimary)
        } catch (e: Exception) {
            e.printStackTrace()
            Color.WHITE
        }
    }

    fun Resources.getCardinalDirectionColor(context: Context): Int {
        return getPrimaryMarkerColor(context)
    }

    fun Resources.getPrimaryMarkerColor(context: Context): Int {
        val prefs = UserPreferences(context)
        return if (prefs.useDynamicColors && prefs.useDynamicColorsOnCompass) {
            getPrimaryColor(context)
        } else {
            AppColor.Orange.color
        }
    }

    fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

}
