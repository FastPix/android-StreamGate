package dev.streamgate.android.ui.screen.upload

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.streamgate.android.ui.theme.StreamGateAccentOrange
import dev.streamgate.android.ui.theme.StreamGateBorder
import dev.streamgate.android.ui.theme.StreamGateCard
import dev.streamgate.android.ui.theme.StreamGateDark
import dev.streamgate.android.ui.theme.StreamGateGreen
import dev.streamgate.android.utils.VIDEO_SHARE_URL
import kotlinx.coroutines.delay

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
            if (success && result != null) {
                mediaId = result
                videoPreviewLink = VIDEO_SHARE_URL.format(result)
                isLoading = false
            } else if (!success) {
                errorMessage = "Error: $result"
                isLoading = false
            }
            if (mediaId == null && errorMessage == null) delay(1000)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(StreamGateDark).padding(16.dp),
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
            SuccessCheckmark()

            SuccessInfoText(errorMessage)

            Spacer(modifier = Modifier.height(24.dp))

            LinkDisplayArea(
                link = videoPreviewLink,
                isLoading = isLoading,
                status = status
            )

            Spacer(modifier = Modifier.height(16.dp))

            SuccessActionButtons(
                context = context,
                link = videoPreviewLink,
                mediaId = mediaId,
                onPreview = onPreview,
                uriHandler = uriHandler
            )

            Spacer(modifier = Modifier.height(16.dp))

            ShareViaButton(context, videoPreviewLink)

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onReturnToStudio) {
                Text("Return to Uploads Screen", color = Color.Gray)
            }
        }
    }
}

@Composable
fun LinkDisplayArea(link: String?, isLoading: Boolean, status: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .border(1.dp, StreamGateBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.PlayArrow, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = link ?: if (isLoading) status ?: "Generating link..." else "Link unavailable",
            color = if (link != null) Color.White else Color.Gray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SuccessCheckmark() {
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
}

@Composable
fun SuccessInfoText(errorMessage: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    }
}

@Composable
fun ShareViaButton(context: android.content.Context, link: String?) {
    Button(
        onClick = {
            link?.let {
                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    putExtra(android.content.Intent.EXTRA_TEXT, it)
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share via"))
            }
        },
        enabled = link != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = StreamGateAccentOrange,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Share via...",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SuccessActionButtons(
    context: android.content.Context,
    link: String?,
    mediaId: String?,
    onPreview: (String) -> Unit,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedSuccessButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.ContentCopy,
            text = "Copy Link",
            onClick = {
                link?.let {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Video Link", it))
                    Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                }
            }
        )

        OutlinedSuccessButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.PlayArrow,
            text = "Preview",
            enabled = link != null,
            onClick = { mediaId?.let { onPreview(it) } ?: link?.let { uriHandler.openUri(it) } }
        )
    }
}

@Composable
fun OutlinedSuccessButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = BorderStroke(1.dp, StreamGateBorder)
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}