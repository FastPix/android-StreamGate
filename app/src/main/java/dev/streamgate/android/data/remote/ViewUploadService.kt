package dev.streamgate.android.data.remote

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.streamgate.android.MainActivity
import dev.streamgate.android.data.repository.UploadRepository
import dev.streamgate.android.data.repository.UploadStatus
import io.fastpix.uploads.UploadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VideoUploadService: Service() {

    @Inject lateinit var repository: UploadRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var uploadJob: Job? = null

    private val notificationId = 1001
    private val channelId = "video_upload_channel"

    private var progress = 0.0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: return START_NOT_STICKY
                val sessionUri = intent.getStringExtra(EXTRA_SESSION_URI) ?: return START_NOT_STICKY
                val uploadId = intent.getStringExtra(EXTRA_UPLOAD_ID) ?: return START_NOT_STICKY

                startForeground(notificationId, buildNotification(0.0, isPaused = false))

                startUploadPipeline(filePath, sessionUri, uploadId)
            }
            ACTION_PAUSE -> {
                repository.pauseActiveUpload()
                updateNotification(progress, isPaused = true) // Retain current look as paused
            }
            ACTION_RESUME -> {
                repository.resumeActiveUpload()
                updateNotification(progress, isPaused = false)
            }
            ACTION_CANCEL -> {
                uploadJob?.cancel()
                repository.abortActiveUpload()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startUploadPipeline(filePath: String, sessionUri: String, uploadId: String) {
        uploadJob?.cancel()
        uploadJob = serviceScope.launch {
            repository.executeUpload(filePath, sessionUri, uploadId).collect { status ->
                repository.updateSharedState(status)
                when (status) {
                    is UploadStatus.Progress -> {
                        progress = status.percentage
                        updateNotification(status.percentage, isPaused = false)
                    }
                    is UploadStatus.Success -> {
                        showCompletionNotification("Upload Finished!", "Tap to view")
                        stopSelf()
                    }
                    is UploadStatus.StateChange -> {
                        if (status.state == UploadState.PAUSED) updateNotification(progress, isPaused = true)
                    }
                    is UploadStatus.Error -> {
                        showCompletionNotification("Upload Failed", status.error.message ?: status.error.localizedMessage)
                        stopSelf()
                    }
                    is UploadStatus.Canceled -> {
                        stopSelf()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun updateNotification(progress: Double, isPaused: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, buildNotification(progress, isPaused))
    }

    private fun buildNotification(progress: Double, isPaused: Boolean): Notification {
        val flag = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT


        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flag
        )

        // Action Button PendingIntents
        val pauseIntent = PendingIntent.getService(
            this, 1, Intent(this, VideoUploadService::class.java).apply { action = ACTION_PAUSE }, flag
        )
        val resumeIntent = PendingIntent.getService(
            this, 2, Intent(this, VideoUploadService::class.java).apply { action = ACTION_RESUME }, flag
        )
        val cancelIntent = PendingIntent.getService(
            this, 3, Intent(this, VideoUploadService::class.java).apply { action = ACTION_CANCEL }, flag
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Uploading Video")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        builder.clearActions()

        if (isPaused) {
            builder.setContentText("Upload paused")
                .addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
        } else {
            builder.setContentText("${String.format(Locale.ENGLISH, "%.1f", progress)}% completed")
                .setProgress(100, progress.toInt(), false)
                .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
        }

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
        return builder.build()
    }

    private fun showCompletionNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Video Upload System", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Displays active media transfer channels." }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)
        repository.abortActiveUpload()
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_CANCEL = "ACTION_CANCEL"

        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        const val EXTRA_SESSION_URI = "EXTRA_SESSION_URI"
        const val EXTRA_UPLOAD_ID = "EXTRA_UPLOAD_ID"
    }
}
