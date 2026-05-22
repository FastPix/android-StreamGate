package dev.streamgate.android.ui.screen.screen_record

import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.streamgate.android.data.local.ScreenRecordService
import dev.streamgate.android.ui.screen.home.VideoSource
import dev.streamgate.android.ui.screen.setting.PreferenceViewModel
import kotlinx.serialization.Serializable

@Serializable
object ScreenRecord

@Composable
fun RecordScreen(
    onScreenRecord: (source: String, String) -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(isServiceRunning(context, ScreenRecordService::class.java)) }

    val currentOnScreenRecord by rememberUpdatedState(onScreenRecord)
    var lastProcessedPath by remember { mutableStateOf("") }

    val mainViewModel = hiltViewModel<PreferenceViewModel>()
    val frameRate by mainViewModel.frameRate.collectAsStateWithLifecycle()

    DisposableEffect(ScreenRecordService.ACTION_RECORD_COMPLETE) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val path = intent?.getStringExtra(ScreenRecordService.EXTRA_FILE_PATH)

                if (!path.isNullOrEmpty() && path != lastProcessedPath) {
                    lastProcessedPath = path
                    isRecording = false

                    currentOnScreenRecord(VideoSource.SCREEN_RECORD.name, path)
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ScreenRecordService.ACTION_RECORD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val projectionManager = remember { context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
    val captureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            isRecording = true
            lastProcessedPath = ""
            val intent = Intent(context, ScreenRecordService::class.java).apply {
                putExtra(ScreenRecordService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecordService.EXTRA_DATA, result.data)
                putExtra(ScreenRecordService.EXTRA_FRAME_RATE, frameRate)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                if (!isRecording) {
                    captureLauncher.launch(projectionManager.createScreenCaptureIntent())
                } else {
                    val stopIntent = Intent(context, ScreenRecordService::class.java).apply {
                        action = ScreenRecordService.ACTION_STOP_SERVICE
                    }
                    context.stopService(stopIntent)
                    isRecording = false
                }
            }
        ) {
            Text(if (isRecording) "Stop Recording" else "Start Screen Recording")
        }
    }
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}
