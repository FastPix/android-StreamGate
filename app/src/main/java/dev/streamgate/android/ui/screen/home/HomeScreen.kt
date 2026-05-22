package dev.streamgate.android.ui.screen.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.streamgate.android.utils.clearAllRecordedVideos
import dev.streamgate.android.utils.copyUriToInternalStorage
import dev.streamgate.android.utils.getFileName
import kotlinx.coroutines.launch
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
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }

    val showNotificationRationale = remember { mutableStateOf(false) }
    val showVideoRationale = remember { mutableStateOf(false) }
    val showSettingsDialog = remember { mutableStateOf(false) }
    val showLoadingDialog = remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notification permission is essential for upload progress and background service. Please enable it in settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val videoPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            onCameraRecord()
        } else {
            val cameraDenied = permissionsMap[Manifest.permission.CAMERA] == false
            val audioDenied = permissionsMap[Manifest.permission.RECORD_AUDIO] == false

            val showCameraRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
            val showAudioRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO) } ?: false

            if ((cameraDenied && !showCameraRationale) || (audioDenied && !showAudioRationale)) {
                showSettingsDialog.value = true
            } else {
                Toast.makeText(context, "Permissions required to record video.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        selectedUri = null
        clearAllRecordedVideos(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                showNotificationRationale.value = true
            }
        }
    }

    if (showNotificationRationale.value) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showNotificationRationale.value = false },
            title = { Text("Enable Notifications") },
            text = { Text("We need notification permissions to show your video upload progress and keep the background service running safely.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale.value = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationale.value = false }) { Text("Later") }
            }
        )
    }

    val requiredVideoPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    if (showVideoRationale.value) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showVideoRationale.value = false },
            title = { Text("Camera & Microphone Access") },
            text = { Text("To record videos, this app needs access to your camera and microphone. Please grant these permissions.") },
            confirmButton = {
                TextButton(onClick = {
                    showVideoRationale.value = false
                    videoPermissionsLauncher.launch(requiredVideoPermissions)
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = { showVideoRationale.value = false }) { Text("Cancel") }
            }
        )
    }

    if (showSettingsDialog.value) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = { showSettingsDialog.value = false },
            title = { Text("Permissions Permanently Denied") },
            text = { Text("You have disabled required permissions permanently. Please enable Camera and Microphone settings manually to use this feature.") },
            confirmButton = {
                Button(onClick = {
                    showSettingsDialog.value = false
                    openAppSettings(context)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog.value = false }) { Text("Cancel") }
            }
        )
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            showLoadingDialog.value = true
            selectedUri = uri
            videoName = getFileName(context, uri)

            scope.launch {
                val absolutePath = copyUriToInternalStorage(context, uri, videoName)
                if (absolutePath != null) {
                    showLoadingDialog.value = false
                    onUpload(VideoSource.DEVICE.name, absolutePath)
                }
            }
        }
    }

    Column(
        modifier=Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Share video, instantly",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = "Upload a Video or Record your Screen - get a shareable link in seconds.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 24.dp)
            )
        }

        Column(
            modifier = Modifier
                .padding(top = 36.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (selectedUri == null) {
                val stroke = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )

                val borderColor = MaterialTheme.colorScheme.surface

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = borderColor,
                                style = stroke,
                                cornerRadius = CornerRadius(16.dp.toPx())
                            )
                        }
                        .clickable(
                            indication = null,
                            interactionSource = null
                        ) {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                    border = null,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Upload a Video",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "MP4, MOV, WebM, AVI and more.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )

                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {

            Button(
                modifier = Modifier.weight(1f),
                onClick = onScreenRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ScreenShare,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Record Screen",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

                    when {
                        hasCamera && hasAudio -> onCameraRecord()
                        else -> showVideoRationale.value = true
                    }},
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Record Camera",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "POWERED BY FASTPIX",
            color = MaterialTheme.colorScheme.surfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }

    if (showLoadingDialog.value) {
        LoadingDialog()
    }
}


@Composable
fun LoadingDialog() {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { },
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


private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
