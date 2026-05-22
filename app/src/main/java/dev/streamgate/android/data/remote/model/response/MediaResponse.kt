package dev.streamgate.android.data.remote.model.response

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class MediaResponse(
    val data: DataPayload
)


@Keep
@Serializable
data class ListMediaResponse(
    val success: Boolean,
    val data: List<DataPayload>,
    val pagination: PaginationInfo
)

@Keep
@Serializable
data class DataPayload(
    @SerialName("id")
    val uploadId: String,
    val status: String, // e.g., "Created", "Downloading", "Downloaded", "Processing", "Validating", "Ready", "Failed", "In Queue"
    val title: String? = null,
    val mediaQuality: String,
    val maxResolution: String,
    val thumbnail: String? = null,
    val playbackIds: List<PlaybackId>
)

@Keep
@Serializable
data class PlaybackId(
    val id: String,
    val accessPolicy: String, // "public" or "private"
)


@Keep
@Serializable
data class PaginationInfo(
    val totalRecords: Int,
    val currentOffset: Int,
    val offsetCount: Int
)