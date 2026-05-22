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
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isPaused by remember { mutableStateOf(false) }
    val progress = remember { mutableDoubleStateOf(0.0) }
    var offline by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetUiState()
    }

    val uri: Uri = filePath.toUri()
    val videoName = remember {
        mutableStateOf(getNewUploadName(source))
    }

    val metaFieldName = remember { mutableStateOf("") }
    val metaFieldValue = remember { mutableStateOf("") }

    val fastPixPlayer = remember(filePath) {
        FastPixPlayer.Builder(context)
            .setLoop(false)
            .setAutoplay(true)
            .build()
    }

    fastPixPlayer.setMediaItem(MediaItem.fromUri(uri))

    if (state !is UploadUiState.Success) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .background(color = MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Upload Video",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 48.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = fastPixPlayer.getExoPlayer()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(250.dp)
            )

            DisposableEffect(Unit) {
                onDispose {
                    fastPixPlayer.release()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val current = state) {
                is UploadUiState.Idle -> {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Media Title:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedTextField(
                                enabled = true,
                                value = videoName.value,
                                onValueChange = {
                                    if (it.length <= 100) {
                                        videoName.value = it
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.labelLarge,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                maxLines = 1,
                                placeholder = { Text("Change Video Title") },
                            )

                            Text(
                                text = videoName.value,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Metadata:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = metaFieldName.value,
                                    onValueChange = { input ->
                                        if (input.length <= 15) {
                                            metaFieldName.value = input
                                        }
                                    },
                                    modifier = Modifier.weight(0.4f),
                                    textStyle = MaterialTheme.typography.labelLarge,
                                    placeholder = { Text("Field") },
                                    singleLine = true,
                                    maxLines = 1,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                )

                                OutlinedTextField(
                                    value = metaFieldValue.value,
                                    onValueChange = { metaFieldValue.value = it },
                                    modifier = Modifier.weight(0.6f),
                                    textStyle = MaterialTheme.typography.labelLarge,
                                    placeholder = { Text("Value") },
                                    singleLine = true,
                                    maxLines = 1,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = { onChangeVideo() },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onBackground,
                                )
                            ) {
                                Text(
                                    text = "Change",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (videoName.value.isBlank()) {
                                        Toast.makeText(context, "Please enter a video title", Toast.LENGTH_SHORT).show()
                                    } else if (metaFieldName.value.isNotBlank() && metaFieldValue.value.isBlank() ||
                                        metaFieldName.value.isBlank() && metaFieldValue.value.isNotBlank()) {
                                        Toast.makeText(context, "Please complete both metadata field and value or leave both empty", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val metadata = if (metaFieldName.value.isNotBlank() && metaFieldValue.value.isNotBlank()) {
                                            mapOf(metaFieldName.value to metaFieldValue.value)
                                        } else null
                                        viewModel.startMediaUpload(filePath, title = videoName.value, metadata=metadata)
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onBackground,
                                )
                            ) {
                                Text(
                                    text = "Upload",
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }

                is UploadUiState.Loading -> {
                    LinearProgressIndicator(
                        trackColor = MaterialTheme.colorScheme.tertiary,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is UploadUiState.Uploading, is UploadUiState.StateChange -> {
                    val uploadingSource = when(source) {
                        VideoSource.DEVICE.name -> "your On Device Video"
                        VideoSource.CAMERA.name -> "your recorded Video"
                        VideoSource.SCREEN_RECORD.name -> "your Screen Recording Video"
                        else -> "Video"
                    }

                    when (current) {
                        is UploadUiState.Uploading -> {
                            progress.doubleValue = current.percentage
                            isPaused = false
                        }

                        is UploadUiState.StateChange -> {
                            when(current.state) {
                                UploadState.PAUSED -> isPaused = true
                                UploadState.UPLOADING -> {
                                    isPaused = false
                                    offline = false
                                }
                                UploadState.RETRYING -> { /* Could show a retrying status if desired */ }
                                UploadState.NETWORK_LOST -> {
                                    offline = true
                                    Toast.makeText(context, "Network connection lost. Upload will resume when connection is restored.", Toast.LENGTH_LONG).show()
                                }
                                else -> { /* No UI change needed for other states */ }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Please wait! Uploading $uploadingSource...",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        LinearProgressIndicator(
                            progress = { (progress.doubleValue / 100.0).toFloat() },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            trackColor = MaterialTheme.colorScheme.tertiary,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${String.format(Locale.ENGLISH, "%.1f", progress.doubleValue)}%",
                            style = MaterialTheme.typography.labelMedium,
                        )

                        if (offline) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Image(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Network connection lost. Upload will resume when connection is restored.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        } else {

                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 56.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {

                                Button(
                                    onClick = {
                                        isPaused = !isPaused
                                        if (isPaused) {
                                            viewModel.pauseUpload()
                                        } else {
                                            viewModel.resumeUpload()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (isPaused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = if (isPaused) "Resume" else "Pause")
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.cancelUpload()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Cancel")
                                }
                            }
                        }
                    }
                }
                is UploadUiState.Error -> {
                    Column(
                        modifier = Modifier.padding(top = 48.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.error),
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.CenterHorizontally),

                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        current.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        modifier = Modifier.padding(vertical = 16.dp),
                        onClick = { onChangeVideo() },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBackIosNew,
                            contentDescription = null,
                        )

                        Text(
                            text = "Go Back",
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    } else {
        UploadSuccessScreen(
            current = state as UploadUiState.Success,
            viewModel = viewModel,
            onPreview = onPreview,
            onReturnToStudio = { onChangeVideo() }
        )
    }
}

@Composable
fun UploadSuccessScreen(
    current: UploadUiState.Success,
    viewModel: UploadViewModel,
    onPreview: (mediaId: String) -> Unit,
    onReturnToStudio: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var videoPreviewLink by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var mediaId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(current.uploadId) {
        isLoading = true

        while (mediaId == null && errorMessage == null) {
            val (success, result, mediaStatus) = viewModel.getMediaId(current.uploadId)
            status = mediaStatus
            if (success) {
                if (result != null) {
                    mediaId = result
                    videoPreviewLink = VIDEO_SHARE_URL.format(result)
                    isLoading = false
                }
            } else {
                errorMessage = "Error: $result"
                isLoading = false
            }

            if (mediaId == null && errorMessage == null) {
                delay(1000)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StreamGateDark)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StreamGateCard, RoundedCornerShape(16.dp))
                .border(1.dp, StreamGateBorder, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(StreamGateGreen.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(StreamGateGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Video Ready to Share!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = errorMessage ?: "Your high-fidelity stream is processed and\nlive on StreamGate.",
                color = if (errorMessage != null) Color.Yellow else Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .border(1.dp, StreamGateBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = videoPreviewLink ?: if (isLoading) status ?: "Generating link... Please wait!" else "Link unavailable",
                    color = if (videoPreviewLink != null) Color.White else Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        videoPreviewLink?.let {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = ClipData.newPlainText("Video Link", it)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StreamGateBorder))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Link", fontWeight = FontWeight.Medium)
                }

                OutlinedButton(
                    onClick = { mediaId?.let { onPreview(it) } ?: videoPreviewLink?.let { uriHandler.openUri(it) } },
                    enabled = videoPreviewLink != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(StreamGateBorder))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preview", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    videoPreviewLink?.let {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, it)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                    }
                },
                enabled = videoPreviewLink != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StreamGateAccentOrange,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share via...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onReturnToStudio) {
                Text(
                    text = "Return to Uploads Screen",
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

