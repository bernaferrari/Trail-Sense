package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.sensors.ISensorSubsystem
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTopBar
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.*
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.PdfRegion
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.SharedMapFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun SharedPhotoMapScreen(mapId: Long) {
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val strings = remember { getAppService<AppStrings>() }
    val maps = remember { getAppService<MapService>() }
    val files = remember { getAppService<SharedMapFiles>() }
    val sensors = remember { getAppService<ISensorSubsystem>() }
    val scope = rememberCoroutineScope()
    var map by remember(mapId) { mutableStateOf<PhotoMap?>(null) }
    var bitmap by remember(mapId) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(mapId) { mutableStateOf(true) }
    var location by remember(mapId) { mutableStateOf<Coordinate?>(null) }
    var calibrating by remember(mapId) { mutableStateOf(false) }
    var draft by remember(mapId) { mutableStateOf(emptyList<MapCalibrationPoint>()) }
    var pickedPoint by remember(mapId) { mutableStateOf<PercentCoordinate?>(null) }
    var confirmDelete by remember(mapId) { mutableStateOf(false) }

    LaunchedEffect(mapId) {
        try {
            maps.getPhotoMap(mapId)?.let { saved ->
                map = saved
                files.openPhoto(saved)?.let { (opened, image) ->
                    map = opened
                    bitmap = image
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Retain the row and let the user decide whether to delete an unavailable map.
        } finally {
            loading = false
        }
    }

    val photo = map
    val image = bitmap

    pickedPoint?.let { point ->
        MapCoordinateDialog(
            onDismiss = { pickedPoint = null },
            onPoint = { coordinate ->
                draft = draft.takeIf { it.size < 2 }.orEmpty() + MapCalibrationPoint(coordinate, point)
                pickedPoint = null
            }
        )
    }

    if (confirmDelete && photo != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(appString(Res.string.delete)) },
            text = { Text(photo.name) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        maps.delete(photo)
                        navigator.navigateUp()
                    }
                }) { Text(appString(Res.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(appString(Res.string.back)) } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        val back = rememberTrailSenseBack()
        TrailSenseTopBar(
            title = photo?.name ?: appString(Res.string.photo_maps),
            onBack = if (calibrating) ({ calibrating = false }) else back,
            actions = {
                if (photo != null && image != null) {
                    if (calibrating) {
                        TextButton(enabled = draft.size == 2, onClick = {
                            val candidate = photo.copy(georeference = photo.georeference.copy(
                                calibrationPoints = draft,
                                rotation = 0f
                            ))
                            val error = when (MapCalibrationValidator.validate(candidate)) {
                                MapCalibrationValidationResult.Valid -> null
                                MapCalibrationValidationResult.SamePixel -> Res.string.map_calibration_error_same_pixel
                                MapCalibrationValidationResult.SameLocation -> Res.string.map_calibration_error_same_location
                                MapCalibrationValidationResult.SameImageAxis -> Res.string.map_calibration_error_same_image_axis
                                else -> Res.string.map_calibration_error_implausible_scale
                            }
                            if (error == null) scope.launch {
                                maps.add(candidate)
                                map = candidate
                                calibrating = false
                            } else effects.showMessage(strings.get(error))
                        }) { Text(appString(Res.string.done)) }
                    } else {
                        PhotoMapMenu(
                            showLocation = photo.isCalibrated,
                            onCalibrate = {
                                draft = emptyList()
                                calibrating = true
                            },
                            onLocation = {
                                scope.launch {
                                    val coordinate = sensors.getLocation()
                                    if (coordinate == Coordinate.zero && sensors.lastKnownLocationAccuracy == null) {
                                        effects.showMessage(strings.get(Res.string.shared_map_location_unavailable))
                                    } else location = coordinate
                                }
                            },
                            onDelete = { confirmDelete = true }
                        )
                    }
                } else if (!loading && photo != null) {
                    TextButton(onClick = { confirmDelete = true }) { Text(appString(Res.string.delete)) }
                }
            }
        )
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            photo == null || image == null -> Text(appString(Res.string.shared_map_unavailable), Modifier.padding(16.dp))
            else -> {
                if (calibrating) {
                    Text(
                        appString(Res.string.shared_map_calibration_hint),
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                val marker = location?.takeIf { photo.isCalibrated && !calibrating }?.let {
                    val pixel = photo.imageProjection.toPixels(it)
                    PercentCoordinate(pixel.x / image.width, pixel.y / image.height)
                }
                PhotoMapCanvas(
                    image, photo, files,
                    (if (calibrating) draft.map { it.imageLocation } else listOfNotNull(marker)),
                    if (calibrating) ({ pickedPoint = it }) else null,
                    Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PhotoMapMenu(
    showLocation: Boolean,
    onCalibrate: () -> Unit,
    onLocation: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text(appString(Res.string.map_options)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (showLocation) {
                DropdownMenuItem(text = { Text(appString(Res.string.my_location)) }, onClick = {
                    open = false
                    onLocation()
                })
            }
            DropdownMenuItem(text = { Text(appString(Res.string.calibrate)) }, onClick = {
                open = false
                onCalibrate()
            })
            DropdownMenuItem(text = { Text(appString(Res.string.delete)) }, onClick = {
                open = false
                onDelete()
            })
        }
    }
}

@Composable
private fun MapCoordinateDialog(onDismiss: () -> Unit, onPoint: (Coordinate) -> Unit) {
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    val lat = latitude.replace(',', '.').toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
    val lon = longitude.replace(',', '.').toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appString(Res.string.map_calibration)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(latitude, { latitude = it }, label = { Text(appString(Res.string.shared_map_latitude)) }, singleLine = true)
                OutlinedTextField(longitude, { longitude = it }, label = { Text(appString(Res.string.shared_map_longitude)) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = lat != null && lon != null, onClick = {
                if (lat != null && lon != null) onPoint(Coordinate(lat, lon))
            }) { Text(appString(Res.string.done)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(appString(Res.string.back)) } }
    )
}

@Composable
private fun PhotoMapCanvas(
    image: ImageBitmap,
    map: PhotoMap,
    files: SharedMapFiles,
    points: List<PercentCoordinate>,
    onPoint: ((PercentCoordinate) -> Unit)?,
    modifier: Modifier
) {
    var zoom by remember(image) { mutableFloatStateOf(1f) }
    var pan by remember(image) { mutableStateOf(Offset.Zero) }
    var viewport by remember(image) { mutableStateOf(IntSize.Zero) }
    val tiles = remember(image, map.filename) { PdfMapTiles(image) }
    val markerColor = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onPrimary
    val selectPoint by rememberUpdatedState(onPoint)
    fun imageSize(width: Float, height: Float, zoom: Float): Offset {
        val scale = minOf(width / image.width, height / image.height) * zoom
        return Offset(image.width * scale, image.height * scale)
    }
    fun topLeft(width: Float, height: Float, drawn: Offset) =
        Offset((width - drawn.x) / 2, (height - drawn.y) / 2)

    fun transform(size: IntSize, centroid: Offset, delta: Offset, factor: Float) {
        val newZoom = (zoom * factor).coerceIn(1f, 32f)
        val drawn = imageSize(size.width.toFloat(), size.height.toFloat(), newZoom)
        val center = Offset(size.width / 2f, size.height / 2f)
        val scaledPan = (pan - centroid + center) * (newZoom / zoom) + centroid - center + delta
        val maxX = ((drawn.x - size.width) / 2).coerceAtLeast(0f)
        val maxY = ((drawn.y - size.height) / 2).coerceAtLeast(0f)
        pan = Offset(scaledPan.x.coerceIn(-maxX, maxX), scaledPan.y.coerceIn(-maxY, maxY))
        zoom = newZoom
    }

    LaunchedEffect(tiles, files) { tiles.load(files, map) }
    LaunchedEffect(tiles, viewport, zoom, pan) {
        if (viewport.width > 0 && viewport.height > 0) {
            val width = viewport.width.toFloat()
            val height = viewport.height.toFloat()
            val drawn = imageSize(width, height, zoom)
            val origin = topLeft(width, height, drawn) + pan
            tiles.update((drawn.x / image.width).toDouble(), PdfRegion(
                (-origin.x / drawn.x).toDouble(), (-origin.y / drawn.y).toDouble(),
                ((width - origin.x) / drawn.x).toDouble(), ((height - origin.y) / drawn.y).toDouble()
            ))
        }
    }

    Canvas(
        modifier.onSizeChanged { viewport = it }.pointerInput(image) {
            detectTransformGestures { centroid, delta, factor, _ -> transform(size, centroid, delta, factor) }
        }.pointerInput(image) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val change = event.changes.firstOrNull() ?: continue
                        val direction = change.scrollDelta.y
                        if (direction != 0f) {
                            transform(size, change.position, Offset.Zero, if (direction < 0) 1.25f else 0.8f)
                            change.consume()
                        }
                    }
                }
            }
        }.pointerInput(image) {
            detectTapGestures(
                onDoubleTap = { point ->
                    if (zoom > 1f) {
                        zoom = 1f
                        pan = Offset.Zero
                    } else {
                        transform(size, point, Offset.Zero, 2f)
                    }
                },
                onTap = { tap ->
                    val drawn = imageSize(size.width.toFloat(), size.height.toFloat(), zoom)
                    val origin = topLeft(size.width.toFloat(), size.height.toFloat(), drawn) + pan
                    val x = (tap.x - origin.x) / drawn.x
                    val y = (tap.y - origin.y) / drawn.y
                    if (x in 0f..1f && y in 0f..1f) selectPoint?.invoke(PercentCoordinate(x, y))
                }
            )
        }
    ) {
        val drawn = imageSize(size.width, size.height, zoom)
        val origin = topLeft(size.width, size.height, drawn) + pan
        clipRect {
            drawImage(
                image,
                dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
                dstSize = IntSize(drawn.x.roundToInt().coerceAtLeast(1), drawn.y.roundToInt().coerceAtLeast(1))
            )
            for ((tile, detail) in tiles.visibleTiles) {
                val region = tile.region
                val left = (origin.x + drawn.x * region.left).roundToInt()
                val top = (origin.y + drawn.y * region.top).roundToInt()
                val right = (origin.x + drawn.x * region.right).roundToInt()
                val bottom = (origin.y + drawn.y * region.bottom).roundToInt()
                if (right > left && bottom > top) {
                    drawImage(detail, dstOffset = IntOffset(left, top), dstSize = IntSize(right - left, bottom - top))
                }
            }
            for (point in points) {
                if (point.x !in 0f..1f || point.y !in 0f..1f) continue
                val center = origin + Offset(point.x * drawn.x, point.y * drawn.y)
                drawCircle(outline, 8.dp.toPx(), center)
                drawCircle(markerColor, 6.dp.toPx(), center)
            }
        }
    }
}
