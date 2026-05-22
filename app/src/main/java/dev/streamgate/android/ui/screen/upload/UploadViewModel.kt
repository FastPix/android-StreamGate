package dev.streamgate.android.ui.screen.upload

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.streamgate.android.data.remote.VideoUploadService
import dev.streamgate.android.data.remote.model.response.DataPayload
import dev.streamgate.android.data.repository.UploadRepository
import dev.streamgate.android.data.repository.UploadStatus
import io.fastpix.uploads.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


data class NewUpload(
    var sessionUri: String? = null,
    var uploadId: String? = null,
    var errorCode: Int? = null,
    var error: String? = null,
)


@HiltViewModel
class UploadViewModel @Inject constructor(
    val fastPixRepository: UploadRepository,
    @param:ApplicationContext private val context: Context
): ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    private val _uiState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaState: StateFlow<ApiResult<List<DataPayload>>> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { fastPixRepository.listMediaFlow() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApiResult.Loading
        )

    init {
        observeRepositoryUploadState()
    }

    fun resetUiState() {
        _uiState.value = UploadUiState.Idle
    }

    fun startMediaUpload(path: String, title: String, metadata: Map<String, String>? = null) {
        if (fastPixRepository.activeUpload) return

        _uiState.value = UploadUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val result = fastPixRepository.getSessionUri(
                metadata = metadata,
                title = title,
            )
            if (result.sessionUri == null) {
                _uiState.value = UploadUiState.Error(result.error ?: "Failed to generate Session Uri")
                return@launch
            }

            val intent = Intent(context, VideoUploadService::class.java).apply {
                action = VideoUploadService.ACTION_START
                putExtra(VideoUploadService.EXTRA_FILE_PATH, path)
                putExtra(VideoUploadService.EXTRA_SESSION_URI, result.sessionUri)
                putExtra(VideoUploadService.EXTRA_UPLOAD_ID, result.uploadId)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun observeRepositoryUploadState() {
        viewModelScope.launch {
            fastPixRepository.uploadState.collect { status ->
                _uiState.value = when (status) {
                    is UploadStatus.Idle -> UploadUiState.Idle
                    is UploadStatus.Progress -> UploadUiState.Uploading(percentage = status.percentage)
                    is UploadStatus.Success -> UploadUiState.Success(uploadId = status.uploadId)
                    is UploadStatus.StateChange -> UploadUiState.StateChange(state = status.state)
                    is UploadStatus.Error -> UploadUiState.Error(message = status.error.message ?: status.error.localizedMessage)
                    is UploadStatus.Canceled -> UploadUiState.Error(message = "Upload Canceled! Upload was canceled before completion.")
                }
            }
        }
    }

    suspend fun getMediaId(uploadId: String): Triple<Boolean, String?, String?> {
        return withContext(Dispatchers.IO) {
            fastPixRepository.getMediaId(uploadId)
        }
    }

    fun pauseUpload() { sendControlAction(VideoUploadService.ACTION_PAUSE) }
    fun resumeUpload() { sendControlAction(VideoUploadService.ACTION_RESUME) }
    fun cancelUpload() { sendControlAction(VideoUploadService.ACTION_CANCEL) }

    private fun sendControlAction(actionString: String) {
        context.startService(Intent(context, VideoUploadService::class.java).apply { action = actionString })
    }

    fun refreshMedia() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }
}

sealed interface UploadUiState {
    object Idle : UploadUiState

    object Loading: UploadUiState
    data class Uploading(val percentage: Double) : UploadUiState
    data class Success(val uploadId: String) : UploadUiState
    data class StateChange(val state: UploadState) : UploadUiState
    data class Error(val message: String) : UploadUiState
}


sealed class ApiResult <out T>  {
    data object Loading : ApiResult<Nothing>()
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val errorCode: Int?, val errorMessage: String?) : ApiResult<Nothing>()
}
