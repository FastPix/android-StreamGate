package dev.streamgate.android.ui.screen.upload

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import dev.streamgate.android.R
import dev.streamgate.android.ui.screen.home.VideoSource
import dev.streamgate.android.ui.theme.StreamGateAccentOrange
import dev.streamgate.android.ui.theme.StreamGateBorder
import dev.streamgate.android.ui.theme.StreamGateCard
import dev.streamgate.android.ui.theme.StreamGateDark
import dev.streamgate.android.ui.theme.StreamGateGreen
import dev.streamgate.android.utils.VIDEO_SHARE_URL
import dev.streamgate.android.utils.getNewUploadName
import io.fastpix.media3.core.FastPixPlayer
import io.fastpix.uploads.UploadState
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.util.Locale


@Serializable
data class ScreenUpload(val source: String, val filePath: String)

@OptIn(UnstableApi::class)
@Composable
fun UploadScreen(
    source: String,
    filePath: String,
    viewModel: UploadViewModel = hiltViewModel(),
    onChangeVideo: () -> Unit,
    onPreview: (mediaId: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.resetUiState() }

    if (state is UploadUiState.Success) {
        UploadSuccessScreen(
            current = state as UploadUiState.Success,
            viewModel = viewModel,
            onPreview = onPreview,
            onReturnToStudio = onChangeVideo
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UploadHeader()

        VideoPreviewSection(filePath)

        Spacer(modifier = Modifier.height(16.dp))

        when (val current = state) {
            is UploadUiState.Idle -> {
                val formState = remember { UploadFormState(getNewUploadName(source)) }
                IdleFormSection(
                    state = formState,
                    onCancel = onChangeVideo,
                    onUpload = {
                        if (formState.isValid) {
                            viewModel.startMediaUpload(filePath, formState.title, formState.getMetadataMap())
                        } else {
                            Toast.makeText(context, "Please complete fields", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            is UploadUiState.Uploading, is UploadUiState.StateChange -> {
                UploadingSection(
                    state = current,
                    sourceName = source,
                    onPauseResume = { isPaused ->
                        if (isPaused) viewModel.pauseUpload() else viewModel.resumeUpload()
                    },
                    onCancel = { viewModel.cancelUpload() }
                )
            }

            is UploadUiState.Error -> ErrorSection(current.message, onBack = onChangeVideo)
            is UploadUiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            else -> {}
        }
    }
}

@Composable
fun UploadHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Upload Video",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 48.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
    }
}

@UnstableApi
@Composable
fun VideoPreviewSection(filePath: String) {
    val context = LocalContext.current
    val fastPixPlayer = remember(filePath) {
        FastPixPlayer.Builder(context).setLoop(false).setAutoplay(true).build().apply {
            setMediaItem(MediaItem.fromUri(filePath.toUri()))
        }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = fastPixPlayer.getExoPlayer() } },
        modifier = Modifier.fillMaxWidth().height(250.dp)
    )

    DisposableEffect(Unit) { onDispose { fastPixPlayer.release() } }
}

@Composable
fun TitleInput(value: String, onValueChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Media Title:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 100) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            )
        )
    }
}

@Composable
fun MetadataInputRow(
    fieldName: String,
    fieldValue: String,
    onNameChange: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text("Metadata:", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = fieldName,
                onValueChange = onNameChange,
                modifier = Modifier.weight(0.4f),
                placeholder = { Text("Field") },
                shape = RoundedCornerShape(24.dp)
            )
            OutlinedTextField(
                value = fieldValue,
                onValueChange = onValueChange,
                modifier = Modifier.weight(0.6f),
                placeholder = { Text("Value") },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun UploadButton(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun IdleFormSection(state: UploadFormState, onCancel: () -> Unit, onUpload: () -> Unit) {
    Column {
        TitleInput(state.title) { state.title = it }

        MetadataInputRow(
            fieldName = state.metaField,
            fieldValue = state.metaValue,
            onNameChange = { state.metaField = it },
            onValueChange = { state.metaValue = it }
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UploadButton( "Change", Modifier.weight(1f), Color.Transparent, onCancel)
            UploadButton("Upload", Modifier.weight(1f), MaterialTheme.colorScheme.primary, onUpload)
        }
    }
}

@Composable
fun UploadingSection(
    state: UploadUiState,
    sourceName: String,
    onPauseResume: (isPaused: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var progress by remember { mutableDoubleStateOf(0.0) }
    var isPaused by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        when(state) {
            is UploadUiState.Uploading -> { progress = state.percentage; isPaused = false }
            is UploadUiState.StateChange -> {
                isPaused = state.state == UploadState.PAUSED
                isOffline = state.state == UploadState.NETWORK_LOST
            }
            else -> {}
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Uploading from $sourceName...", style = MaterialTheme.typography.titleMedium)

        LinearProgressIndicator(
            progress = { (progress / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )

        if (isOffline) {
            OfflineWarning()
        } else {
            UploadControlButtons(isPaused, onPauseResume, onCancel)
        }
    }
}

@Composable
fun UploadControlButtons(
    isPaused: Boolean,
    onPauseResume: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.padding(vertical = 56.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = { onPauseResume(!isPaused) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, null)
            Spacer(Modifier.width(8.dp))
            Text(if (isPaused) "Resume" else "Pause")
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Close, null)
            Spacer(Modifier.width(8.dp))
            Text("Cancel")
        }
    }
}

@Composable
fun OfflineWarning() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 24.dp)) {
        Image(Icons.Filled.WifiOff, null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            "Network lost. Upload will resume automatically.",
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorSection(message: String, onBack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Image(
            painter = painterResource(R.drawable.error),
            contentDescription = null,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        UploadButton(text = "Go Back", onClick = onBack)
    }
}