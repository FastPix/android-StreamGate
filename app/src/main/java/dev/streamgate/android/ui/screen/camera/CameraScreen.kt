package dev.streamgate.android.ui.screen.camera

import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.streamgate.android.ui.screen.home.VideoSource
import dev.streamgate.android.utils.createVideoFile
import kotlinx.serialization.Serializable

@Serializable
object ScreenCamera

@Composable
fun CameraScreen(
    onVideoSaved: (source: String, filePath: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        }
    }

    val hasFrontCamera = remember { mutableStateOf(false) }
    val hasBackCamera = remember { mutableStateOf(false) }

    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isAudioOn by remember { mutableStateOf(true) }
    var isTorchOn by remember { mutableStateOf(false) }
    var isBackCamera by remember { mutableStateOf(true) }

    var currentRecording: Recording? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            try {
                hasBackCamera.value = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                hasFrontCamera.value = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(isBackCamera) {
        controller.cameraSelector = if (isBackCamera)
            CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
    }

    LaunchedEffect(isTorchOn) {
        try { controller.enableTorch(isTorchOn) } catch (e: Exception) { e.printStackTrace() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {

                if (hasBackCamera.value && hasFrontCamera.value) {
                    IconButton(onClick = { isBackCamera = !isBackCamera }) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isAudioOn = !isAudioOn
                        currentRecording?.mute(!isAudioOn)
                    }
                ) {
                    Icon(
                        if (isAudioOn) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Audio",
                        tint = if (isAudioOn) Color.White else Color.Red
                    )
                }

                IconButton(onClick = { isTorchOn = !isTorchOn }) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (isTorchOn) Color.Yellow else Color.White
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isRecording) {
                IconButton(
                    onClick = {
                        if (isPaused) {
                            currentRecording?.resume()
                            isPaused = false
                        } else {
                            currentRecording?.pause()
                            isPaused = true
                        }
                    },
                    modifier = Modifier.size(50.dp).background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause/Resume",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(50.dp))
            }

            IconButton(
                onClick = {
                    if (isRecording) {
                        currentRecording?.stop()
                        isRecording = false
                        isPaused = false
                    } else {
                        val videoFile = createVideoFile(context, VideoSource.CAMERA.name)
                        val outputOptions = FileOutputOptions.Builder(videoFile).build()

                        currentRecording = controller.startRecording(
                            outputOptions,
                            AudioConfig.create(true),
                            ContextCompat.getMainExecutor(context),
                            { event ->
                                when (event) {
                                    is VideoRecordEvent.Finalize -> {
                                        if (!event.hasError()) {
                                            Toast.makeText(context, "Saved: ${videoFile.name}", Toast.LENGTH_SHORT).show()
                                            onVideoSaved(VideoSource.CAMERA.name, videoFile.absolutePath)
                                        } else {
                                            currentRecording?.close()
                                            isRecording = false
                                            Toast.makeText(context, "Error: ${event.error}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        )

                        currentRecording?.mute(!isAudioOn)
                        isRecording = true
                    }
                },
                modifier = Modifier
                    .size(80.dp)
                    .background(if (isRecording) Color.Red else Color.White, CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                    contentDescription = "Record",
                    tint = if (isRecording) Color.White else Color.Red,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.size(50.dp))
        }
    }
}
