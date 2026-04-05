package screens

import NetworkClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import androidx.compose.foundation.layout.size
import AppSettings
import androidx.compose.material.Switch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment

data class TrophyRow(val name: String, val description: String, val earned: Boolean, val timestamp: String?, val imageUrl: String?)

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
                    // Descriptions are usually in the td child right after title, or same container
                    val desc = row.selectFirst("td")?.text()?.substringAfter(name)?.trim() ?: "" 
                    val definitelyEarned = row.hasClass("completed")
                    val timestamp = if (definitelyEarned) row.selectFirst(".typo-top-date nobr")?.text()?.trim() else null
                    var imageUrl = row.selectFirst("picture.trophy img")?.attr("src")
                    if (imageUrl != null && imageUrl.startsWith("/")) imageUrl = "https://psnprofiles.com$imageUrl"
                    
                    tempTrophies.add(TrophyRow(name, desc, definitelyEarned, timestamp, imageUrl))
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
            
            // Localized toggle filter array
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
                    androidx.compose.material.Button(
                        onClick = {
                            navigator?.push(GuideScreen(guideHref!!, trophies!!.filter { it.earned }.map { it.name }))
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("View Trophy Guide")
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTrophies) { trophy ->
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            if (trophy.imageUrl != null) {
                                KamelImage(
                                    resource = asyncPainterResource(data = trophy.imageUrl),
                                    contentDescription = "Trophy Image",
                                    modifier = Modifier.size(50.dp).padding(end = 12.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trophy.name, style = MaterialTheme.typography.subtitle1, color = if (trophy.earned) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface)
                                Text(trophy.description, style = MaterialTheme.typography.caption)
                            }
                            androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    text = if (trophy.earned) "EARNED" else "Lock",
                                    color = if (trophy.earned) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color.Gray,
                                    style = MaterialTheme.typography.overline
                                )
                                if (trophy.timestamp != null) {
                                    Text(
                                        text = trophy.timestamp, 
                                        style = MaterialTheme.typography.caption,
                                        color = androidx.compose.ui.graphics.Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Loading trophies...", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
