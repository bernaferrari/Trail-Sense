package com.kylecorry.trail_sense.tools.beacons.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.BundleCompat
import androidx.core.view.setPadding
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.views.BearingInputView
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.ui.form.CreateBeaconData
import com.kylecorry.trail_sense.tools.beacons.ui.form.DoesBeaconFormDataHaveChanges
import com.kylecorry.trail_sense.tools.beacons.ui.form.IsBeaconFormDataComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaceBeaconFragment : Fragment() {

    private val beaconService by lazy { BeaconService(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private val isComplete = IsBeaconFormDataComplete()
    private var hasChanges = DoesBeaconFormDataHaveChanges(CreateBeaconData.empty)

    private var editingBeaconId: Long? = null
    private var initialLocation: GeoUri? = null
    private var backCallback: OnBackPressedCallback? = null

    private var data by mutableStateOf(CreateBeaconData.empty)
    private var groupName by mutableStateOf("")

    private var coordinateView: CoordinateInputView? = null
    private var elevationView: ElevationInputView? = null
    private var bearingView: BearingInputView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("edit_beacon") ?: 0L
        val groupId = arguments?.getLong("initial_group") ?: 0L
        initialLocation = BundleCompat.getParcelable(
            arguments ?: Bundle(),
            "initial_location",
            GeoUri::class.java
        )

        editingBeaconId = if (beaconId == 0L) null else beaconId
        data = data.copy(groupId = if (groupId == 0L) null else groupId)
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PlaceBeaconScreen(
                        data = data,
                        groupName = groupName.ifBlank { stringResource(R.string.no_group) },
                        canSave = isComplete.isSatisfiedBy(data),
                        hikingDistanceUnits = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
                        onNameChange = { updateData(data.copy(name = it)) },
                        onCoordinateChange = { updateData(data.copy(coordinate = it)) },
                        onElevationChange = { updateData(data.copy(elevation = it)) },
                        onCreateAtDistanceChange = {
                            updateData(data.copy(createAtDistance = it))
                        },
                        onDistanceChange = { updateData(data.copy(distanceTo = it)) },
                        onBearingChange = { bearing, isTrueNorth ->
                            updateData(
                                data.copy(
                                    bearingTo = bearing,
                                    bearingIsTrueNorth = isTrueNorth
                                )
                            )
                        },
                        onNotesChange = { updateData(data.copy(notes = it)) },
                        onGroupClick = ::pickGroup,
                        onColorClick = ::pickColor,
                        onIconClick = ::pickIcon,
                        onSave = ::onSubmit,
                        onCoordinateCreated = {
                            coordinateView = it
                            bindLocationAndElevation()
                        },
                        onElevationCreated = {
                            elevationView = it
                            bindLocationAndElevation()
                        },
                        onBearingCreated = { bearingView = it }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backCallback = promptIfUnsavedChanges { hasChanges.isSatisfiedBy(data) }

        updateBeaconGroupName()

        editingBeaconId?.let {
            loadExistingBeacon(it)
        } ?: run {
            val defaultColor = prefs.beacons.defaultBeaconColor
            updateData(data.copy(color = defaultColor))
            hasChanges = DoesBeaconFormDataHaveChanges(
                CreateBeaconData.empty.copy(color = defaultColor)
            )
        }

        initialLocation?.let {
            updateData(CreateBeaconData.from(it).copy(groupId = data.groupId, color = data.color))
        }
    }

    override fun onResume() {
        super.onResume()
        bearingView?.start()
    }

    override fun onPause() {
        elevationView?.pause()
        coordinateView?.pause()
        bearingView?.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        coordinateView = null
        elevationView = null
        bearingView = null
        super.onDestroyView()
    }

    private fun loadExistingBeacon(beaconId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val beacon = withContext(Dispatchers.IO) {
                beaconService.getBeacon(beaconId)
            }
            beacon?.let {
                val loadedData = CreateBeaconData.from(it)
                hasChanges = DoesBeaconFormDataHaveChanges(loadedData)
                updateData(loadedData)
            }
        }
    }

    private fun updateData(newData: CreateBeaconData) {
        data = newData
        updateBeaconGroupName()
    }

    private fun bindLocationAndElevation() {
        val coordinate = coordinateView ?: return
        val elevation = elevationView ?: return

        coordinate.setOnAutoLocationClickListener {
            if (elevation.elevation == null) {
                elevation.autofill()
            }
        }

        coordinate.setOnBeaconSelectedListener {
            if (elevation.elevation == null) {
                elevation.autofill()
            }
        }
    }

    private fun pickGroup() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = BeaconPickers.pickGroup(
                requireContext(),
                initialGroup = data.groupId
            )
            if (result.first) {
                return@launch
            }
            updateData(data.copy(groupId = result.second?.id))
        }
    }

    private fun pickColor() {
        CustomUiUtils.pickColor(
            requireContext(),
            data.color,
            getString(R.string.color)
        ) {
            if (it != null) {
                updateData(data.copy(color = it))
            }
        }
    }

    private fun pickIcon() {
        CustomUiUtils.pickBeaconIcon(
            requireContext(),
            data.icon,
            getString(R.string.icon)
        ) {
            updateData(data.copy(icon = it))
        }
    }

    private fun updateBeaconGroupName() {
        val parent = data.groupId
        viewLifecycleOwner.lifecycleScope.launch {
            groupName = withContext(Dispatchers.IO) {
                if (parent == null) {
                    getString(R.string.no_group)
                } else {
                    beaconService.getGroup(parent)?.name ?: ""
                }
            }
        }
    }

    private fun onSubmit() {
        val beacon = data.toBeacon() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                beaconService.add(beacon)
            }

            backCallback?.remove()
            findNavController().navigateUp()
        }
    }
}

@Composable
private fun PlaceBeaconScreen(
    data: CreateBeaconData,
    groupName: String,
    canSave: Boolean,
    hikingDistanceUnits: List<com.kylecorry.sol.units.DistanceUnits>,
    onNameChange: (String) -> Unit,
    onCoordinateChange: (Coordinate?) -> Unit,
    onElevationChange: (Distance?) -> Unit,
    onCreateAtDistanceChange: (Boolean) -> Unit,
    onDistanceChange: (Distance?) -> Unit,
    onBearingChange: (Float?, Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onGroupClick: () -> Unit,
    onColorClick: () -> Unit,
    onIconClick: () -> Unit,
    onSave: () -> Unit,
    onCoordinateCreated: (CoordinateInputView) -> Unit,
    onElevationCreated: (ElevationInputView) -> Unit,
    onBearingCreated: (BearingInputView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceBeaconHeader(
                title = stringResource(R.string.create_beacon),
                canSave = canSave,
                onSave = onSave
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("create_beacon_scroll")
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 96.dp)
            ) {
                TextInput(
                    text = data.name,
                    label = stringResource(R.string.name),
                    viewId = R.id.beacon_name,
                    inputType = InputType.TYPE_CLASS_TEXT,
                    errorText = stringResource(R.string.beacon_invalid_name),
                    validateOnBlur = { it.isNotBlank() },
                    onTextChange = onNameChange
                )
                Spacer(modifier = Modifier.height(24.dp))
                CoordinateInput(
                    coordinate = data.coordinate,
                    onCoordinateChange = onCoordinateChange,
                    onCoordinateCreated = onCoordinateCreated
                )
                Spacer(modifier = Modifier.height(24.dp))
                ElevationInput(
                    elevation = data.elevation,
                    onElevationChange = onElevationChange,
                    onElevationCreated = onElevationCreated
                )
                Spacer(modifier = Modifier.height(24.dp))
                CreateAtDistanceSwitch(
                    checked = data.createAtDistance,
                    onCheckedChange = onCreateAtDistanceChange
                )
                if (data.createAtDistance) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DistanceInput(
                        distance = data.distanceTo,
                        units = hikingDistanceUnits,
                        onDistanceChange = onDistanceChange
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    BearingInput(
                        bearing = data.bearingTo,
                        trueNorth = data.bearingIsTrueNorth,
                        onBearingChange = onBearingChange,
                        onBearingCreated = onBearingCreated
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                PickerRow(
                    id = R.id.beacon_group_picker,
                    text = groupName,
                    icon = R.drawable.ic_beacon_group,
                    iconTint = AppColor.Gray.color,
                    onClick = onGroupClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                PickerRow(
                    id = R.id.beacon_color_picker,
                    text = stringResource(R.string.color),
                    icon = R.drawable.bubble,
                    iconTint = data.color.color,
                    onClick = onColorClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                PickerRow(
                    id = R.id.beacon_icon_picker,
                    text = stringResource(R.string.icon),
                    icon = data.icon?.icon ?: R.drawable.bubble,
                    iconTint = null,
                    onClick = onIconClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextInput(
                    text = data.notes,
                    label = stringResource(R.string.beacon_comment),
                    viewId = R.id.comment,
                    inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                    onTextChange = onNotesChange
                )
            }
        }
    }
}

@Composable
private fun PlaceBeaconHeader(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 18.dp)
                    .testTag("create_beacon_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canSave) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("create_beacon_save_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun TextInput(
    text: String,
    label: String,
    viewId: Int,
    inputType: Int,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    validateOnBlur: ((String) -> Boolean)? = null
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            TextInputLayout(context).apply {
                hint = label
                addView(
                    TextInputEditText(context).apply {
                        id = viewId
                        this.inputType = inputType
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        doAfterTextChanged {
                            onTextChange(it?.toString().orEmpty())
                        }
                        setOnFocusChangeListener { _, hasFocus ->
                            if (!hasFocus && validateOnBlur != null && !validateOnBlur(this.text?.toString().orEmpty())) {
                                error = errorText
                            } else if (!hasFocus) {
                                error = null
                            }
                        }
                    }
                )
            }
        },
        update = { holder ->
            holder.hint = label
            val editText = holder.editText
            if (editText?.text?.toString() != text) {
                editText?.setText(text)
                editText?.setSelection(editText.text?.length ?: 0)
            }
        }
    )
}

@Composable
private fun CoordinateInput(
    coordinate: Coordinate?,
    onCoordinateChange: (Coordinate?) -> Unit,
    onCoordinateCreated: (CoordinateInputView) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            CoordinateInputView(context).apply {
                id = R.id.beacon_location
                setOnCoordinateChangeListener(onCoordinateChange)
                onCoordinateCreated(this)
            }
        },
        update = {
            if (it.coordinate != coordinate) {
                it.coordinate = coordinate
            }
        }
    )
}

@Composable
private fun ElevationInput(
    elevation: Distance?,
    onElevationChange: (Distance?) -> Unit,
    onElevationCreated: (ElevationInputView) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            ElevationInputView(context).apply {
                id = R.id.beacon_elevation
                hint = context.getString(R.string.elevation)
                setOnElevationChangeListener(onElevationChange)
                onElevationCreated(this)
            }
        },
        update = {
            if (it.elevation != elevation) {
                it.elevation = elevation
            }
        }
    )
}

@Composable
private fun CreateAtDistanceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            MaterialSwitch(context).apply {
                id = R.id.create_at_distance
                text = context.getString(R.string.create_at_distance)
                isChecked = checked
                setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
            }
        },
        update = {
            if (it.isChecked != checked) {
                it.isChecked = checked
            }
        }
    )
}

@Composable
private fun DistanceInput(
    distance: Distance?,
    units: List<com.kylecorry.sol.units.DistanceUnits>,
    onDistanceChange: (Distance?) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            DistanceInputView(context).apply {
                id = R.id.distance_away
                this.units = units
                setOnValueChangeListener(onDistanceChange)
            }
        },
        update = {
            it.units = units
            if (it.value != distance) {
                it.value = distance
            }
        }
    )
}

@Composable
private fun BearingInput(
    bearing: Float?,
    trueNorth: Boolean,
    onBearingChange: (Float?, Boolean) -> Unit,
    onBearingCreated: (BearingInputView) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            BearingInputView(context).apply {
                id = R.id.bearing_to
                setOnBearingChangeListener(onBearingChange)
                onBearingCreated(this)
            }
        },
        update = {
            if (it.bearing != bearing) {
                it.bearing = bearing
            }
            if (it.trueNorth != trueNorth) {
                it.trueNorth = trueNorth
            }
        }
    )
}

@Composable
private fun PickerRow(
    id: Int,
    text: String,
    @DrawableRes icon: Int,
    iconTint: Int?,
    onClick: () -> Unit
) {
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                this.id = id
                isClickable = true
                isFocusable = true
                setPadding(Resources.dp(context, 4f).toInt())
                compoundDrawablePadding = Resources.dp(context, 8f).toInt()
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setOnClickListener { onClick() }
            }
        },
        update = { view ->
            view.text = text
            view.setCompoundDrawables(
                Resources.dp(view.context, 24f).toInt(),
                left = icon
            )
            TextViewCompat.setCompoundDrawableTintList(
                view,
                iconTint?.let { ColorStateList.valueOf(it) }
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceBeaconScreenPreview() {
    TrailSenseTheme {
        PlaceBeaconScreen(
            data = CreateBeaconData.empty.copy(name = "Camp"),
            groupName = "No group",
            canSave = false,
            hikingDistanceUnits = emptyList(),
            onNameChange = {},
            onCoordinateChange = {},
            onElevationChange = {},
            onCreateAtDistanceChange = {},
            onDistanceChange = {},
            onBearingChange = { _, _ -> },
            onNotesChange = {},
            onGroupClick = {},
            onColorClick = {},
            onIconClick = {},
            onSave = {},
            onCoordinateCreated = {},
            onElevationCreated = {},
            onBearingCreated = {}
        )
    }
}
