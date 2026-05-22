package dev.streamgate.android.ui.screen.library

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.streamgate.android.data.remote.model.response.DataPayload
import dev.streamgate.android.ui.screen.upload.ApiResult
import dev.streamgate.android.ui.screen.upload.UploadViewModel
import dev.streamgate.android.utils.VIDEO_SHARE_URL
import kotlinx.serialization.Serializable

@Serializable
object ScreenLibrary

@Composable
fun LibraryScreen(
    onPreview: (mediaId: String) -> Unit,
) {

    val uploadsViewModel = hiltViewModel<UploadViewModel>()
    val mediaList by uploadsViewModel.mediaState.collectAsStateWithLifecycle()
    val isRefreshing = mediaList is ApiResult.Loading

    LaunchedEffect(Unit) {
        uploadsViewModel.refreshMedia()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { uploadsViewModel.refreshMedia() },
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = mediaList) {
            is ApiResult.Success -> {
                if (state.data.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No media found. Pull down to refresh.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.data) { item ->
                            MediaUpload(
                                dataPayload = item,
                                onPreview = onPreview
                            )
                        }
                    }
                }
            }
            is ApiResult.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to load media. Please try again.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { uploadsViewModel.refreshMedia() }) {
                        Text(text = "Retry")
                    }
                }
            }
            is ApiResult.Loading -> {
                CircularProgressIndicator()
            }
        }
    }
}


@Composable
fun MediaUpload(
    modifier: Modifier = Modifier,
    dataPayload: DataPayload,
    onPreview: (mediaId: String) -> Unit = {}
) {

    val context = LocalContext.current
    val mediaId = dataPayload.playbackIds.firstOrNull()?.id

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            .clickable(interactionSource = null, indication = null) {
                onPreview(mediaId ?: "")
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = dataPayload.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dataPayload.title ?: "StreamGate Media",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                Column {
                    Text(
                        text = "${dataPayload.mediaQuality.capitalize(Locale.current)} ${dataPayload.maxResolution}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: ${dataPayload.status}",
                        style = MaterialTheme.typography.labelSmall)
                }

                mediaId?.let {
                    val videoPreviewLink = VIDEO_SHARE_URL.format(it)
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = ClipData.newPlainText("Video Link", videoPreviewLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = Color.White),
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

}