package dev.streamgate.android.ui.screen.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ScreenShare
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.streamgate.android.utils.clearAllRecordedVideos
import kotlinx.serialization.Serializable

@Serializable
object ScreenHome


enum class VideoSource {
    DEVICE, SCREEN_RECORD, CAMERA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onUpload: (source: String, filePath: String) -> Unit,
    onScreenRecord: () -> Unit,
    onCameraRecord: () -> Unit
) {
    val context = LocalContext.current
    val state = rememberHomeState(context, rememberCoroutineScope(), onUpload, onCameraRecord)

    val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        state.onVideoPicked(it)
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val videoPermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        state.handleVideoPermissionResult(it, context as? Activity)
    }

    LaunchedEffect(Unit) {
        clearAllRecordedVideos(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) state.showNotificationRationale = true
        }
    }

    HomeDialogs(state, notificationLauncher, videoPermissionsLauncher)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        HomeHeader()

        Spacer(Modifier.height(36.dp))

        UploadCard(onClick = {
            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        })

        Spacer(Modifier.height(16.dp))

        ActionButtons(
            onScreenRecord = onScreenRecord,
            onCameraClick = {
                val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasCamera && hasAudio) onCameraRecord() else state.showVideoRationale = true
            }
        )

        Spacer(Modifier.weight(1f))

        Footer()
    }

    if (state.showLoadingDialog) LoadingDialog()
}

@Composable
fun HomeHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Share video, instantly", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Upload a Video or Record your Screen - get a shareable link in seconds.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp).padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun UploadCard(onClick: () -> Unit) {
    val stroke = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f))
    val borderColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth()
            .drawBehind { drawRoundRect(color = borderColor, style = stroke, cornerRadius = CornerRadius(16.dp.toPx())) }
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape).padding(16.dp)) {
                Icon(Icons.Outlined.CloudUpload, null, modifier = Modifier.size(36.dp))
            }
            Text("Upload a Video", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text("MP4, MOV, WebM, AVI and more.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun ActionButtons(onScreenRecord: () -> Unit, onCameraClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RecordButton(Modifier.weight(1f), "Record Screen", Icons.AutoMirrored.Outlined.ScreenShare, onScreenRecord)
        RecordButton(Modifier.weight(1f), "Record Camera", Icons.Outlined.Videocam, onCameraClick)
    }
}

@Composable
fun RecordButton(modifier: Modifier, text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.7f), contentColor = MaterialTheme.colorScheme.onBackground)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 16.dp)) {
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(text)
        }
    }
}

@Composable
fun HomeDialogs(
    state: HomeState,
    notificationLauncher: ManagedActivityResultLauncher<String, Boolean>,
    videoPermissionsLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    if (state.showNotificationRationale) {
        PermissionDialog(
            title = "Enable Notifications",
            text = "We need notification permissions to show your video upload progress.",
            onConfirm = {
                state.showNotificationRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = { state.showNotificationRationale = false }
        )
    }

    if (state.showVideoRationale) {
        PermissionDialog(
            title = "Camera & Microphone Access",
            text = "To record videos, this app needs access to your camera and microphone.",
            onConfirm = {
                state.showVideoRationale = false
                videoPermissionsLauncher.launch(state.requiredVideoPermissions)
            },
            onDismiss = { state.showVideoRationale = false }
        )
    }

    if (state.showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { state.showSettingsDialog = false },
            title = { Text("Permissions Permanently Denied") },
            text = { Text("Please enable permissions in settings.") },
            confirmButton = {
                Button(onClick = { openAppSettings(state.context); state.showSettingsDialog = false }) { Text("Settings") }
            }
        )
    }
}

@Composable
fun PermissionDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Allow") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } }
    )
}

@Composable
fun LoadingDialog() {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { }, // Prevent dismissal during processing
        confirmButton = { },
        dismissButton = { },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CircularProgressIndicator()
                Text("Loading, please wait...")
            }
        }
    )
}

@Composable
fun Footer() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        text = "POWERED BY FASTPIX",
        color = MaterialTheme.colorScheme.surfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}