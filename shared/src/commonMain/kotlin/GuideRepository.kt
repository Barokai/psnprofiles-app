package repository

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import NetworkClient

data class TextSpan(val text: String, val isBold: Boolean = false, val isItalic: Boolean = false, val linkAnchor: String? = null)

sealed class GuideNode {
    data class Paragraph(val spans: List<TextSpan>, val isHeader: Boolean = false) : GuideNode()
    data class ImageNode(val url: String) : GuideNode()
    data class YouTubeNode(val videoId: String) : GuideNode()
    data class SectionHeader(val title: String, val anchorId: String) : GuideNode()
    data class TagNode(val text: String, val colorHex: String) : GuideNode()

    data class StatTag(val top: String, val bottom: String, val colorHex: String)
    data class CategoryTrophy(val name: String, val anchor: String, val rarity: String, val isEarned: Boolean)
    data class CategoryGroup(val name: String, val colorHex: String, val trophies: List<CategoryTrophy>)
    data class GuideInfoBox(val stats: List<StatTag>, val categories: List<CategoryGroup>, val anchorId: String) : GuideNode()

    data class GenericBox(val details: List<GuideNode>) : GuideNode()

    data class TrophyGroup(
        val name: String,
        val description: String,
        val img: String,
        val rarity: String,
        val completionRate: String,
        val rarityText: String,
        val isEarned: Boolean,
        val anchorId: String,
        val details: List<GuideNode>
    ) : GuideNode()

    data class TocItem(val name: String, val anchor: String, val rarity: String, val isEarned: Boolean, val colorHex: String)
    data class TableOfContents(val items: List<TocItem>) : GuideNode()

    data class RoadmapTrophy(val name: String, val desc: String, val rarityLabel: String, val anchor: String, val isEarned: Boolean)
    data class RoadmapGrid(val trophies: List<RoadmapTrophy>) : GuideNode()
    data class TableCell(
        val content: List<GuideNode>,
        val widthPercent: Int? = null,
        val isIconOnly: Boolean = false,
        val colSpan: Int = 1
    )
    data class Table(val rows: List<List<TableCell>>, val isInvisible: Boolean) : GuideNode()
    data class GuideTitle(val title: String) : GuideNode()
}

object GuideRepository {

    suspend fun fetchGuide(url: String): List<GuideNode> {
        val response = NetworkClient.client.get(url)
        val doc: Document = Ksoup.parse(response.bodyAsText())
        val extractedNodes = mutableListOf<GuideNode>()

        val docTitle = doc.selectFirst("title")?.text()?.trim() ?: "Trophy Guide"
        extractedNodes.add(GuideNode.GuideTitle(docTitle))

        // 1. Table of Contents (lives in #TOCWrapper, outside SectionContainers)
        val tocWrapper = doc.selectFirst("#TOCWrapper")
        if (tocWrapper != null) {
            val tocItems = mutableListOf<GuideNode.TocItem>()
            tocWrapper.select("li a").forEach { aNode ->
                val name = aNode.text().trim()
                if (name.isNotEmpty()) {
                    val href = aNode.attr("href")
                    val anchor = if (href.contains("#")) href.substringAfterLast("#") else ""
                    val classes = aNode.attr("class").lowercase()
                    val rarity = when {
                        classes.contains("platinum") -> "Platinum"
                        classes.contains("gold") -> "Gold"
                        classes.contains("silver") -> "Silver"
                        classes.contains("bronze") -> "Bronze"
                        else -> "Bronze"
                    }
                    val colorHex = when (rarity) {
                        "Platinum" -> "#B0BEC5"
                        "Gold" -> "#FFD700"
                        "Silver" -> "#C0C0C0"
                        else -> "#CD7F32"
                    }
                    val isEarned = aNode.parent()?.hasClass("earned") == true
                    tocItems.add(GuideNode.TocItem(name, anchor, rarity, isEarned, colorHex))
                }
            }
            if (tocItems.isNotEmpty()) extractedNodes.add(GuideNode.TableOfContents(tocItems))
        }

        // 2. Walk only top-level SectionContainers (avoids processing nested trophy boxes twice)
        val sections = doc.select("div[id^=SectionContainer]")
        sectionLoop@ for (section in sections) {
            // Section heading: comes from the direct child div's .title > h3
            val innerDiv = section.children().firstOrNull()
            val sectionAnchor = innerDiv?.id() ?: ""
            val sectionH3 = innerDiv?.selectFirst("div.title h3, div.title h4")?.text()?.trim() ?: ""
            if (sectionH3.isNotEmpty()) {
                extractedNodes.add(GuideNode.SectionHeader(sectionH3, sectionAnchor))
            }

            // Trophy sections: each trophy is a div.box.section-holder containing table.zebra
            var activeTrophyDetails: MutableList<GuideNode>? = null
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
                trophyImg = resolveUrl(trophyImg)
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

                // Parse remaining content (guide text, images, videos) as detail nodes
                // Scoping fix: search ONLY within this box or immediate siblings after the zebra table if they aren't another box
                zebraTable.remove() 
                val details = mutableListOf<GuideNode>()
                parseNestedParagraphs(box, details, "") // Use the 'box' instead of 'innerDiv' to avoid duplicating section text
                
                // If there's content after the box but before the next section-holder, we might need it too.
                // But for now, scoping to the box is much safer than the whole section.
                
                extractedNodes.add(GuideNode.TrophyGroup(
                    name = trophyName,
                    description = trophyDesc,
                    img = trophyImg,
                    rarity = rarityLabel,
                    completionRate = completionRate,
                    rarityText = rarityText,
                    isEarned = isEarned,
                    anchorId = sectionAnchor,
                    details = details
                ))
                activeTrophyDetails = details
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
                        extractedNodes.add(GuideNode.SectionHeader(stageH1.text().trim(), sectionAnchor))
                        stageH1.remove()
                    }
                    val currentDetails = mutableListOf<GuideNode>()
                    parseNestedParagraphs(frView, currentDetails, "")
                    if (currentDetails.isNotEmpty()) {
                        extractedNodes.add(GuideNode.GenericBox(currentDetails))
                    }
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
                        val trophies = mutableListOf<GuideNode.CategoryTrophy>()
                        tr.select("a.icon-sprite").forEach { aNode ->
                            val tName = aNode.text().trim()
                            if (tName.isNotEmpty()) {
                                val href = aNode.attr("href")
                                val anchor = if (href.contains("#")) href.substringAfterLast("#") else ""
                                val classes = aNode.attr("class").lowercase()
                                val rarity = when {
                                    classes.contains("platinum") -> "Platinum"
                                    classes.contains("gold") -> "Gold"
                                    classes.contains("silver") -> "Silver"
                                    classes.contains("bronze") -> "Bronze"
                                    else -> "Bronze"
                                }
                                val rColorHex = when (rarity) {
                                    "Platinum" -> "#B0BEC5"
                                    "Gold" -> "#FFD700"
                                    "Silver" -> "#C0C0C0"
                                    else -> "#CD7F32"
                                }
                                // Category list items are usually inside a <nobr> or just straight <a>. It might be inside a parent that has 'earned'.
                                val isEarned = aNode.parent()?.hasClass("earned") == true || aNode.parent()?.parent()?.hasClass("earned") == true
                                trophies.add(GuideNode.CategoryTrophy(tName, anchor, rarity, isEarned))
                            }
                        }
                        categories.add(GuideNode.CategoryGroup(catName, catColor, trophies))
                    }
                }
                extractedNodes.add(GuideNode.GuideInfoBox(stats, categories, sectionAnchor))
            }

            // Generic text sections (Tips & Strategies, etc.)
            // Find all potential boxes, but filter for "Top-Level" ones to avoid duplication
            val allCandidates = innerDiv?.select("div.box:not(.section-holder):not(.roadmap)") ?: emptyList()
            val textBoxes = allCandidates.filter { box ->
                box.selectFirst("span.tag .typo-top") == null &&
                box.parents().none { it.hasClass("box") && it in allCandidates }
            }
            
            for (box in textBoxes) {
                val details = mutableListOf<GuideNode>()
                parseNestedParagraphs(box, details, "")
                if (details.isNotEmpty()) {
                    val active = activeTrophyDetails
                    if (active != null) {
                        active.addAll(details)
                    } else {
                        extractedNodes.add(GuideNode.GenericBox(details))
                    }
                }
            }
        }

        return mergeAdjacentTables(extractedNodes)
    }

    private fun mergeAdjacentTables(nodes: List<GuideNode>): List<GuideNode> {
        if (nodes.size < 2) return nodes
        val result = mutableListOf<GuideNode>()
        var pendingTable: GuideNode.Table? = null

        fun stripEmptyColumns(table: GuideNode.Table): GuideNode.Table {
            val rows = table.rows
            if (rows.isEmpty()) return table
            val colCount = rows.maxOf { it.size }
            val emptyColIndices = mutableListOf<Int>()
            
            for (j in 0 until colCount) {
                val isColEffectivelyEmpty = rows.all { row ->
                    val cell = row.getOrNull(j)
                    cell == null || isCellEffectivelyEmpty(cell)
                }
                if (isColEffectivelyEmpty) emptyColIndices.add(j)
            }
            
            if (emptyColIndices.isEmpty()) return table
            
            val newRows = rows.map { row ->
                row.filterIndexed { index, _ -> index !in emptyColIndices }
            }.filter { it.isNotEmpty() }
            
            return table.copy(rows = newRows)
        }

        for (node in nodes) {
            if (node is GuideNode.Table) {
                if (pendingTable == null) {
                    pendingTable = node
                } else {
                    val pendingCols = pendingTable.rows.firstOrNull()?.size ?: 0
                    val currentCols = node.rows.firstOrNull()?.size ?: 0
                    if (pendingCols == currentCols && pendingCols > 0) {
                        pendingTable = pendingTable.copy(rows = pendingTable.rows + node.rows)
                    } else {
                        result.add(stripEmptyColumns(pendingTable))
                        pendingTable = node
                    }
                }
            } else {
                if (pendingTable != null) {
                    result.add(stripEmptyColumns(pendingTable))
                    pendingTable = null
                }
                result.add(node)
            }
        }
        if (pendingTable != null) result.add(stripEmptyColumns(pendingTable))
        return result
    }

    private fun isCellEffectivelyEmpty(cell: GuideNode.TableCell): Boolean {
        if (cell.content.isEmpty()) return true
        return cell.content.all { node ->
            when (node) {
                is GuideNode.Paragraph -> node.spans.isEmpty() || node.spans.all { it.text.isBlank() }
                is GuideNode.Table -> node.rows.all { r -> r.all { isCellEffectivelyEmpty(it) } }
                else -> false
            }
        }
    }

    private fun resolveUrl(url: String): String {
        if (url.isEmpty()) return ""
        if (url.startsWith("http")) return url
        if (url.startsWith("/")) return "https://psnprofiles.com$url"
        // Handle saved pages or relative links (e.g. ./brotato_files/...)
        if (url.contains("_files/")) return "https://psnprofiles.com/guide/18933-brotato-trophy-guide/$url" 
        return "https://psnprofiles.com/$url"
    }

    private fun parseNestedParagraphs(parent: Element, list: MutableList<GuideNode>, titleStrToSkip: String = "") {
        val currentSpans = mutableListOf<TextSpan>()

        fun flushSpans() {
            if (currentSpans.isNotEmpty()) {
                val fullTxt = currentSpans.joinToString("") { it.text }
                if (fullTxt.isNotBlank()) {
                    list.add(GuideNode.Paragraph(currentSpans.toList()))
                }
                currentSpans.clear()
            }
        }

        fun traverse(node: Node, isBoldCtx: Boolean = false, isItalicCtx: Boolean = false, linkAnchor: String? = null) {
            if (node is TextNode) {
                val txt = node.text()
                if (txt.isNotEmpty()) currentSpans.add(TextSpan(txt, isBoldCtx, isItalicCtx, linkAnchor))
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

                if (node.hasClass("lazyYT") && node.hasAttr("data-youtube-id")) {
                    flushSpans()
                    list.add(GuideNode.YouTubeNode(node.attr("data-youtube-id")))
                    return
                }

                when (tag) {
                    "br", "p", "div", "li", "h1", "h2", "h3", "h4", "h5", "ul", "ol" -> {
                        flushSpans()
                        val bold = isBoldCtx || tag == "b" || tag == "strong" || tag.startsWith("h")
                        val italic = isItalicCtx || tag == "i" || tag == "em"
                        node.childNodes().forEach { traverse(it, bold, italic, linkAnchor) }
                        flushSpans()
                    }
                    "table" -> {
                        flushSpans()
                        val rows = mutableListOf<List<GuideNode.TableCell>>()
                        val isInvisible = node.hasClass("invisible")
                        
                        val trElements = mutableListOf<Element>()
                        node.children().forEach { child ->
                            if (child.tagName() == "tr") trElements.add(child)
                            else if (child.tagName() in listOf("thead", "tbody", "tfoot")) {
                                child.children().forEach { subChild ->
                                    if (subChild.tagName() == "tr") trElements.add(subChild)
                                }
                            }
                        }
                        
                        trElements.forEach { tr ->
                            val cells = mutableListOf<GuideNode.TableCell>()
                            tr.children().filter { it.tagName() in listOf("th", "td") }.forEach { cell ->
                                val cellDetails = mutableListOf<GuideNode>()
                                parseNestedParagraphs(cell, cellDetails, "")
                                
                                val style = cell.attr("style")
                                val widthMatch = Regex("width:\\s*([0-9.]+)?%").find(style)
                                val widthPercent = widthMatch?.groupValues?.get(1)?.toDoubleOrNull()?.toInt()
                                val colSpan = cell.attr("colspan").toIntOrNull() ?: 1
                                
                                val isIconOnly = cellDetails.size == 1 && cellDetails.first() is GuideNode.ImageNode
                                cells.add(GuideNode.TableCell(cellDetails, widthPercent, isIconOnly, colSpan))
                            }
                            
                            // Colspan Header Extraction: If 1 cell spans multiple cols and contains a header
                            if (cells.size == 1 && cells[0].colSpan > 1) {
                                val hasHeader = cells[0].content.any { it is GuideNode.Paragraph && it.isHeader }
                                if (hasHeader) {
                                    list.addAll(cells[0].content)
                                    return@forEach // Skip this row in the Table node
                                }
                            }
                            
                            if (cells.isNotEmpty()) rows.add(cells)
                        }

                        if (rows.isNotEmpty()) {
                            val nonSpacerRows = rows.filter { r -> r.any { !isCellEffectivelyEmpty(it) } }
                            
                            // Advanced Header Detection: If 1 active row & only 1 active cell which is a header
                            if (nonSpacerRows.size == 1) {
                                val activeRow = nonSpacerRows[0]
                                val activeCells = activeRow.filter { !isCellEffectivelyEmpty(it) }
                                
                                if (activeCells.size == 1) {
                                    val firstContent = activeCells[0].content.firstOrNull()
                                    if (firstContent is GuideNode.Paragraph && firstContent.isHeader) {
                                        list.add(firstContent)
                                        return
                                    }
                                }
                            }
                            
                            // If table is entirely empty/spacers, skip it
                            if (nonSpacerRows.isEmpty()) return

                            list.add(GuideNode.Table(rows, isInvisible))
                        }
                        return
                    }
                    "span" -> {
                        if (node.hasClass("tag")) {
                            flushSpans()
                            val tagText = node.text().trim()
                            var tagColor = "#666666"
                            val style = node.attr("style")
                            if (style.contains("background-color:")) {
                                tagColor = style.substringAfter("background-color:").substringBefore(";").trim()
                            } else if (node.hasClass("Status")) tagColor = "#d78413"
                            else if (node.hasClass("Collectable")) tagColor = "#a34544"
                            
                            if (tagText.isNotEmpty()) {
                                list.add(GuideNode.TagNode(tagText, tagColor))
                            }
                        } else {
                            val bold = isBoldCtx || node.hasClass("bold")
                            val italic = isItalicCtx || node.hasClass("italic")
                            node.childNodes().forEach { traverse(it, bold, italic, linkAnchor) }
                        }
                    }
                    "img" -> {
                        if (node.hasClass("input") || node.hasClass("icon-sprite")) {
                            val altText = node.attr("alt")
                            if (altText.isNotEmpty()) {
                                currentSpans.add(TextSpan("[$altText]", isBoldCtx, isItalicCtx, linkAnchor))
                            }
                            } else {
                                flushSpans()
                                var src = node.attr("src")
                                src = resolveUrl(src)
                                if (src.contains("youtube.com/vi/")) {
                                    list.add(GuideNode.YouTubeNode(src.substringAfter("vi/").substringBefore("/")))
                                } else if (!node.hasClass("trophy") && src.isNotEmpty()) {
                                    list.add(GuideNode.ImageNode(src))
                                }
                            }
                    }
                    "iframe" -> {
                        flushSpans()
                        val src = node.attr("src")
                        if (src.contains("youtube.com/embed/")) {
                            list.add(GuideNode.YouTubeNode(src.substringAfterLast("/").substringBefore("?")))
                        }
                    }
                    "a" -> {
                        val href = node.attr("href")
                        val aAnchor = if (href.contains("#")) href.substringAfterLast("#") else linkAnchor
                        val bold = isBoldCtx || node.hasClass("bold")
                        val italic = isItalicCtx || node.hasClass("italic")
                        node.childNodes().forEach { traverse(it, bold, italic, aAnchor) }
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
        
        // Final pass to merge tables in this detail level
        val originalDetails = list.toList()
        list.clear()
        list.addAll(mergeAdjacentTables(originalDetails))
    }
}
