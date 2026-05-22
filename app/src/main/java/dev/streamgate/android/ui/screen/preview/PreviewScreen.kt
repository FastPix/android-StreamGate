package dev.streamgate.android.ui.screen.preview

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import io.fastpix.media3.core.FastPixPlayer
import kotlinx.serialization.Serializable

@Serializable
data class ScreenPreview(
    val mediaId: String
)

@OptIn(UnstableApi::class)
@Composable
fun PreviewScreen(
    mediaId: String,
) {

    val context = LocalContext.current
    val fastPixPlayer = remember(mediaId) {
        FastPixPlayer.Builder(context)
            .setLoop(false)
            .setAutoplay(true)
            .build()
    }

    fastPixPlayer.setFastPixMediaItem {
        playbackId = mediaId
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = fastPixPlayer.getExoPlayer()
            }
        },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            fastPixPlayer.release()
        }
    }
}