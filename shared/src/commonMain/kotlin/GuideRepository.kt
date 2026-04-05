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
    data class TrophyGroup(val name: String, val img: String, val details: List<GuideNode>) : GuideNode()
}

object GuideRepository {

    suspend fun fetchGuide(url: String): List<GuideNode> {
        val response = NetworkClient.client.get(url)
        val doc: Document = Ksoup.parse(response.bodyAsText())
        val extractedNodes = mutableListOf<GuideNode>()
        
        val boxes = doc.select("div.box")
        for (box in boxes) {
            val titleNode = box.selectFirst(".title, h3, h4")
            val titleStr = titleNode?.text() ?: ""
            
            // Skip ToC completely!
            if (box.hasClass("toc") || titleStr.contains("Table of Contents", ignoreCase = true) || box.id() == "toc") {
                continue
            }
            
            // Extract typical Guide trophy structural parameters if present
            if (box.hasClass("guide-trophy") || (titleStr.isNotEmpty() && box.select(".trophy-image").isNotEmpty())) {
                var trophyImg = box.selectFirst(".trophy-image img, img")?.attr("src") ?: ""
                if (trophyImg.startsWith("/")) trophyImg = "https://psnprofiles.com$trophyImg"
                
                val detailsList = mutableListOf<GuideNode>()
                parseNestedParagraphs(box, detailsList)
                extractedNodes.add(GuideNode.TrophyGroup(name = titleStr, img = trophyImg, details = detailsList))
            } else {
                if (titleStr.isNotEmpty()) {
                    extractedNodes.add(GuideNode.Paragraph(listOf(TextSpan(titleStr, isBold = true)), isHeader = true))
                }
                parseNestedParagraphs(box, extractedNodes)
            }
        }
        return extractedNodes
    }

    private fun parseNestedParagraphs(parent: Element, list: MutableList<GuideNode>) {
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
                // Retain space preservation so organic sentences don't fuse!
                if (txt.isNotEmpty()) {
                    currentSpans.add(TextSpan(txt, isBoldCtx, isItalicCtx))
                }
            } else if (node is Element) {
                val tag = node.tagName().lowercase()
                when (tag) {
                    "br", "p", "div", "li", "td", "tr", "table", "h1", "h2", "h3", "h4", "h5", "ul", "ol" -> {
                        flushSpans() // Break paragraph
                        
                        val bold = isBoldCtx || tag == "b" || tag == "strong" || tag.startsWith("h")
                        val italic = isItalicCtx || tag == "i" || tag == "em"
                        node.childNodes().forEach { traverse(it, bold, italic) }
                        
                        flushSpans() // Force separation after block element concludes
                    }
                    "img", "iframe" -> {
                        flushSpans()
                        if (tag == "img") {
                            var src = node.attr("src")
                            if (src.startsWith("/")) src = "https://psnprofiles.com$src"
                            if (src.contains("youtube.com/vi/")) {
                                val vidId = src.substringAfter("vi/").substringBefore("/")
                                list.add(GuideNode.YouTubeNode(vidId))
                            } else if (!node.hasClass("icon-sprite") && src.isNotEmpty() && src.startsWith("http")) {
                                list.add(GuideNode.ImageNode(src))
                            }
                        } else if (tag == "iframe") {
                            val src = node.attr("src")
                            if (src.contains("youtube.com/embed/")) {
                                val vidId = src.substringAfterLast("/").substringBefore("?")
                                list.add(GuideNode.YouTubeNode(vidId))
                            }
                        }
                    }
                    else -> {
                        // Safely traverse inline styles (spans, anchors, formats)
                        val bold = isBoldCtx || tag == "b" || tag == "strong"
                        val italic = isItalicCtx || tag == "i" || tag == "em"
                        node.childNodes().forEach { traverse(it, bold, italic) }
                    }
                }
            }
        }

        // Drop the manual title string previously intercepted so we don't output dupe lines
        parent.childNodes().forEach { 
            val childTag = (it as? Element)?.tagName()?.lowercase() ?: ""
            if (!childTag.startsWith("h") && !((it as? Element)?.hasClass("title") == true)) {
                traverse(it) 
            }
        }
        flushSpans()
    }
}
