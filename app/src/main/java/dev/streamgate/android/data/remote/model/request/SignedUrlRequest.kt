package dev.streamgate.android.data.remote.model.request


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadRequest(
    @SerialName("corsOrigin") val corsOrigin: String = "*",
    @SerialName("pushMediaSettings") val pushMediaSettings: PushMediaSettings,
)

@Serializable
data class PushMediaSettings(
    @SerialName("accessPolicy") val accessPolicy: String, // "public" or "private" or "drm"
    @SerialName("title") val title: String? = null,
    @SerialName("maxResolution") val maxResolution: String? = null,
    @SerialName("mediaQuality") val mediaQuality: String? = null,
    @SerialName("optimizeAudio") val optimizeAudio: Boolean? = null,
    @SerialName("metadata") val metadata: Map<String, String>? = null
)
