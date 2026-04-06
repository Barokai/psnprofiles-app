package screens

import AppSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import components.renderGuideNodes
import kotlinx.coroutines.launch
import repository.GuideNode
import repository.GuideRepository

class GuideScreen(val guideUrl: String, val earnedTrophyNames: List<String>) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var guideNodes by remember { mutableStateOf<List<GuideNode>?>(null) }
        var localHideEarned by remember { mutableStateOf(AppSettings.hideEarnedTrophies) }
        var showTocMenu by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        
        val tocNode = guideNodes?.filterIsInstance<GuideNode.TableOfContents>()?.firstOrNull()
        
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
                },
                actions = {
                    if (tocNode != null && tocNode.items.isNotEmpty()) {
                        IconButton(onClick = { showTocMenu = !showTocMenu }) {
                            Icon(Icons.Default.List, contentDescription = "Table of Contents")
                        }
                        DropdownMenu(
                            expanded = showTocMenu,
                            onDismissRequest = { showTocMenu = false }
                        ) {
                            tocNode.items.forEach { tocTitle ->
                                DropdownMenuItem(onClick = { showTocMenu = false }) {
                                    Text(tocTitle, style = MaterialTheme.typography.body2)
                                }
                            }
                        }
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
                // Build a map: anchor -> lazy list index so roadmap cards can scroll to trophy sections
                val anchorIndexMap = remember(filteredNodes) {
                    val map = mutableMapOf<String, Int>()
                    var idx = 0
                    filteredNodes.forEach { node ->
                        if (node is GuideNode.TrophyGroup) {
                            // The anchor is the the section id: e.g. "5-i-watched-the-intro"
                            // TrophyGroup name: "I Watched the Intro" - match by normalizing
                            val normalizedName = node.name.trim().lowercase()
                                .replace(" ", "-")
                                .replace("'", "")
                                .replace("!", "")
                            map[normalizedName] = idx
                        }
                        idx++
                    }
                    map
                }
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    renderGuideNodes(filteredNodes) { anchor ->
                        // anchor looks like "5-i-watched-the-intro" - strip leading number
                        val nameOnly = anchor.substringAfter("-")
                        val matchKey = anchorIndexMap.keys.firstOrNull { it.contains(nameOnly) || nameOnly.contains(it.substringAfter("-")) }
                        val targetIdx = if (matchKey != null) anchorIndexMap[matchKey] else null
                        if (targetIdx != null) {
                            coroutineScope.launch { listState.animateScrollToItem(targetIdx) }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading native guide data...")
                }
            }
        }
    }
}
