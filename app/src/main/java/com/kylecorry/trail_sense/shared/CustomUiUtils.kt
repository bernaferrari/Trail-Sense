package com.kylecorry.trail_sense.shared

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
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
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.shared.views.PressureInputView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.qr.ui.QR_SHARE_MAX_LENGTH
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
        var distance: Distance? = default
        val distanceInput = DistanceInputView(context).apply {
            id = R.id.prompt_distance
            defaultHint = hint
            setOnValueChangeListener {
                distance = it
            }
            this.units = units
            value = default
            if (default == null) {
                unit = units.firstOrNull()
            }
            this.showFeetAndInches = showFeetAndInches
            this.allowNegative = allowNegative
        }
        val view = createPromptContent(context, distanceInput)

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
        var elevation: Distance? = default
        val elevationInputView = ElevationInputView(context).apply {
            id = R.id.prompt_elevation
            this.hint = hint
            setOnElevationChangeListener {
                elevation = it
            }
            this.elevation = default
        }
        val view = createPromptContent(context, elevationInputView)

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

        val view = playBar ?: fragment.requireView()

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
    ) {
        showQR(fragment.requireContext(), fragment.findNavController(), title, qr)
    }

    fun showQR(
        context: Context,
        navController: NavController,
        title: String,
        qr: String
    ) {
        if (qr.length > QR_SHARE_MAX_LENGTH) {
            Alerts.toast(context, context.getString(R.string.qr_text_too_long))
        }
        navController.navigate(
            R.id.viewQrFragment,
            Bundle().apply {
                putString("qr_title", title)
                putString("qr_text", qr.take(QR_SHARE_MAX_LENGTH))
            }
        )
    }

    suspend fun takePhoto(
        fragment: TrailSenseComposeDestinationFragment,
        size: Size? = null
    ): Uri? =
        suspendCoroutine { cont ->
            takePhoto(fragment, size) {
                cont.resume(it)
            }
        }

    private fun takePhoto(
        fragment: TrailSenseComposeDestinationFragment,
        size: Size? = null,
        onCapture: (uri: Uri?) -> Unit
    ) {
        fragment.requestCamera {
            if (!it) {
                onCapture(null)
                return@requestCamera
            }

            val prefs = UserPreferences(fragment.requireContext()).camera
            fragment.showPhotoImport(size, prefs.useZeroShutterLag) { uri ->
                onCapture(uri)
            }
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
        var selected = default
        val pressureInput = PressureInputView(context).apply {
            // TODO: Sort the units properly
            units = PressureUnits.entries
            value = default
            setOnValueChangeListener {
                selected = it
            }
        }
        val view = createPromptContent(
            context,
            pressureInput,
            horizontalPadding = 28,
            verticalPadding = 0
        )

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
        showChart(fragment.requireContext(), title, content, populateFn)
    }

    fun showChart(
        context: Context,
        title: String,
        content: CharSequence? = null,
        populateFn: (Chart) -> Unit
    ) {
        val view = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            val context = LocalContext.current
            val chart = remember {
                Chart(context).also(populateFn)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (content != null) {
                    Text(
                        text = content.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
                AndroidView(
                    factory = { chart },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.chart_height))
                )
            }
        }
        Alerts.dialog(
            context,
            title,
            contentView = view,
            cancelText = null
        )
    }

    private fun createPromptContent(
        context: Context,
        content: View,
        horizontalPadding: Int = 16,
        verticalPadding: Int = 16
    ): View {
        return TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            AndroidView(
                factory = { content },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = horizontalPadding.dp,
                        vertical = verticalPadding.dp
                    )
            )
        }
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
