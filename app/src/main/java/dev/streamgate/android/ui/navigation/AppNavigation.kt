package dev.streamgate.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.streamgate.android.ui.screen.MainScreen
import dev.streamgate.android.ui.screen.camera.CameraScreen
import dev.streamgate.android.ui.screen.camera.ScreenCamera
import dev.streamgate.android.ui.screen.home.ScreenHome
import dev.streamgate.android.ui.screen.preview.PreviewScreen
import dev.streamgate.android.ui.screen.preview.ScreenPreview
import dev.streamgate.android.ui.screen.screen_record.RecordScreen
import dev.streamgate.android.ui.screen.screen_record.ScreenRecord
import dev.streamgate.android.ui.screen.upload.ScreenUpload
import dev.streamgate.android.ui.screen.upload.UploadScreen
import dev.streamgate.android.ui.screen.upload.UploadViewModel

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val uploadsViewModel = hiltViewModel<UploadViewModel>()

    NavHost(
        navController = navController,
        startDestination = GraphNavigation,
    ) {
        composable<GraphNavigation> {
            MainScreen(
                startDestination = ScreenHome,
                onUpload = { source, filePath ->
                    navController.navigate(ScreenUpload(source, filePath))
                },
                onScreenRecord = {
                    navController.navigate(ScreenRecord)
                },
                onCameraRecord = {
                    navController.navigate(ScreenCamera)
                },
                onPreview = {
                    navController.navigate(ScreenPreview(it))
                }
            )
        }

        composable<ScreenUpload> {
            val screenUpload: ScreenUpload = it.toRoute()
            UploadScreen(
                screenUpload.source, screenUpload.filePath,
                onPreview = { mediaId ->
                    navController.navigate(ScreenPreview(mediaId))
                }, onChangeVideo = { navController.popBackStack() }
            )
        }

        composable<ScreenCamera> {
            CameraScreen { source, filePath ->
                navController.navigate(ScreenUpload(source, filePath))
            }
        }

        composable<ScreenRecord> {
            RecordScreen { source, filePath ->
                navController.navigate(ScreenUpload(source, filePath)) {
                    launchSingleTop = true
                }
            }
        }

        composable<ScreenPreview>{
            val screenPreview: ScreenPreview = it.toRoute()
            PreviewScreen(
                mediaId = screenPreview.mediaId
            )
        }
    }

}