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
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                guideNodes?.mapNotNull { node ->
                    when (node) {
                        is GuideNode.TrophyGroup -> {
                            val cleanName = node.name.split("-")[0].trim()
                            val isEarned = earnedTrophyNames.any { cleanName.contains(it) || it.contains(cleanName) } || node.isEarned
                            if (isEarned) null else node
                        }
                        is GuideNode.TableOfContents -> {
                            val filteredItems = node.items.filter { item ->
                                val cleanName = item.name.split("-")[0].trim()
                                val isEarned = earnedTrophyNames.any { cleanName.contains(it) || it.contains(cleanName) } || item.isEarned
                                !isEarned
                            }
                            if (filteredItems.isNotEmpty()) GuideNode.TableOfContents(filteredItems) else null
                        }
                        is GuideNode.RoadmapGrid -> {
                            val filteredTrophies = node.trophies.filter { item ->
                                val cleanName = item.name.split("-")[0].trim()
                                val isEarned = earnedTrophyNames.any { cleanName.contains(it) || it.contains(cleanName) } || item.isEarned
                                !isEarned
                            }
                            if (filteredTrophies.isNotEmpty()) GuideNode.RoadmapGrid(filteredTrophies) else null
                        }
                        // GuideInfoBox categories could be filtered here too, but let's keep it simple
                        else -> node
                    }
                }
            } else {
                guideNodes
            }
        }

        val docTitleNode = guideNodes?.firstOrNull { it is GuideNode.GuideTitle } as? GuideNode.GuideTitle
        val appBarTitle = docTitleNode?.title ?: "Native Trophy Guide"

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(
                    text = appBarTitle,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                ) },
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
                            tocNode.items.forEach { tocItem ->
                                // Optional filter inside dropdown if we wanted to mirror the main list, 
                                // but we already filtered `tocNode` locally so this represents the user's choice.
                                val rColor = try { Color(tocItem.colorHex.removePrefix("#").toLong(16) or 0xFF000000) } catch(e:Exception) { Color.Gray }
                                val txtColor = if (tocItem.isEarned) Color(0xFF4CAF50) else MaterialTheme.colors.onSurface
                                val txtStyle = if (tocItem.isEarned) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                DropdownMenuItem(onClick = { 
                                    showTocMenu = false
                                    // if we passed anchor click lambda here we could scroll to it!
                                }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(rColor, androidx.compose.foundation.shape.CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Text(tocItem.name, style = MaterialTheme.typography.body2.copy(textDecoration = txtStyle), color = txtColor)
                                    }
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
