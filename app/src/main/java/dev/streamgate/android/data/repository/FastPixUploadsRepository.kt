package dev.streamgate.android.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import dev.streamgate.android.BuildConfig
import dev.streamgate.android.data.remote.FastPixApi
import dev.streamgate.android.data.remote.model.request.PushMediaSettings
import dev.streamgate.android.data.remote.model.request.UploadRequest
import dev.streamgate.android.data.remote.model.response.DataPayload
import dev.streamgate.android.ui.screen.upload.ApiResult
import dev.streamgate.android.ui.screen.upload.NewUpload
import io.fastpix.uploads.FastPixUploader
import io.fastpix.uploads.UploadError
import io.fastpix.uploads.UploadListener
import io.fastpix.uploads.UploadState
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

class UploadRepository @Inject constructor(
    private val fastPixApi: FastPixApi,
    private val context: Context
) {

    @Volatile
    private var activeSdkInstance: FastPixUploader? = null
    private val _uploadState = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadState: StateFlow<UploadStatus> = _uploadState.asStateFlow()

    suspend fun getSessionUri(
        metadata: Map<String, String>? = null,
        title: String? = null,
        visibility: String = "public",
        maxResolution: String = "1080p",
        mediaQuality: String = "standard",
        optimizeAudio: Boolean? = null
    ): NewUpload {

        val credentials = "${BuildConfig.FASTPIX_TOKEN_ID}:${BuildConfig.FASTPIX_SECRET_KEY}"
        val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        return try {
            val response = fastPixApi.initiateAndGetSessionUri(
                authHeader = auth,
                request = UploadRequest(
                    corsOrigin = "*",
                    pushMediaSettings = PushMediaSettings(
                        title = title,
                        accessPolicy = visibility,
                        maxResolution = maxResolution,
                        mediaQuality = mediaQuality,
                        optimizeAudio = optimizeAudio,
                        metadata = metadata
                    ),
                ),
            )

            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data?.url == null) {
                    Log.wtf("FastPixUploadsRepository", "Missing URL or Upload ID in response: ${response.body()}")
                    return NewUpload(error = "Invalid response from server: Missing URL or Upload ID")
                }
                return NewUpload(sessionUri = data.url, uploadId = data.uploadId)
            }
            else {
                val errorCode = response.code()
                val errorString = response.errorBody()?.string() ?: "Unknown error"
                Log.wtf("FastPixUploadsRepository", "Upload URL request failed: $errorCode $errorString")
                return NewUpload(errorCode = errorCode, error = errorString)
            }
        } catch (e: Exception) {
            Log.wtf("FastPixUploadsRepository", e)
            NewUpload(error = e.message ?: "Failed to get Session Uri")
        }
    }

    suspend fun getMediaId(uploadId: String): Triple<Boolean, String?, String?> {
        val credentials = "${BuildConfig.FASTPIX_TOKEN_ID}:${BuildConfig.FASTPIX_SECRET_KEY}"
        val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

        return try {
            val response = fastPixApi.getMediaInfo(authHeader = auth, uploadId = uploadId)
            if (response.isSuccessful) {
                val mediaResponse = response.body()
                val data = mediaResponse?.data
                val status = data?.status
                val playbackIdsList = data?.playbackIds

                if (status == null) {
                    Log.wtf("FastPixUploadRepository", "Missing status in response: ${response.body()}")
                    return Triple(false, "Invalid response from server: Missing status", status)
                }

                if (status.lowercase().contains("ready")) {
                    val firstId = playbackIdsList?.firstOrNull()?.id
                    if (firstId != null) {
                        Triple(true, firstId, status)
                    } else {
                        Log.wtf("FastPixUploadRepository", "Missing playback IDs in response: ${response.body()}")
                        Triple(false, "Invalid response from server: Missing playback IDs", status)
                    }
                } else if (status.lowercase().contains("failed")) {
                    Log.wtf("FastPixUploadRepository", "Upload failed according to status: ${response.body()}")
                    Triple(false, "Upload failed: $status", status)
                } else {
                    Log.wtf("FastPixUploadRepository", "Media not ready yet: $status")
                    Triple(true, null, status)
                }
            } else {
                val errorCode = response.code()
                val errorString = response.errorBody()?.string() ?: "Unknown error"
                if ("media workspace relation not found" in errorString.lowercase()) {
                    return Triple(true, null, null)
                }

                Log.wtf("FastPixUploadRepository", "Media info request failed: $errorCode $errorString")
                Triple(false, "Failed to retrieve media info: $errorCode $errorString", null)
            }
        } catch (e: Exception) {
            Log.wtf("FastPixUploadRepository", e)
            Triple(false, e.message ?: "Failed to retrieve media info", null)
        }
    }

    fun listMediaFlow(limit: Int = 10, offset: Int = 1, orderBy: String = "desc"): Flow<ApiResult<List<DataPayload>>> = flow {
        emit(ApiResult.Loading)

        try {
            val credentials = "${BuildConfig.FASTPIX_TOKEN_ID}:${BuildConfig.FASTPIX_SECRET_KEY}"
            val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

            val response = fastPixApi.listMedia(authHeader = auth, limit = limit, offset = offset, orderBy = orderBy)
            if (response.isSuccessful) {
                val mediaList = response.body()?.data ?: emptyList()
                emit(ApiResult.Success(mediaList))
            } else {
                val errorCode = response.code()
                val errorString = response.errorBody()?.string() ?: "Unknown error"
                Log.wtf("FastPixUploadRepository", "List media request failed: $errorCode $errorString")
                emit(ApiResult.Error(
                    errorCode = errorCode,
                    errorMessage = errorString
                ))
            }
        } catch (e: IOException) {
            Log.e("FastPixUploadRepository", "Network failure", e)
            emit(ApiResult.Error(errorCode = null, errorMessage = "No internet connection"))
        } catch (e: Exception) {
            Log.e("FastPixUploadRepository", "Unexpected error", e)
            emit(ApiResult.Error(errorCode = null, errorMessage = e.localizedMessage ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    val activeUpload: Boolean
        get() = activeSdkInstance != null

    fun updateSharedState(status: UploadStatus) {
        _uploadState.value = status
    }

    fun executeUpload(
        filePath: String, sessionUri: String,
        uploadId: String, chunkSize: Int = 8,
        maxRetries: Int = 5
    ): Flow<UploadStatus> = callbackFlow {

        if (activeUpload) {
            trySend(UploadStatus.Error(UploadError.UnexpectedResponse("An upload is already in progress.")))
            close()
            return@callbackFlow
        }

        val file = File(filePath)

        val sdk = FastPixUploader.Builder(context)
            .file(file)
            .chunkSize((chunkSize * 1024 * 1024).toLong())
            .sessionUri(sessionUri)
            .maxRetries(maxRetries)
//            .debugLogging(true)
            .listener(object: UploadListener {
                override fun onProgress(
                    bytesUploaded: Long,
                    totalBytes: Long,
                    percentage: Double
                ) {
                    super.onProgress(bytesUploaded, totalBytes, percentage)
                    trySend(UploadStatus.Progress(percentage))
                }

                override fun onStateChange(state: UploadState) {
                    super.onStateChange(state)
                    trySend(UploadStatus.StateChange(state))
                }

                override fun onSuccess(elapsedMillis: Long) {
                    super.onSuccess(elapsedMillis)
                    trySend(UploadStatus.Success(uploadId = uploadId))
                    cleanupSession()
                    close()
                }

                override fun onCancelled(elapsedMillis: Long) {
                    super.onCancelled(elapsedMillis)
                    trySend(UploadStatus.Canceled)
                    cleanupSession()
                    close()
                }

                override fun onFailure(
                    error: UploadError,
                    elapsedMillis: Long
                ) {
                    super.onFailure(error, elapsedMillis)
                    trySend(UploadStatus.Error(error))
                    cleanupSession()
                    close()
                }
            })
            .build()

        activeSdkInstance = sdk
        sdk.start()

        awaitClose {
            sdk.cancel()
            cleanupSession()
        }
    }

    fun pauseActiveUpload() {
        activeSdkInstance?.pause()
    }
    fun resumeActiveUpload() {
        activeSdkInstance?.resume()
    }
    fun abortActiveUpload() {
        activeSdkInstance?.cancel()
        cleanupSession()
        _uploadState.value = UploadStatus.Canceled
    }

    private fun cleanupSession() { activeSdkInstance = null }
}

sealed interface UploadStatus {
    object Idle: UploadStatus
    data class Progress(val percentage: Double): UploadStatus
    data class Success(val uploadId: String): UploadStatus
    data class StateChange(val state: UploadState): UploadStatus
    data class Error(val error: UploadError): UploadStatus
    object Canceled: UploadStatus
}
