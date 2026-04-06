package screens

import ProfileOverview
import ProfileRepository
import components.SystemAppearance
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

class ProfileScreen : Screen {
    @OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var profile by remember { mutableStateOf<ProfileOverview?>(null) }
        var isRefreshing by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        SystemAppearance(MaterialTheme.colors.primary)

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    profile = ProfileRepository().fetchProfileOverview()
                    isRefreshing = false
                }
            }
        )

        LaunchedEffect(Unit) {
            isRefreshing = true
            profile = ProfileRepository().fetchProfileOverview()
            isRefreshing = false
        }

        SystemAppearance(MaterialTheme.colors.primary)

        Column(Modifier.fillMaxWidth()) {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (profile?.avatarUrl != null) {
                            KamelImage(
                                resource = asyncPainterResource(data = profile!!.avatarUrl!!),
                                contentDescription = "Avatar",
                                modifier = Modifier.size(32.dp).clip(CircleShape).padding(end = 8.dp)
                            )
                        }
                        Text(profile?.psnId ?: "Loading...") 
                    }
                },
                actions = {
                    IconButton(onClick = { navigator?.push(UpdateProfileScreen()) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Update Profile")
                    }
                    IconButton(onClick = { navigator?.push(SettingsScreen()) }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )

            if (profile != null) {
                Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Synthesize the extracted ul.profile-bar explicitly over the Game List
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(profile!!.barStats) { stat ->
                                val color = when {
                                    stat.startsWith("Plat:") -> Color(0xFFB0BEC5)
                                    stat.startsWith("Gold:") -> Color(0xFFFFD700)
                                    stat.startsWith("Silver:") -> Color(0xFFC0C0C0)
                                    stat.startsWith("Bronze:") -> Color(0xFFCD7F32)
                                    else -> Color.Transparent
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (color != Color.Transparent) {
                                        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = stat, 
                                        style = MaterialTheme.typography.subtitle2,
                                        color = if (color != Color.Transparent) Color.DarkGray else MaterialTheme.colors.primary
                                    )
                                }
                            }
                        }
                        Divider()

                        LazyColumn(modifier = Modifier.weight(1f), state = listState) {
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
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = listState.firstVisibleItemIndex > 2,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { 50 }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { 50 }),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            FloatingActionButton(
                                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                                backgroundColor = MaterialTheme.colors.primary,
                                contentColor = Color.White
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Loading native ecosystem data...")
                }
            }
        }
    }
}
