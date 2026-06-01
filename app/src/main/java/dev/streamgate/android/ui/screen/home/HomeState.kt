package dev.streamgate.android.ui.screen.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import dev.streamgate.android.utils.copyUriToInternalStorage
import dev.streamgate.android.utils.getFileName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class HomeState(
    val context: Context,
    val scope: CoroutineScope,
    val onUpload: (String, String) -> Unit,
    val onCameraRecord: () -> Unit
) {
    var showNotificationRationale by mutableStateOf(false)
    var showVideoRationale by mutableStateOf(false)
    var showSettingsDialog by mutableStateOf(false)
    var showLoadingDialog by mutableStateOf(false)
    var selectedUri by mutableStateOf<Uri?>(null)

    val requiredVideoPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    fun handleVideoPermissionResult(permissions: Map<String, Boolean>, activity: Activity?) {
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            onCameraRecord()
        } else {
            val cameraDenied = permissions[Manifest.permission.CAMERA] == false
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false

            if (cameraDenied && !showRationale) {
                showSettingsDialog = true
            }
        }
    }

    fun onVideoPicked(uri: Uri?) {
        if (uri == null) return
        showLoadingDialog = true
        selectedUri = uri
        val videoName = getFileName(context, uri)

        scope.launch {
            val path = copyUriToInternalStorage(context, uri, videoName)
            showLoadingDialog = false
            if (path != null) onUpload(VideoSource.DEVICE.name, path)
        }
    }
}

@Composable
fun rememberHomeState(
    context: Context = LocalContext.current,
    scope: CoroutineScope = rememberCoroutineScope(),
    onUpload: (String, String) -> Unit,
    onCameraRecord: () -> Unit
) = remember { HomeState(context, scope, onUpload, onCameraRecord) }