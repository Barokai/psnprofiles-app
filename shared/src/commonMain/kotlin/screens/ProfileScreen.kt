package screens

import ProfileOverview
import ProfileRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator

class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var profile by remember { mutableStateOf<ProfileOverview?>(null) }

        LaunchedEffect(Unit) {
            profile = ProfileRepository().fetchProfileOverview()
        }

        Column(Modifier.fillMaxWidth()) {
            TopAppBar(
                title = { Text(profile?.psnId ?: "Loading...") },
                actions = {
                    IconButton(onClick = { navigator?.push(SettingsScreen()) }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )

            if (profile != null) {
                LazyColumn {
                    items(profile!!.gamesList) { game ->
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { 
                            navigator?.push(GameTrophiesScreen(game.title, game.gameIdUrl))
                        }) {
                            Column(Modifier.weight(1f)) {
                                Text(game.title, style = MaterialTheme.typography.subtitle1)
                                Text(game.platform, style = MaterialTheme.typography.caption)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(game.progress, color = MaterialTheme.colors.primary)
                                Text(game.lastPlayed, style = MaterialTheme.typography.caption)
                            }
                        }
                    }
                }
            }
        }
    }
}
