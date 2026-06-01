package dev.streamgate.android.ui.screen.camera

import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.serialization.Serializable

@Serializable
object ScreenCamera
@Composable
fun CameraScreen(onVideoSaved: (source: String, filePath: String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.VIDEO_CAPTURE)
    } }

    val state = rememberCameraState(context, controller, onVideoSaved)

    LaunchedEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        state.hasBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        state.hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PreviewView(ctx).apply { this.controller = controller; controller.bindToLifecycle(lifecycleOwner) } },
            modifier = Modifier.fillMaxSize()
        )

        TopControlBar(state)

        BottomControlBar(
            state = state,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}


@Composable
fun TopControlBar(state: CameraState) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(0.2f), CircleShape)
                .padding(horizontal = 4.dp)
        ) {
            if (state.hasBackCamera && state.hasFrontCamera) {
                IconButton(onClick = { state.toggleCamera() }) {
                    Icon(Icons.Default.Cameraswitch, "Switch", tint = Color.White)
                }
            }

            IconButton(onClick = { state.toggleAudio() }) {
                Icon(
                    if (state.isAudioOn) Icons.Default.Mic else Icons.Default.MicOff,
                    "Audio", tint = if (state.isAudioOn) Color.White else Color.Red
                )
            }

            IconButton(onClick = { state.toggleTorch() }) {
                Icon(
                    if (state.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    "Torch", tint = if (state.isTorchOn) Color.Yellow else Color.White
                )
            }
        }
    }
}

@Composable
fun BottomControlBar(state: CameraState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isRecording) {
            CircleIconButton(
                icon = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                onClick = { state.togglePause() }
            )
        } else {
            Spacer(Modifier.size(50.dp))
        }

        IconButton(
            onClick = { state.handleRecordAction() },
            modifier = Modifier
                .size(80.dp)
                .background(if (state.isRecording) Color.Red else Color.White, CircleShape)
        ) {
            Icon(
                if (state.isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = "Record",
                tint = if (state.isRecording) Color.White else Color.Red,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.size(50.dp))
    }
}

@Composable
fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(50.dp).background(Color.DarkGray.copy(0.5f), CircleShape)
    ) {
        Icon(icon, null, tint = Color.White)
    }
}