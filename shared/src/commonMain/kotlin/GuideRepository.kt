package repository

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import NetworkClient

data class TextSpan(val text: String, val isBold: Boolean = false, val isItalic: Boolean = false)

sealed class GuideNode {
    data class Paragraph(val spans: List<TextSpan>, val isHeader: Boolean = false) : GuideNode()
    data class ImageNode(val url: String) : GuideNode()
    data class YouTubeNode(val videoId: String) : GuideNode()
    data class SectionHeader(val title: String) : GuideNode()

    data class StatTag(val top: String, val bottom: String, val colorHex: String)
    data class CategoryGroup(val name: String, val colorHex: String, val trophies: List<String>)
    data class GuideInfoBox(val stats: List<StatTag>, val categories: List<CategoryGroup>) : GuideNode()

    data class TrophyGroup(
        val name: String,
        val description: String,
        val img: String,
        val rarity: String,
        val completionRate: String,
        val rarityText: String,
        val isEarned: Boolean,
        val details: List<GuideNode>
    ) : GuideNode()

    data class TableOfContents(val items: List<String>) : GuideNode()

    data class RoadmapTrophy(val name: String, val desc: String, val rarityLabel: String, val anchor: String, val isEarned: Boolean)
    data class RoadmapGrid(val trophies: List<RoadmapTrophy>) : GuideNode()
}

object GuideRepository {

    suspend fun fetchGuide(url: String): List<GuideNode> {
        val response = NetworkClient.client.get(url)
        val doc: Document = Ksoup.parse(response.bodyAsText())
        val extractedNodes = mutableListOf<GuideNode>()

        // 1. Table of Contents (lives in #TOCWrapper, outside SectionContainers)
        val tocWrapper = doc.selectFirst("#TOCWrapper")
        if (tocWrapper != null) {
            val tocItems = tocWrapper.select("li a").map { it.text().trim() }.filter { it.isNotEmpty() }
            if (tocItems.isNotEmpty()) extractedNodes.add(GuideNode.TableOfContents(tocItems))
        }

        // 2. Walk only top-level SectionContainers (avoids processing nested trophy boxes twice)
        val sections = doc.select("div[id^=SectionContainer]")
        for (section in sections) {
            // Section heading: comes from the direct child div's .title > h3
            val innerDiv = section.children().firstOrNull()
            val sectionH3 = innerDiv?.selectFirst("div.title h3, div.title h4")?.text()?.trim() ?: ""
            if (sectionH3.isNotEmpty()) {
                extractedNodes.add(GuideNode.SectionHeader(sectionH3))
            }

            // Trophy sections: each trophy is a div.box.section-holder containing table.zebra
            val trophyBoxes = innerDiv?.select("div.box.section-holder") ?: emptyList()
            for (box in trophyBoxes) {
                val zebraTable = box.selectFirst("table.zebra") ?: continue

                // Extract from the zebra table header row (second tr — first is usually empty)
                val dataRow = zebraTable.select("tr").firstOrNull { it.children().isNotEmpty() } ?: continue

                // Trophy image (class "trophy earned" or "trophy unearned")
                val trophyImgEl = dataRow.selectFirst("img.trophy")
                var trophyImg = trophyImgEl?.attr("srcset")
                    ?.split(",")?.firstOrNull()?.trim()?.split(" ")?.firstOrNull() ?: ""
                if (trophyImg.isEmpty()) trophyImg = trophyImgEl?.attr("src") ?: ""
                if (!trophyImg.startsWith("http")) trophyImg = "" // skip local paths
                val isEarned = trophyImgEl?.hasClass("earned") == true

                // Name and description from the wide td (width: 100%)
                val titleTd = dataRow.selectFirst("td[style*='width: 100%']")
                val trophyName = titleTd?.selectFirst("a.title")?.text()?.trim() ?: ""
                // Description is the direct text node after the <br>
                val trophyDesc = titleTd?.text()?.let { full ->
                    full.substringAfter(trophyName).trim().trimStart(',').trim()
                } ?: ""

                // Completion % and rarity text
                val completionRate = dataRow.selectFirst(".typo-top")?.text()?.trim() ?: ""
                val rarityText = dataRow.selectFirst(".typo-bottom nobr, .typo-bottom")?.text()?.trim() ?: ""

                // Rarity label from img alt attribute in the gradient-separator cell
                val rarityImg = dataRow.selectFirst("img[alt=Platinum], img[alt=Gold], img[alt=Silver], img[alt=Bronze]")
                val rarityLabel = rarityImg?.attr("alt") ?: "Bronze"

                // Remove zebra table from box so parseNestedParagraphs doesn't re-render it
                zebraTable.remove()

                // Parse remaining content (guide text, images, videos) as detail nodes
                val details = mutableListOf<GuideNode>()
                parseNestedParagraphs(box, details, "")

                extractedNodes.add(GuideNode.TrophyGroup(
                    name = trophyName,
                    description = trophyDesc,
                    img = trophyImg,
                    rarity = rarityLabel,
                    completionRate = completionRate,
                    rarityText = rarityText,
                    isEarned = isEarned,
                    details = details
                ))
            }

            // Roadmap box: div.box.roadmap containing roadmapStep divs
            val roadmapBox = innerDiv?.selectFirst("div.box.roadmap")
            if (roadmapBox != null) {
                val steps = roadmapBox.select("div[id^=roadmapStep]")
                val stepTargets: List<Element> = if (steps.isEmpty()) listOf(roadmapBox) else steps.toList()
                for (step in stepTargets) {
                    val frView = step.selectFirst(".fr-view, .step-original") ?: continue
                    val stageH1 = frView.selectFirst("h1")
                    if (stageH1 != null) {
                        extractedNodes.add(GuideNode.SectionHeader(stageH1.text().trim()))
                        stageH1.remove()
                    }
                    parseNestedParagraphs(frView, extractedNodes, "")
                }
                continue
            }

            // Overview box (Difficulty / Hours / Playthroughs): has span.tag with .typo-top
            val overviewBox = innerDiv?.selectFirst("div.box:has(span.tag .typo-top)")
            if (overviewBox != null) {
                val stats = mutableListOf<GuideNode.StatTag>()
                overviewBox.select("span.tag").forEach { tagNode ->
                    val tTop = tagNode.selectFirst(".typo-top")
                    if (tTop != null) {
                        val topText = tTop.text().trim()
                        val bottomText = tagNode.selectFirst(".typo-bottom")?.text()?.trim() ?: ""
                        var color = "#666666"
                        val style = tagNode.attr("style")
                        if (style.contains("background-color:")) {
                            color = style.substringAfter("background-color:").substringBefore(";").trim()
                        }
                        stats.add(GuideNode.StatTag(topText, bottomText, color))
                    }
                }
                val categories = mutableListOf<GuideNode.CategoryGroup>()
                overviewBox.select("tbody tr").forEach { tr ->
                    val catTag = tr.selectFirst("span.tag")
                    if (catTag != null) {
                        val catName = catTag.text().trim()
                        var catColor = "#666666"
                        val style = catTag.attr("style")
                        if (style.contains("background-color:")) {
                            catColor = style.substringAfter("background-color:").substringBefore(";").trim()
                        } else if (catTag.hasClass("Status")) catColor = "#d78413"
                        else if (catTag.hasClass("Collectable")) catColor = "#a34544"
                        val trophies = tr.select("a.icon-sprite").map { it.text().trim() }
                        categories.add(GuideNode.CategoryGroup(catName, catColor, trophies))
                    }
                }
                extractedNodes.add(GuideNode.GuideInfoBox(stats, categories))
                continue
            }

            // Generic text sections (Tips & Strategies, etc.)
            val textBoxes = innerDiv?.select("div.box:not(.section-holder):not(.roadmap)") ?: emptyList()
            for (box in textBoxes) {
                parseNestedParagraphs(box, extractedNodes, "")
            }
        }

        return extractedNodes
    }

    private fun parseNestedParagraphs(parent: Element, list: MutableList<GuideNode>, titleStrToSkip: String = "") {
        val currentSpans = mutableListOf<TextSpan>()

        fun flushSpans() {
            if (currentSpans.isNotEmpty()) {
                list.add(GuideNode.Paragraph(currentSpans.toList()))
                currentSpans.clear()
            }
        }

        fun traverse(node: Node, isBoldCtx: Boolean = false, isItalicCtx: Boolean = false) {
            if (node is TextNode) {
                val txt = node.text()
                if (txt.isNotEmpty()) currentSpans.add(TextSpan(txt, isBoldCtx, isItalicCtx))
            } else if (node is Element) {
                val tag = node.tagName().lowercase()

                // Handle roadmap trophy grid before generic div handling
                if (node.hasClass("roadmap-intended-trophies")) {
                    flushSpans()
                    val rmTrophies = mutableListOf<GuideNode.RoadmapTrophy>()
                    node.select("div.col-xs-6").forEach { col ->
                        val trophyDiv = col.selectFirst("div.trophy") ?: return@forEach
                        val titleAnchor = trophyDiv.selectFirst("a.title, a[href*='guide']")
                        val tName = titleAnchor?.text()?.trim() ?: ""
                        val tDesc = trophyDiv.selectFirst(".small-info")?.text()?.trim() ?: ""
                        val isEarned = trophyDiv.hasClass("earned")
                        val href = titleAnchor?.attr("href") ?: ""
                        val anchor = if (href.contains("#")) href.substringAfterLast("#") else ""
                        val imgName = trophyDiv.selectFirst("img")?.attr("src")?.lowercase() ?: ""
                        val rarityLabel = when {
                            imgName.contains("platinum") -> "Platinum"
                            imgName.contains("gold") -> "Gold"
                            imgName.contains("silver") -> "Silver"
                            else -> "Bronze"
                        }
                        if (tName.isNotEmpty()) {
                            rmTrophies.add(GuideNode.RoadmapTrophy(tName, tDesc, rarityLabel, anchor, isEarned))
                        }
                    }
                    if (rmTrophies.isNotEmpty()) list.add(GuideNode.RoadmapGrid(rmTrophies))
                    return
                }

                when (tag) {
                    "br", "p", "div", "li", "td", "tr", "table", "h1", "h2", "h3", "h4", "h5", "ul", "ol" -> {
                        flushSpans()
                        val bold = isBoldCtx || tag == "b" || tag == "strong" || tag.startsWith("h")
                        val italic = isItalicCtx || tag == "i" || tag == "em"
                        node.childNodes().forEach { traverse(it, bold, italic) }
                        flushSpans()
                    }
                    "img" -> {
                        flushSpans()
                        var src = node.attr("src")
                        if (src.startsWith("/")) src = "https://psnprofiles.com$src"
                        if (src.contains("youtube.com/vi/")) {
                            list.add(GuideNode.YouTubeNode(src.substringAfter("vi/").substringBefore("/")))
                        } else if (!node.hasClass("icon-sprite") && !node.hasClass("trophy") && !node.hasClass("input") && src.startsWith("http")) {
                            list.add(GuideNode.ImageNode(src))
                        }
                    }
                    "iframe" -> {
                        flushSpans()
                        val src = node.attr("src")
                        if (src.contains("youtube.com/embed/")) {
                            list.add(GuideNode.YouTubeNode(src.substringAfterLast("/").substringBefore("?")))
                        }
                    }
                    else -> {
                        val bold = isBoldCtx || tag == "b" || tag == "strong"
                        val italic = isItalicCtx || tag == "i" || tag == "em"
                        node.childNodes().forEach { traverse(it, bold, italic) }
                    }
                }
            }
        }

        parent.childNodes().forEach { child ->
            val element = child as? Element
            val isBoxHeader = element != null && (element.hasClass("title") || element.tagName() in listOf("h3", "h4"))
            if (!isBoxHeader || element?.text() != titleStrToSkip) {
                traverse(child)
            }
        }
        flushSpans()
    }
}
