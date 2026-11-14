@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import com.kylecorry.trail_sense.shared.time.toInstant

import com.kylecorry.trail_sense.shared.time.toLocalDateTime

import kotlin.time.Clock

import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import androidx.core.net.toUri
import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.foundation.layout.height
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toFile
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.andromeda.bitmaps.BitmapUtils
import com.kylecorry.andromeda.bitmaps.BitmapUtils.resizeExact
import com.kylecorry.andromeda.bitmaps.BitmapUtils.rotate
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.DebugCloudCommand
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLoadingIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import com.kylecorry.trail_sense.tools.clouds.domain.classification.ClassificationResult
import com.kylecorry.trail_sense.tools.clouds.domain.classification.ICloudClassifier
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.math.abs
import com.kylecorry.trail_sense.shared.dialogs.datetime

@Composable
fun CloudResultsToolScreen(route: Route.CloudResults) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        CloudResultsViewModel(
            imageUri = route.imageUri,
            repo = getAppService<CloudRepo>(),
            classifier = SoftmaxCloudClassifier { DebugCloudCommand(appContext, it).execute() },
            loadImage = { loadCloudImage(it.toUri()) },
            clearTemp = { DeleteTempFilesCommand(appContext).execute() }
        )
    }
    val formatter = remember(appContext) { FormatService.getInstance(appContext) }

    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navigator.navigateUp() }
    }

    CloudResultsScreen(
        image = remember(viewModel.image) { viewModel.image?.asImageBitmap() },
        selection = viewModel.selection,
        title = stringResource(R.string.clouds),
        subtitle = formatter.formatDateTime(
            viewModel.time,
            relative = true,
            abbreviateMonth = true
        ),
        isLoading = viewModel.isLoading,
        canSave = viewModel.selection.any { it.isSelected },
        onDateClick = {
            scope.launch {
                val current = viewModel.time.toLocalDateTime()
                val newTime = CoroutinePickers.datetime(
                    context,
                    UserPreferences(appContext).use24HourTime,
                    current
                )
                if (newTime != null) {
                    viewModel.time = newTime.toInstant()
                }
            }
        },
        onSave = viewModel::save,
        onSelectionChanged = viewModel::setSelection
    )
}

/**
 * Picking the clouds in a photo (classified, best guesses preselected) or from every cloud type when there is no
 * photo, and the time they were seen. Reports [saved] once the chosen clouds are logged.
 */
internal class CloudResultsViewModel(
    private val imageUri: String?,
    private val repo: IReadingRepo<CloudObservation>,
    private val classifier: ICloudClassifier,
    private val loadImage: suspend (String) -> Bitmap?,
    private val clearTemp: suspend () -> Unit,
    now: Instant = Clock.System.now()
) : ViewModel() {

    var image by mutableStateOf<Bitmap?>(null)
        private set
    var selection by mutableStateOf<List<CloudSelection>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var time by mutableStateOf(now)

    private val _saved = Channel<Unit>(Channel.CONFLATED)
    /** One event per save, after the readings are stored. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (imageUri != null) {
            analyze(imageUri)
        } else {
            selection = CloudGenus.entries.map { CloudSelection(it, null, false) } + listOf(
                CloudSelection(
                    null,
                    null,
                    false
                )
            )
        }
    }

    fun setSelection(genus: CloudGenus?, selected: Boolean) {
        selection = selection.map {
            if (genus == it.genus) {
                it.copy(isSelected = selected)
            } else {
                it
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val readings =
                selection.filter { it.isSelected }
                    .map { Reading(CloudObservation(0, it.genus), time) }
            readings.forEach {
                repo.add(it)
            }
            _saved.send(Unit)
        }
    }

    private fun analyze(uri: String) {
        isLoading = true
        selection = emptyList()

        viewModelScope.launch {
            if (image == null) {
                image = loadImage(uri)
                clearTemp()
            }
            val results = onDefault {
                image?.let { classifier.classify(it) }
            }
            results?.let { setResult(it) } ?: run {
                isLoading = false
            }
        }
    }

    private fun setResult(result: List<ClassificationResult<CloudGenus?>>) {
        isLoading = false
        val maxConfidence = result[0].confidence
        val threshold = if (result[0].value == null) 0.001f else 0.1f
        selection = result.map {
            CloudSelection(
                it.value,
                it.confidence,
                abs(it.confidence - maxConfidence) <= threshold
            )
        }
    }

    override fun onCleared() {
        image?.recycle()
        image = null
    }
}

private suspend fun loadCloudImage(uri: Uri): Bitmap? = onIO {
    val file = uri.toFile()
    val path = file.path
    val rotation = tryOrDefault(0) {
        val exif = ExifInterface(uri.toFile())
        exif.rotationDegrees
    }
    val full = BitmapUtils.decodeBitmapScaled(
        path,
        SoftmaxCloudClassifier.IMAGE_SIZE,
        SoftmaxCloudClassifier.IMAGE_SIZE
    ) ?: return@onIO null
    val bmp = full.resizeExact(
        SoftmaxCloudClassifier.IMAGE_SIZE,
        SoftmaxCloudClassifier.IMAGE_SIZE
    )
    full.recycle()
    val rotated = bmp.rotate(rotation.toFloat())
    bmp.recycle()
    rotated
}
