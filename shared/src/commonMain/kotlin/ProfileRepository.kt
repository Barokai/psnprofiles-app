import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

data class GameListRow(val title: String, val platform: String, val progress: String, val lastPlayed: String, val gameIdUrl: String)
data class ProfileOverview(val psnId: String, val level: String, val gamesPlayed: String, val gamesList: List<GameListRow>)

class ProfileRepository {
    suspend fun fetchProfileOverview(): ProfileOverview {
        // Fetch front page to grab dynamic username profile link
        val initialResp = NetworkClient.client.get("https://psnprofiles.com/")
        val doc1: Document = Ksoup.parse(initialResp.bodyAsText())
        val userMenuNode = doc1.selectFirst("a.dropdown-toggle")
        val profileUrl = userMenuNode?.attr("href") ?: "/Unknown"
        val username = userMenuNode?.text()?.trim() ?: "Unknown User"
        
        // Fetch actual profile
        val profileResp = NetworkClient.client.get("https://psnprofiles.com$profileUrl")
        val doc2: Document = Ksoup.parse(profileResp.bodyAsText())
        
        val games = mutableListOf<GameListRow>()
        val tableRows = doc2.select("table.zebra tr")
        
        for (row in tableRows) {
            val titleNode = row.selectFirst("a.title")
            if (titleNode != null) {
                val title = titleNode.text()
                val href = titleNode.attr("href") // e.g. /trophies/1234-game
                val platforms = row.select("span.platform").joinToString { it.text() }
                val progress = row.selectFirst("div.progress-bar span")?.text() ?: "0%"
                // Usually the last tyop-bottom contains last played
                val lastPlayed = row.select("td.typo-bottom").last()?.text()?.trim() ?: ""
                
                games.add(GameListRow(title, platforms, progress, lastPlayed, href))
            }
        }
        
        return ProfileOverview(
            psnId = username,
            level = "1",
            gamesPlayed = games.size.toString(),
            gamesList = games
        )
    }
}
