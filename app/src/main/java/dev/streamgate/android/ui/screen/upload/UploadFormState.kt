package dev.streamgate.android.ui.screen.upload

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UploadFormState(initialTitle: String) {
    var title by mutableStateOf(initialTitle)
    var metaField by mutableStateOf("")
    var metaValue by mutableStateOf("")

    val isValid get() = title.isNotBlank() &&
            ((metaField.isBlank() && metaValue.isBlank()) || (metaField.isNotBlank() && metaValue.isNotBlank()))

    fun getMetadataMap() = if (metaField.isNotBlank()) mapOf(metaField to metaValue) else null
}