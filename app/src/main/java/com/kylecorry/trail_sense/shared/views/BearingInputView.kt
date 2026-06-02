package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.findFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.camera.SightingCompassBottomSheetFragment
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService

class BearingInputView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(getContext()) }
    private val sensors by lazy { SensorService(getContext()) }
    private val compass by lazy { sensors.getCompass() }
    private val hasCompass by lazy { sensors.hasCompass() }

    var bearing: Float? = null
        set(value) {
            field = value
            if (value == null) {
                bearingTxt.text = context.getString(R.string.direction_not_set)
                clearBtn.isVisible = false
                northReferenceBadge.isVisible = false
            } else {
                bearingTxt.text = formatter.formatDegrees(value, replace360 = true)
                clearBtn.isVisible = true
                northReferenceBadge.isVisible = true
            }
            onChange()
        }

    var trueNorth: Boolean = false
        set(value) {
            field = value
            northReferenceBadge.useTrueNorth = value
            onChange()
        }

    private var changeListener: ((bearing: Float?, isTrueNorth: Boolean) -> Unit)? = null

    private val bearingTxt: TextView
    private val compassBtn: ImageButton
    private val cameraBtn: ImageButton
    private val compassText: TextView
    private val manualEntryBtn: TextView
    private val clearBtn: ImageButton
    private val northReferenceBadge: NorthReferenceBadge

    private var cameraSheet: SightingCompassBottomSheetFragment? = null


    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val iconSize = Resources.dp(context, 32f).toInt()
        val iconPadding = Resources.dp(context, 8f).toInt()
        val smallSpacing = Resources.dp(context, 4f).toInt()

        val compassAutofillHolder = LinearLayout(context).apply {
            id = R.id.compass_autofill_holder
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        addView(
            compassAutofillHolder,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = iconPadding
            }
        )

        compassBtn = ImageButton(context).apply {
            id = R.id.compass_btn
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setImageResource(R.drawable.ic_compass_icon)
            setBackgroundResource(R.drawable.rounded_rectangle)
        }
        compassAutofillHolder.addView(
            compassBtn,
            LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
        )

        compassText = TextView(context).apply {
            id = R.id.compass_bearing
            compoundDrawablePadding = iconPadding
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
        }
        compassAutofillHolder.addView(
            compassText,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = smallSpacing
            }
        )

        val bearingContent = LinearLayout(context).apply {
            orientation = VERTICAL
        }
        addView(
            bearingContent,
            LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = iconPadding
            }
        )

        bearingTxt = TextView(context).apply {
            id = R.id.bearing
            text = context.getString(R.string.direction_not_set)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
        }
        bearingContent.addView(
            bearingTxt,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        northReferenceBadge = NorthReferenceBadge(context).apply {
            id = R.id.north_reference_badge
        }
        bearingContent.addView(
            northReferenceBadge,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        manualEntryBtn = TextView(context).apply {
            id = R.id.manual_bearing
            compoundDrawablePadding = smallSpacing
            gravity = Gravity.CENTER_VERTICAL
            text = context.getString(R.string.enter_manually)
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setTextColor(Resources.getPrimaryColor(context))
            setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_keyboard_arrow_right,
                0
            )
            compoundDrawableTintList = ColorStateList.valueOf(Resources.getPrimaryColor(context))
        }
        bearingContent.addView(
            manualEntryBtn,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        clearBtn = ImageButton(context).apply {
            id = R.id.clear_btn
            background = null
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            visibility = View.INVISIBLE
            setImageResource(R.drawable.ic_cancel)
            imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
        }
        addView(
            clearBtn,
            LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
                marginEnd = iconPadding
            }
        )

        cameraBtn = ImageButton(context).apply {
            id = R.id.camera_btn
            background = null
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            setImageResource(R.drawable.ic_camera)
            imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
        }
        addView(
            cameraBtn,
            LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
        )

        CustomUiUtils.setButtonState(compassBtn, true)

        manualEntryBtn.setOnClickListener {
            pickManualBearing()
        }

        clearBtn.setOnClickListener {
            bearing = null
            trueNorth = false
            northReferenceBadge.useTrueNorth = false
        }

        compassBtn.setOnClickListener {
            bearing = compass.rawBearing
            trueNorth = false
            northReferenceBadge.useTrueNorth = false
        }

        cameraBtn.setOnClickListener {
            val fragment = runCatching { findFragment<androidx.fragment.app.Fragment>() }
                .getOrNull() ?: return@setOnClickListener

            fragment.requestCamera { hasPermission ->
                if (hasPermission) {
                    if (cameraSheet == null) {
                        cameraSheet = SightingCompassBottomSheetFragment {
                            bearing = it
                            trueNorth = false
                            northReferenceBadge.useTrueNorth = false
                        }
                    }

                    cameraSheet?.show(fragment)
                } else {
                    fragment.alertNoCameraPermission()
                }
            }
        }

        if (!hasCompass) {
            compassAutofillHolder.isVisible = false
            cameraBtn.isVisible = false
        }
    }

    fun start() {
        stop()
        compass.start {
            val bearing = compass.rawBearing
            compassText.text = formatter.formatDegrees(bearing, replace360 = true)
            cameraSheet?.bearing = bearing
            true
        }
    }

    fun stop() {
        compass.stop(null)
        // Note: This does not dismiss the sheet - if the sheet is visible, the camera is already listening to state changes
    }

    private fun onChange() {
        changeListener?.invoke(bearing, trueNorth)
    }

    fun setOnBearingChangeListener(listener: ((bearing: Float?, isTrueNorth: Boolean) -> Unit)?) {
        changeListener = listener
    }

    private fun pickManualBearing() {
        val view = createManualBearingPicker()
        var chosenBearing = bearing
        var chosenTrueNorth = trueNorth

        val bearingInputView = view.findViewById<EditText>(R.id.bearing)
        bearingInputView.addTextChangedListener {
            val text = bearingInputView.text?.toString()
            chosenBearing = text?.toFloatOrNull()?.let { Bearing.getBearing(it) }
        }
        bearingInputView.setText(chosenBearing?.let { DecimalFormatter.format(it, 1) })

        val trueNorthSwitch = view.findViewById<MaterialSwitch>(R.id.true_north)
        trueNorthSwitch.isChecked = chosenTrueNorth
        trueNorthSwitch.setOnCheckedChangeListener { _, isChecked ->
            chosenTrueNorth = isChecked
        }

        Alerts.dialog(
            context,
            context.getString(R.string.direction),
            contentView = view
        ) { cancelled ->
            if (!cancelled) {
                bearing = chosenBearing
                trueNorth = chosenTrueNorth
                northReferenceBadge.useTrueNorth = chosenTrueNorth
            }
        }
    }

    private fun createManualBearingPicker(): View {
        val padding = Resources.dp(context, 16f).toInt()
        val root = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val bearingHolder = TextInputLayout(context).apply {
            hint = context.getString(R.string.bearing)
        }
        bearingHolder.addView(
            TextInputEditText(context).apply {
                id = R.id.bearing
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            bearingHolder,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            MaterialSwitch(context).apply {
                id = R.id.true_north
                text = context.getString(R.string.true_north)
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = padding
            }
        )

        return root
    }

}
