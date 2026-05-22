package dev.streamgate.android.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.streamgate.android.R
import dev.streamgate.android.ui.component.BottomBar
import dev.streamgate.android.ui.navigation.BottomNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    startDestination: Any,
    onUpload: (source: String, filePath: String) -> Unit,
    onScreenRecord: () -> Unit,
    onCameraRecord: () -> Unit,
    onPreview: (mediaId: String) -> Unit,
) {

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(top = 8.dp)
                    .navigationBarsPadding(),
                currentDestination = currentDestination
            ) { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.startDestinationId) {saveState = true}
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxSize()
        ) {
            BottomNavGraph(
                navController = navController,
                startDestination = startDestination,
                onUpload = onUpload,
                onScreenRecord = onScreenRecord,
                onCameraRecord = onCameraRecord,
                onPreview = onPreview
            )
        }
    }
}
