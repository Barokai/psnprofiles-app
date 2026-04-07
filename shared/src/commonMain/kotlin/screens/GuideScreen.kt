package screens

import AppSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import components.renderGuideNodes
import components.SystemAppearance
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import repository.GuideNode
import repository.GuideRepository

class GuideScreen(val guideUrl: String, val earnedTrophyNames: List<String>) : Screen {
    @OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var guideNodes by remember { mutableStateOf<List<GuideNode>?>(null) }
        var localHideEarned by remember { mutableStateOf(AppSettings.hideEarnedTrophies) }
        var showTocMenu by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var isRefreshing by remember { mutableStateOf(false) }
        var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }

        SystemAppearance(MaterialTheme.colors.primary)
        
        suspend fun fetchGuideData() {
            isRefreshing = true
            guideNodes = GuideRepository.fetchGuide(guideUrl)
            isRefreshing = false
        }

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { coroutineScope.launch { fetchGuideData() } }
        )

        LaunchedEffect(Unit) {
            fetchGuideData()
        }

        // Native offline-ready data filtering mapped precisely to the local toggle
        val filteredNodes = remember(guideNodes, localHideEarned) {
            fun filterNodes(nodes: List<GuideNode>): List<GuideNode> {
                return nodes.mapNotNull { node ->
                    when (node) {
                        is GuideNode.TrophyGroup -> {
                            val cleanName = node.name.split("-")[0].trim()
                            val isEarned = earnedTrophyNames.any { cleanName.contains(it) || it.contains(cleanName) } || node.isEarned
                            if (isEarned) null else node.copy(details = filterNodes(node.details))
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
                        is GuideNode.GuideInfoBox -> {
                            val filteredCategories = node.categories.mapNotNull { cat ->
                                val filteredTrophies = cat.trophies.filter { item ->
                                    val cleanName = item.name.split("-")[0].trim()
                                    val isEarned = earnedTrophyNames.any { cleanName.contains(it) || it.contains(cleanName) } || item.isEarned
                                    !isEarned
                                }
                                if (filteredTrophies.isNotEmpty()) cat.copy(trophies = filteredTrophies) else null
                            }
                            node.copy(categories = filteredCategories)
                        }
                        is GuideNode.GenericBox -> {
                            val filteredDetails = filterNodes(node.details)
                            if (filteredDetails.isNotEmpty()) GuideNode.GenericBox(filteredDetails) else null
                        }
                        else -> node
                    }
                }
            }

            val currentNodes = guideNodes
            if (localHideEarned && currentNodes != null) {
                filterNodes(currentNodes)
            } else {
                currentNodes
            }
        }

        val docTitleNode = guideNodes?.firstOrNull { it is GuideNode.GuideTitle } as? GuideNode.GuideTitle
        val appBarTitle = docTitleNode?.title ?: "Native Trophy Guide"
        val tocNode = filteredNodes?.firstOrNull { it is GuideNode.TableOfContents } as? GuideNode.TableOfContents

        // Build a map: anchor -> lazy list index so roadmap cards can scroll to trophy sections
        val anchorIndexMap = remember(filteredNodes) {
            val map = mutableMapOf<String, Int>()
            var idx = 0
            filteredNodes?.forEach { node ->
                if (node !is GuideNode.TableOfContents) {
                    when (node) {
                        is GuideNode.TrophyGroup -> if (!map.containsKey(node.anchorId)) map[node.anchorId] = idx
                        is GuideNode.SectionHeader -> if (!map.containsKey(node.anchorId)) map[node.anchorId] = idx
                        is GuideNode.GuideInfoBox -> if (!map.containsKey(node.anchorId)) map[node.anchorId] = idx
                        else -> {}
                    }
                    idx++
                }
            }
            map
        }

        val performScroll: (String) -> Unit = { anchor ->
            val targetIdx = anchorIndexMap[anchor]
            if (targetIdx != null) {
                coroutineScope.launch { listState.animateScrollToItem(targetIdx) }
            }
        }

        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
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
                                    performScroll(tocItem.anchor)
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
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        renderGuideNodes(
                            nodes = filteredNodes, 
                            onAnchorClick = { anchor -> performScroll(anchor) },
                            onImageClick = { url -> fullscreenImageUrl = url }
                        )
                    }

                    // Floating scroll to top button
                    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
                    if (showButton) {
                        androidx.compose.material.FloatingActionButton(
                            onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).padding(bottom = 32.dp),
                            backgroundColor = MaterialTheme.colors.primary
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top", tint = Color.White)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading native guide data...")
                }
            }
        }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Fullscreen Image Overlay (Lightbox)
        if (fullscreenImageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { fullscreenImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                KamelImage(
                    resource = asyncPainterResource(data = fullscreenImageUrl!!),
                    contentDescription = "Fullscreen View",
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentScale = ContentScale.Fit,
                    onFailure = { Text("Failed to load full image", color = Color.White) },
                    onLoading = { CircularProgressIndicator(color = Color.White) }
                )
                
                // Close indicator hint
                Text(
                    text = "Tap to close",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.overline,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
                )
            }
        }
    }
    }
}
