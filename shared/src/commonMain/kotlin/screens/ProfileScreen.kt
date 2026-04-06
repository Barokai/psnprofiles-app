package screens

import ProfileOverview
import ProfileRepository
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

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
                // Synthesize the extracted ul.profile-bar explicitly over the Game List
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(profile!!.barStats) { stat ->
                        Text(
                            text = stat, 
                            style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
                Divider()

                LazyColumn {
                    items(profile!!.gamesList) { game ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { 
                            navigator?.push(GameTrophiesScreen(game.title, game.gameIdUrl))
                        }.padding(16.dp)) {
                            
                            // Safe Image Load preventing Ktor Malformed Exceptions
                            if (game.imageUrl.startsWith("http")) {
                                KamelImage(
                                    resource = asyncPainterResource(data = game.imageUrl),
                                    contentDescription = "Game Image",
                                    modifier = Modifier.size(60.dp).padding(end = 16.dp)
                                )
                            }
                            
                            Column(Modifier.weight(1f)) {
                                Text(game.title, style = MaterialTheme.typography.subtitle1)
                                Text(game.platform, style = MaterialTheme.typography.caption)
                                if (game.earnedTrophies.isNotEmpty() && game.totalTrophies.isNotEmpty()) {
                                    Text(
                                        "${game.earnedTrophies} of ${game.totalTrophies} Trophies", 
                                        style = MaterialTheme.typography.caption,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(game.progress, color = MaterialTheme.colors.primary)
                                Text(game.lastPlayed, style = MaterialTheme.typography.caption)
                            }
                        }
                        Divider(startIndent = 16.dp)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Loading native ecosystem data...")
                }
            }
        }
    }
}
