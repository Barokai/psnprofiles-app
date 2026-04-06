package screens

import AppSettings
import NetworkClient
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.fleeksoft.ksoup.Ksoup
import components.SystemAppearance
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

data class TrophyRow(val name: String, val description: String, val earned: Boolean, val timestamp: String?, val imageUrl: String?, val rarity: String)

data class GameDetails(
    val avatarUrl: String?,
    val username: String?,
    val earnedCount: Int,
    val totalCount: Int,
    val rank: String?,
    val progressPercent: Int,

    val coverImageUrl: String?,
    val platforms: List<String>,
    val platCount: Int,
    val goldCount: Int,
    val silverCount: Int,
    val bronzeCount: Int,
    val totalTrophies: Int,
    val totalPoints: Int
)

class GameTrophiesScreen(private val gameTitle: String, private val gameHref: String) : Screen {
    @OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var trophies by remember { mutableStateOf<List<TrophyRow>?>(null) }
        var details by remember { mutableStateOf<GameDetails?>(null) }
        var guideHref by remember { mutableStateOf<String?>(null) }
        var localHideEarned by remember { mutableStateOf(AppSettings.hideEarnedTrophies) }
        var isRefreshing by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        SystemAppearance(MaterialTheme.colors.primary)
        
        suspend fun fetchGameData() {
            isRefreshing = true
            val response = NetworkClient.client.get("https://psnprofiles.com$gameHref")
            val doc = Ksoup.parse(response.bodyAsText())
            
            val guideNode = doc.selectFirst("a[href^=/guide/]")
            if (guideNode != null) {
                guideHref = "https://psnprofiles.com" + guideNode.attr("href")
            }

            val tempTrophies = mutableListOf<TrophyRow>()
            val rows = doc.select("table.zebra tr")
            for (row in rows) {
                val titleNode = row.selectFirst("a.title")
                if (titleNode != null) {
                    val name = titleNode.text()
                    val desc = row.selectFirst("td")?.text()?.substringAfter(name)?.trim() ?: "" 
                    val definitelyEarned = row.hasClass("completed")
                    val timestamp = if (definitelyEarned) row.selectFirst(".typo-top-date nobr")?.text()?.trim() else null
                    
                    var imageUrl = row.selectFirst("picture.trophy img, img.trophy")?.attr("src") ?: ""
                    if (imageUrl.startsWith("/")) imageUrl = "https://psnprofiles.com$imageUrl"
                    
                    val rarityNode = row.selectFirst("img[title=Bronze], img[title=Silver], img[title=Gold], img[title=Platinum]")
                    val rarityStr = rarityNode?.attr("title") ?: "Unknown"

                    tempTrophies.add(TrophyRow(name, desc, definitelyEarned, timestamp, imageUrl, rarityStr))
                }
            }
            trophies = tempTrophies

            var progAvatarUrl: String? = null
            var progUsername: String? = null
            var progEarnedCount = 0
            var progTotalCount = 0
            var progRank: String? = null
            var progPercent = 0
            
            val progTable = doc.selectFirst("table.box")
            if (progTable != null) {
                progAvatarUrl = progTable.selectFirst("img.trophy")?.attr("src")?.let { 
                    if (it.startsWith("/")) "https://psnprofiles.com$it" else it 
                }
                progUsername = progTable.selectFirst("a.title")?.text()
                val infoText = progTable.select("span.small-info b").map { it.text().trim() }
                if (infoText.size >= 2) {
                    progEarnedCount = infoText[0].toIntOrNull() ?: 0
                    progTotalCount = infoText[1].toIntOrNull() ?: 0
                }
                progRank = progTable.select("span[class^=game-rank]").text()
                progPercent = progTable.selectFirst(".progress-bar span")?.text()?.removeSuffix("%")?.toIntOrNull() ?: 0
            }

            var coverImgUrl: String? = null
            var plats = listOf<String>()
            var pCount = 0
            var gCount = 0
            var sCount = 0
            var bCount = 0
            var tTrophies = 0
            var tPoints = 0
            
            val gameImgHolder = doc.selectFirst(".game-image-holder")
            if (gameImgHolder != null) {
                coverImgUrl = gameImgHolder.selectFirst("picture.game img")?.attr("src")?.let {
                    if (it.startsWith("/")) "https://psnprofiles.com$it" else it
                }
                val statTable = gameImgHolder.nextElementSibling()
                if (statTable != null) {
                    plats = statTable.select("span.platform").map { it.text().trim() }
                    pCount = statTable.selectFirst("li.platinum")?.text()?.toIntOrNull() ?: 0
                    gCount = statTable.selectFirst("li.gold")?.text()?.toIntOrNull() ?: 0
                    sCount = statTable.selectFirst("li.silver")?.text()?.toIntOrNull() ?: 0
                    bCount = statTable.selectFirst("li.bronze")?.text()?.toIntOrNull() ?: 0
                    val bTags = statTable.select("span.small-info b").map { it.text().replace(",", "") }
                    if (bTags.size >= 2) {
                        tTrophies = bTags[0].toIntOrNull() ?: 0
                        tPoints = bTags[1].toIntOrNull() ?: 0
                    }
                }
            }
            details = GameDetails(progAvatarUrl, progUsername, progEarnedCount, progTotalCount, progRank, progPercent, coverImgUrl, plats, pCount, gCount, sCount, bCount, tTrophies, tPoints)
            isRefreshing = false
        }

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = { coroutineScope.launch { fetchGameData() } }
        )

        LaunchedEffect(Unit) {
            fetchGameData()
        }
        
        val filteredTrophies = if (localHideEarned) trophies?.filter { !it.earned } else trophies

        Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { Text(gameTitle) },
                    navigationIcon = {
                        IconButton(onClick = { navigator?.pop() }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                
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
                
                if (filteredTrophies != null) {
                    if (guideHref != null) {
                        Button(
                            onClick = {
                                navigator?.push(GuideScreen(guideHref!!, trophies!!.filter { it.earned }.map { it.name }))
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text("View Trophy Guide")
                        }
                    }

                    LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                    item {
                        details?.let { info ->
                            // Cover Image and Stats
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                elevation = 2.dp,
                                backgroundColor = Color.White
                            ) {
                                Column {
                                    if (info.coverImageUrl != null) {
                                        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                            KamelImage(
                                                resource = asyncPainterResource(data = info.coverImageUrl),
                                                contentDescription = "Game Cover",
                                                modifier = Modifier.fillMaxWidth(),
                                                contentScale = ContentScale.FillWidth
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left Side: Platforms
                                        Row {
                                            info.platforms.forEach { platform ->
                                                Box(
                                                    modifier = Modifier.background(Color.White, RoundedCornerShape(4.dp))
                                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(platform, style = MaterialTheme.typography.caption, color = Color.DarkGray)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                        }
                                        // Right Side: Trophies
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (info.platCount > 0) {
                                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFB0BEC5), CircleShape))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${info.platCount}", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }
                                                Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFD700), CircleShape))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${info.goldCount}", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.size(10.dp).background(Color(0xFFC0C0C0), CircleShape))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${info.silverCount}", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.size(10.dp).background(Color(0xFFCD7F32), CircleShape))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("${info.bronzeCount}", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold, color = Color.Gray)
                                            }
                                            Text("${info.totalTrophies} Trophies • ${info.totalPoints} Points", style = MaterialTheme.typography.caption, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            // Progress Bar
                            if (info.username != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    elevation = 2.dp,
                                    backgroundColor = Color(0xFFE8F0FE) // Light blueish background similar to screenshot
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (info.avatarUrl != null) {
                                            KamelImage(
                                                resource = asyncPainterResource(data = info.avatarUrl),
                                                contentDescription = "Avatar",
                                                modifier = Modifier.size(50.dp).clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(info.username ?: "", style = MaterialTheme.typography.subtitle1, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                                            Text("${info.earnedCount} of ${info.totalCount} Trophies", style = MaterialTheme.typography.caption, color = Color.Gray)
                                        }
                                        if (info.rank != null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
                                                Text(info.rank, style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                Text("RANK", style = MaterialTheme.typography.overline, color = Color.Gray)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(100.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🏆", style = MaterialTheme.typography.caption)
                                            }
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(14.dp).background(Color(0xFFBBDEFB)),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Box(modifier = Modifier.fillMaxWidth(info.progressPercent / 100f).fillMaxHeight().background(Color(0xFF1976D2)))
                                                Text("${info.progressPercent}%", style = MaterialTheme.typography.overline, color = Color.White, modifier = Modifier.padding(start = 4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(filteredTrophies) { trophy ->
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (trophy.imageUrl != null && trophy.imageUrl.startsWith("http")) {
                                KamelImage(
                                    resource = asyncPainterResource(data = trophy.imageUrl),
                                    contentDescription = "Trophy Image",
                                    modifier = Modifier.size(50.dp).padding(end = 12.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trophy.name, style = MaterialTheme.typography.subtitle1, color = if (trophy.earned) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    val rColor = when (trophy.rarity) {
                                        "Bronze" -> Color(0xFFCD7F32)
                                        "Silver" -> Color(0xFFC0C0C0)
                                        "Gold" -> Color(0xFFFFD700)
                                        "Platinum" -> Color(0xFFE5E4E2)
                                        else -> Color.Transparent
                                    }
                                    if (rColor != Color.Transparent) {
                                        Box(modifier = Modifier.size(10.dp).background(rColor, shape = CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(trophy.description, style = MaterialTheme.typography.caption)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (trophy.earned) "EARNED" else "Lock",
                                    color = if (trophy.earned) Color(0xFF4CAF50) else Color.Gray,
                                    style = MaterialTheme.typography.overline
                                )
                                if (trophy.timestamp != null) {
                                    Text(
                                        text = trophy.timestamp, 
                                        style = MaterialTheme.typography.caption,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        Divider(startIndent = 16.dp)
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Loading interactive interface...")
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
}
}
