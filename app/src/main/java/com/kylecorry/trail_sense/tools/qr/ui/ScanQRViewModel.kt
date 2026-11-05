package com.kylecorry.trail_sense.tools.qr.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.shared.haptics.HapticSubsystem
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteRepo
import com.kylecorry.trail_sense.tools.qr.infrastructure.BeaconQREncoder
import com.kylecorry.trail_sense.tools.qr.infrastructure.NoteQREncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface ScanQREvent {
    data class NoteSaved(val noteId: Long) : ScanQREvent
    data class BeaconCreated(val beaconId: Long) : ScanQREvent
    data class ResultDeleted(val text: String, val index: Int) : ScanQREvent
    data object NoQrCodeDetected : ScanQREvent
}

/** The scanner's history (newest first, ten at most) and what to do with a scanned code. */
internal class ScanQRViewModel(
    private val haptics: HapticSubsystem,
    private val noteRepo: NoteRepo,
    private val beaconService: IBeaconService
) : ViewModel() {

    var history by mutableStateOf<List<String>>(emptyList())
        private set
    var historyScrollRequest by mutableIntStateOf(0)
        private set
    var isCameraDenied by mutableStateOf(false)
        private set

    private val beaconQREncoder = BeaconQREncoder()
    private val noteQREncoder = NoteQREncoder()
    private val scanLock = Any()

    private val _events = Channel<ScanQREvent>(Channel.BUFFERED)
    val events: Flow<ScanQREvent> = _events.receiveAsFlow()

    fun onResume() {
        historyScrollRequest++
    }

    fun onPause() {
        haptics.off()
    }

    fun onCameraPermission(isGranted: Boolean) {
        isCameraDenied = !isGranted
    }

    /** Reads a code from [bitmap] (a camera frame, or a picked image when [isFromGallery]) and recycles it. */
    fun onImage(bitmap: Bitmap, isFromGallery: Boolean = false) = synchronized(scanLock) {
        var message: String? = null
        try {
            message = QR.decode(bitmap, highAccuracy = isFromGallery)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bitmap.recycle()
        }

        if (message != null) {
            onQRScanned(message)
        } else if (isFromGallery) {
            _events.trySend(ScanQREvent.NoQrCodeDetected)
        }
    }

    fun createNote(text: String) {
        viewModelScope.launch {
            val id = withContext(Dispatchers.IO) {
                noteRepo.addNote(noteQREncoder.decode(text))
            }
            _events.send(ScanQREvent.NoteSaved(id))
        }
    }

    fun createBeacon(text: String) {
        val beacon = beaconQREncoder.decode(text) ?: return
        viewModelScope.launch {
            val id = beaconService.add(beacon)
            _events.send(ScanQREvent.BeaconCreated(id))
        }
    }

    fun deleteResult(text: String) {
        val index = history.indexOf(text)
        if (index < 0) {
            return
        }
        history = history.filterNot { it == text }
        _events.trySend(ScanQREvent.ResultDeleted(text, index))
    }

    /** Undoes [deleteResult]: puts [text] back where it was. */
    fun restoreResult(text: String, index: Int) {
        if (history.contains(text)) {
            return
        }
        val restored = history.toMutableList()
        if (index <= restored.size) {
            restored.add(index, text)
        } else {
            restored.add(text)
        }
        history = restored
    }

    private fun onQRScanned(message: String) {
        val lastMessage = history.firstOrNull()
        if (message.isNotEmpty() && lastMessage != message) {
            addReading(message)
            haptics.click()
        }
    }

    private fun addReading(text: String) {
        val updated = history.toMutableList()
        updated.remove(text)
        updated.add(0, text)
        while (updated.size > MAX_HISTORY) {
            updated.removeLastOrNull()
        }
        history = updated
        historyScrollRequest++
    }

    companion object {
        private const val MAX_HISTORY = 10
    }
}
