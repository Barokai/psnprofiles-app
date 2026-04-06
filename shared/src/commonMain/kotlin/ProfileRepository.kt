import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

data class GameListRow(
    val title: String, 
    val platform: String, 
    val progress: String, 
    val lastPlayed: String, 
    val gameIdUrl: String,
    val imageUrl: String,
    val earnedTrophies: String,
    val totalTrophies: String
)
data class ProfileOverview(
    val psnId: String, 
    val level: String, 
    val gamesPlayed: String, 
    val gamesList: List<GameListRow>,
    val barStats: List<String>,
    val avatarUrl: String?
)

class ProfileRepository {
    suspend fun fetchProfileOverview(): ProfileOverview {
        // Fetch front page to grab dynamic username profile link
        val initialResp = NetworkClient.client.get("https://psnprofiles.com/")
        val doc1: Document = Ksoup.parse(initialResp.bodyAsText())
        val userMenuNode = doc1.selectFirst("a.dropdown-toggle")
        if (userMenuNode == null) {
            // If the user menu is missing, we might have been logged out or the parsing failed
            val loginButton = doc1.selectFirst("a[href=/login]")
            if (loginButton != null) {
                 println("DEBUG: User is definitely logged out.")
            }
        }
        val profileUrl = userMenuNode?.attr("href") ?: "/Unknown"
        val username = userMenuNode?.text()?.trim() ?: "Unknown User"
        var avatarUrl = userMenuNode?.selectFirst("img")?.attr("src")
        if (avatarUrl?.startsWith("/") == true) avatarUrl = "https://psnprofiles.com$avatarUrl"
        
        // Fetch actual profile
        val profileResp = NetworkClient.client.get("https://psnprofiles.com$profileUrl")
        val doc2: Document = Ksoup.parse(profileResp.bodyAsText())
        
        val barStatsNodes = doc2.select("ul.profile-bar li")
        val parsedStats = mutableListOf<String>()
        for (stat in barStatsNodes) {
             val txt = stat.text().trim()
             if (stat.hasClass("platinum") && txt.isNotEmpty()) parsedStats.add("Plat: $txt")
             else if (stat.hasClass("gold") && txt.isNotEmpty()) parsedStats.add("Gold: $txt")
             else if (stat.hasClass("silver") && txt.isNotEmpty()) parsedStats.add("Silver: $txt")
             else if (stat.hasClass("bronze") && txt.isNotEmpty()) parsedStats.add("Bronze: $txt")
             else if (stat.hasClass("total") && txt.isNotEmpty()) parsedStats.add("Total: $txt")
             else if (stat.hasClass("level") && txt.isNotEmpty()) parsedStats.add("Level: $txt")
             else if (stat.hasClass("plus")) parsedStats.add("PS+") 
             else if (txt.isNotEmpty()) parsedStats.add(txt)
        }

        val games = mutableListOf<GameListRow>()
        val tableRows = doc2.select("table.zebra tr")
        
        for (row in tableRows) {
            val titleNode = row.selectFirst("a.title")
            if (titleNode != null) {
                val title = titleNode.text()
                if (title.contains("more games", ignoreCase = true)) continue
                
                val href = titleNode.attr("href") // e.g. /trophies/1234-game
                val platforms = row.select("span.platform").joinToString(" ") { it.text() }
                val progress = row.selectFirst("div.progress-bar span")?.text() ?: "0%"
                val lastPlayed = row.select("td.typo-bottom").last()?.text()?.trim() ?: ""
                
                var imageUrl = row.selectFirst("picture.game img, img")?.attr("src") ?: ""
                if (imageUrl.startsWith("/")) imageUrl = "https://psnprofiles.com$imageUrl"
                
                var earnedTrophies = ""
                var totalTrophies = ""
                val smallInfoNode = row.selectFirst(".small-info")
                if (smallInfoNode != null) {
                    val bTags = smallInfoNode.select("b")
                    if (bTags.size >= 2) {
                        earnedTrophies = bTags[0].text().trim()
                        totalTrophies = bTags[1].text().trim()
                    }
                }
                
                games.add(GameListRow(
                    title = title, 
                    platform = platforms, 
                    progress = progress, 
                    lastPlayed = lastPlayed, 
                    gameIdUrl = href,
                    imageUrl = imageUrl,
                    earnedTrophies = earnedTrophies,
                    totalTrophies = totalTrophies
                ))
            }
        }
        
        return ProfileOverview(
            psnId = username,
            level = "1",
            gamesPlayed = games.size.toString(),
            gamesList = games,
            barStats = parsedStats,
            avatarUrl = avatarUrl
        )
    }
}
