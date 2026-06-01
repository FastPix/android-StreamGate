package dev.streamgate.android.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import dev.streamgate.android.ui.screen.home.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "FileUtil"


fun getNewUploadName(source: String): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
    val videoSource = when(source) {
        VideoSource.DEVICE.name -> "DEVICE"
        VideoSource.CAMERA.name -> "CAM"
        VideoSource.SCREEN_RECORD.name -> "SCRN_RECORD"
        else -> "VIDEO"
    }

    return "STREAMGATE_${videoSource}_$timestamp"
}

fun getFileName(context: Context, uri: Uri): String {
    var name = "selected_video.mp4"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

fun createVideoFile(context: Context, source: String): File {
    val storageDir = context.cacheDir // Internal Storage
    return File(storageDir, "${getNewUploadName(source)}.mp4")
}

fun getVideoStorageDir(context: Context): File {
    val folder = File(context.cacheDir, "pending_uploads")
    if (!folder.exists()) {
        folder.mkdirs()
    }
    return folder
}

suspend fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val storageDir = getVideoStorageDir(context)
            val cacheFile = File(storageDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            cacheFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to internal storage: ${e.localizedMessage}", e)
            null
        }
    }
}

fun clearAllRecordedVideos(context: Context) {
    val folder = File(context.cacheDir, "pending_uploads")

    if (folder.exists() && folder.isDirectory) {
        val success = folder.deleteRecursively()

        if (success) {
            folder.mkdirs()
        }
    }
}

