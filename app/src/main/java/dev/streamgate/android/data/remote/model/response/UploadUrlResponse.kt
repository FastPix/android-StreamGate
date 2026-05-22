package dev.streamgate.android.data.remote.model.response


import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class UploadUrlResponse(
    val success: Boolean = false,
    val data: Data
)

@Keep
@Serializable
data class Data(
    val uploadId: String,
    val url: String
)