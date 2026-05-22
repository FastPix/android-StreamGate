package dev.streamgate.android.ui.screen.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.streamgate.android.BuildConfig
import dev.streamgate.android.utils.APP_TERMS_URL
import kotlinx.serialization.Serializable


@Serializable
object ScreenSetting


@Composable
fun SettingScreen() {

    val uriHandler = LocalUriHandler.current

    val preferencesViewModel = hiltViewModel<PreferenceViewModel>()
    val frameRate by preferencesViewModel.frameRate.collectAsStateWithLifecycle()

    val recordingQualityOptions = listOf(30, 60)
    val selectedRecordingQuality by remember(frameRate) {
        derivedStateOf {
            val index = recordingQualityOptions.indexOf(frameRate)
            if (index == -1) 0 else index
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        )
        Spacer(Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.surface),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Recording Preferences",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Frame Rate (FPS)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Higher FPS means smoother video",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    SingleChoiceSegmentedButtonRow {
                        recordingQualityOptions.forEachIndexed { index, label ->
                            SegmentedButton(
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onBackground,
                                    inactiveContentColor = MaterialTheme.colorScheme.onBackground,
                                    inactiveContainerColor = Color.Transparent
                                ),
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = recordingQualityOptions.size,
                                    baseShape = RoundedCornerShape(12.dp)
                                ),
                                contentPadding = PaddingValues(vertical = 0.dp, horizontal = 0.dp),
                                onClick = {
                                    preferencesViewModel.updateFrameRate(recordingQualityOptions.getOrElse(index) { 30 })
                                  },
                                selected = index == selectedRecordingQuality,
                                label = {
                                    Text(
                                        text = label.toString(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                border = SegmentedButtonDefaults.borderStroke(color = MaterialTheme.colorScheme.surface, width = 1.dp),
                                icon = {}
                            )
                        }
                    }

                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.surface),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "About",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = null, indication = null) {
                            uriHandler.openUri(APP_TERMS_URL)
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable(interactionSource = null, indication = null) {}
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "About StreamGate",
                        style = MaterialTheme.typography.labelLarge
                    )

                    val version = BuildConfig.VERSION_NAME
                    Text(
                        text = "v$version",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
