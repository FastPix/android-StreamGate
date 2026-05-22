package dev.streamgate.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import dagger.hilt.android.AndroidEntryPoint
import dev.streamgate.android.ui.navigation.AppNavigation
import dev.streamgate.android.ui.theme.StreamGateTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamGateTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}
