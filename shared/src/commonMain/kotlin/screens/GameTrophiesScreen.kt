package screens

import AppSettings
import NetworkClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.fleeksoft.ksoup.Ksoup
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

data class TrophyRow(val name: String, val description: String, val earned: Boolean, val timestamp: String?, val imageUrl: String?, val rarity: String)

class GameTrophiesScreen(private val gameTitle: String, private val gameHref: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var trophies by remember { mutableStateOf<List<TrophyRow>?>(null) }
        var guideHref by remember { mutableStateOf<String?>(null) }
        var localHideEarned by remember { mutableStateOf(AppSettings.hideEarnedTrophies) }
        
        LaunchedEffect(Unit) {
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
        }
        
        val filteredTrophies = if (localHideEarned) trophies?.filter { !it.earned } else trophies

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

                LazyColumn(modifier = Modifier.weight(1f)) {
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading interactive interface...")
                }
            }
        }
    }
}
