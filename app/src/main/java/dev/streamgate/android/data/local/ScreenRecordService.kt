package dev.streamgate.android.data.local

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.streamgate.android.ui.screen.home.VideoSource
import dev.streamgate.android.utils.createVideoFile
import java.io.File

class ScreenRecordService: Service() {

    companion object {
        private const val TAG = "ScreenRecordService"

        const val ACTION_RECORD_COMPLETE = "SCREEN_RECORD_COMPLETE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA = "EXTRA_DATA"
        const val EXTRA_FRAME_RATE = "EXTRA_FRAME_RATE"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"

        private const val NOTIFICATION_CHANNEL_ID = "screen_record_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Screen Recording Service"
        private const val NOTIFICATION_ID = 101
        private const val VIRTUAL_DISPLAY_NAME = "ScreenCapture"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null
    private val outputFile: File by lazy {
        createVideoFile(applicationContext, VideoSource.SCREEN_RECORD.name)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            cleanupAndNotify()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_OK) ?: Activity.RESULT_CANCELED
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        val frameRate = intent?.getIntExtra(EXTRA_FRAME_RATE, 30) ?: 30

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()

        try {
            startRecording(resultCode, resultData, frameRate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start screen recording", e)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceNotification() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("StreamGate Screen Recording Active")
            .setContentText("You are Recording your Screen using StreamGate.")
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Recording",
                stopPendingIntent
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording(resultCode: Int, data: Intent, frameRate: Int) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                cleanupAndNotify()
            }
        }, null)

        val recorderInstance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        val (screenWidth, screenHeight) = getScreenDimensions()
        mediaRecorder = recorderInstance.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFile(outputFile.absolutePath)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            setVideoSize(screenWidth, screenHeight)
            setVideoEncodingBitRate(5 * 1024 * 1024) // 5 Mbps optimal for mobile ig
            setVideoFrameRate(frameRate)

            prepare()
        }

        val metrics = resources.displayMetrics
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME,
            screenWidth, screenHeight, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null, null
        )

        mediaRecorder?.start()
    }

    private fun setupVirtualDisplay(width: Int, height: Int) {
        val metrics = resources.displayMetrics
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            VIRTUAL_DISPLAY_NAME, width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface, null, null
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        virtualDisplay?.release()

        val (newWidth, newHeight) = getScreenDimensions()
        setupVirtualDisplay(newWidth, newHeight)
    }

    private fun cleanupAndNotify() {
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null

        virtualDisplay?.release()
        virtualDisplay = null

        val path = if (outputFile.exists() && outputFile.length() > 0) outputFile.absolutePath else ""

        if (path.isNotEmpty()) {
            val completeIntent = Intent(ACTION_RECORD_COMPLETE).apply {
                putExtra(EXTRA_FILE_PATH, path)
                setPackage(packageName)
            }
            sendBroadcast(completeIntent)
        } else {
            Log.e(TAG, "Recording failed or output file is empty.")
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        launchIntent?.let {
            startActivity(it)
        }
    }

    override fun onDestroy() {
        cleanupAndNotify()
        mediaProjection?.stop()
        super.onDestroy()
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        val windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }

}