package screens

import AppSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import components.renderGuideNodes
import repository.GuideNode
import repository.GuideRepository

class GuideScreen(val guideUrl: String, val earnedTrophyNames: List<String>) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var guideNodes by remember { mutableStateOf<List<GuideNode>?>(null) }
        var localHideEarned by remember { mutableStateOf(AppSettings.hideEarnedTrophies) }
        
        LaunchedEffect(Unit) {
            guideNodes = GuideRepository.fetchGuide(guideUrl)
        }

        // Native offline-ready data filtering mapped precisely to the local toggle
        val filteredNodes = remember(guideNodes, localHideEarned) {
            if (localHideEarned) {
                guideNodes?.filter { node ->
                    if (node is GuideNode.TrophyGroup) {
                        val cleanName = node.name.split("-")[0].trim()
                        !earnedTrophyNames.any { cleanName.contains(it) || it.contains(cleanName) }
                    } else {
                        true
                    }
                }
            } else {
                guideNodes
            }
        }

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Native Trophy Guide") },
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
            
            // Toggle controller dynamically localizing the hide logic physically on screen
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hide Earned Trophies", style = MaterialTheme.typography.subtitle2)
                Switch(
                    checked = localHideEarned,
                    onCheckedChange = { localHideEarned = it }
                )
            }
            
            Divider()

            if (filteredNodes != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    renderGuideNodes(filteredNodes)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading native guide data...")
                }
            }
        }
    }
}
