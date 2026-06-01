package dev.streamgate.android.ui.screen.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.streamgate.android.ui.screen.home.VideoSource
import dev.streamgate.android.utils.createVideoFile

class CameraState(
    val context: Context,
    val controller: LifecycleCameraController,
    val onVideoSaved: (String, String) -> Unit
) {

    companion object {
        private const val TAG = "CameraState"
    }

    var isRecording by mutableStateOf(false)
    var isPaused by mutableStateOf(false)
    var isAudioOn by mutableStateOf(true)
    var isTorchOn by mutableStateOf(false)
    var isBackCamera by mutableStateOf(true)
    var hasFrontCamera by mutableStateOf(false)
    var hasBackCamera by mutableStateOf(false)

    private var currentRecording: Recording? = null

    fun toggleCamera() {
        isBackCamera = !isBackCamera
        controller.cameraSelector = if (isBackCamera)
            CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
    }

    fun toggleTorch() {
        isTorchOn = !isTorchOn
        controller.enableTorch(isTorchOn)
    }

    fun toggleAudio() {
        isAudioOn = !isAudioOn
        currentRecording?.mute(!isAudioOn)
    }

    fun togglePause() {
        if (isPaused) currentRecording?.resume() else currentRecording?.pause()
        isPaused = !isPaused
    }

    fun handleRecordAction() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val videoFile = createVideoFile(context, VideoSource.CAMERA.name)
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        currentRecording = controller.startRecording(
            outputOptions,
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(context)
        ) { event ->
            if (event is VideoRecordEvent.Finalize) {
                handleRecordingFinalized(event, videoFile.absolutePath)
            }
        }
        currentRecording?.mute(!isAudioOn)
        isRecording = true
    }

    private fun stopRecording() {
        currentRecording?.stop()
        isRecording = false
        isPaused = false
    }

    private fun handleRecordingFinalized(event: VideoRecordEvent.Finalize, path: String) {
        if (!event.hasError()) {
            onVideoSaved(VideoSource.CAMERA.name, path)
        } else {
            isRecording = false
            Log.e(TAG, "Error: ${event.error}")
        }
    }
}


@Composable
fun rememberCameraState(
    context: Context = LocalContext.current,
    controller: LifecycleCameraController,
    onVideoSaved: (String, String) -> Unit
) = remember { CameraState(context, controller, onVideoSaved) }