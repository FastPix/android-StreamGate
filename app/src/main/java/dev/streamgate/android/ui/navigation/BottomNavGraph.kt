package dev.streamgate.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.streamgate.android.ui.screen.home.HomeScreen
import dev.streamgate.android.ui.screen.home.ScreenHome
import dev.streamgate.android.ui.screen.library.LibraryScreen
import dev.streamgate.android.ui.screen.library.ScreenLibrary
import dev.streamgate.android.ui.screen.setting.ScreenSetting
import dev.streamgate.android.ui.screen.setting.SettingScreen
import kotlinx.serialization.Serializable

@Serializable
object GraphNavigation

@Composable
fun BottomNavGraph(
    navController: NavHostController,
    startDestination: Any,
    onUpload: (source: String, filePath: String) -> Unit,
    onScreenRecord: () -> Unit,
    onCameraRecord: () -> Unit,
    onPreview: (mediaId: String) -> Unit,
) {

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<ScreenHome> {
            HomeScreen(
                onUpload = onUpload,
                onScreenRecord = onScreenRecord,
                onCameraRecord = onCameraRecord
            )
        }

        composable<ScreenLibrary> {
            LibraryScreen(onPreview = onPreview)
        }

        composable<ScreenSetting> {
            SettingScreen()
        }
    }

}