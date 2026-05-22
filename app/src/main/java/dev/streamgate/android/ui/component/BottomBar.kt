package dev.streamgate.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import dev.streamgate.android.ui.screen.home.ScreenHome
import dev.streamgate.android.ui.screen.library.ScreenLibrary
import dev.streamgate.android.ui.screen.setting.ScreenSetting


@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    currentDestination: NavDestination? = null,
    onTabSelected: (route: Any) -> Unit
) {

    val bottomBarItems = listOf(
        BottomBarItem.Home,
        BottomBarItem.Library,
        BottomBarItem.Settings,
    )

    val selectedTabIndex by remember(currentDestination) {
        derivedStateOf {
            bottomBarItems.indexOfLast { currentDestination?.hasRoute(it.route::class) ?: false }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bottomBarItems.forEachIndexed { index, item ->
            BottomBarItem(
                item = item,
                selected = index == selectedTabIndex
            ) {
                onTabSelected(it)
            }
        }
    }
}


@Composable
fun BottomBarItem(
    modifier: Modifier = Modifier,
    item: BottomBarItem,
    selected: Boolean,
    onTabSelected: (route: Any) -> Unit
) {

    Column(
        modifier = modifier
            .width(96.dp)
            .clickable(interactionSource = null, indication = null) {
                onTabSelected(item.route)
            }
            .then(
                if (selected) Modifier.background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                else Modifier
            ).padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        val tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current

        Icon(
            imageVector = if (selected && item.selectedIcon != null) item.selectedIcon else item.icon,
            null,
            tint = tint
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }

}


sealed class BottomBarItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector?,
    val route: Any
) {

    object Home: BottomBarItem(
        label = "Home",
        icon = Icons.Outlined.CloudUpload,
        selectedIcon = Icons.Filled.CloudUpload,
        route = ScreenHome
    )

    object Library: BottomBarItem(
        label = "Library",
        icon = Icons.Outlined.VideoLibrary,
        selectedIcon = Icons.Filled.VideoLibrary,
        route = ScreenLibrary
    )

    object Settings: BottomBarItem(
        label = "Settings",
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
        route = ScreenSetting
    )
}