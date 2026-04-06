package components

import androidx.compose.foundation.text.ClickableText

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import repository.GuideNode

fun LazyListScope.renderGuideNodes(nodes: List<GuideNode>, onAnchorClick: (String) -> Unit = {}) {
    for (node in nodes) {
        when (node) {
            is GuideNode.TableOfContents -> {} // Handled in AppBar
            is GuideNode.SectionHeader -> {
                item {
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                        elevation = 2.dp,
                        backgroundColor = Color(0xFF1A237E),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = node.title,
                            style = MaterialTheme.typography.h6,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
            is GuideNode.GuideInfoBox -> {
                item {
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        elevation = 2.dp,
                        backgroundColor = Color(0xFFFAFAFA),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            // Top Stats
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                node.stats.forEach { stat ->
                                    val parsedColor = try { Color(stat.colorHex.removePrefix("#").toLong(16) or 0xFF000000) } catch (e: Exception) { Color.Gray }
                                    Column(
                                        modifier = Modifier.background(parsedColor).padding(horizontal = 24.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(stat.top, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6)
                                        Text(stat.bottom.uppercase(), color = Color.White, style = MaterialTheme.typography.overline)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            
                            // Categories
                            node.categories.forEach { cat ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                    val parsedCatColor = try { Color(cat.colorHex.removePrefix("#").toLong(16) or 0xFF000000) } catch (e: Exception) { Color.DarkGray }
                                    Box(modifier = Modifier.background(parsedCatColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(cat.name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    @OptIn(ExperimentalLayoutApi::class)
                                    FlowRow(
                                        modifier = Modifier.weight(1f).padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        cat.trophies.forEach { trophy ->
                                            val rarityColor = when (trophy.rarity) {
                                                "Platinum" -> Color(0xFFB0BEC5)
                                                "Gold" -> Color(0xFFFFD700)
                                                "Silver" -> Color(0xFFC0C0C0)
                                                else -> Color(0xFFCD7F32)
                                            }
                                            val txtColor = if (trophy.isEarned) Color(0xFF4CAF50) else Color.DarkGray
                                            val txtStyle = if (trophy.isEarned) TextDecoration.LineThrough else null
                                            Row(
                                                modifier = Modifier.clickable(enabled = trophy.anchor.isNotEmpty()) { onAnchorClick(trophy.anchor) }.padding(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.size(8.dp).background(rarityColor, CircleShape))
                                                Spacer(Modifier.width(4.dp))
                                                Text(text = trophy.name, style = MaterialTheme.typography.body2.copy(textDecoration = txtStyle), color = txtColor, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is GuideNode.TrophyGroup -> {
                item {
                    val cardBg = if (node.isEarned) Color(0xFFEDF7ED) else Color(0xFFFAFAFA)
                    val borderCol = if (node.isEarned) Color(0xFF81C784) else Color(0xFFE0E0E0)
                    val rColor = when (node.rarity) {
                        "Platinum" -> Color(0xFFB0BEC5)
                        "Gold" -> Color(0xFFFFD700)
                        "Silver" -> Color(0xFFC0C0C0)
                        else -> Color(0xFFCD7F32)
                    }
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                        elevation = if (node.isEarned) 0.dp else 2.dp,
                        backgroundColor = cardBg,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                if (node.img.isNotEmpty() && node.img.startsWith("http")) {
                                    KamelImage(
                                        resource = asyncPainterResource(data = node.img),
                                        contentDescription = "Trophy",
                                        modifier = Modifier.size(52.dp).padding(end = 10.dp)
                                    )
                                } else {
                                    Box(modifier = Modifier.size(40.dp).padding(end = 10.dp).background(rColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = node.name, style = MaterialTheme.typography.subtitle1, color = if (node.isEarned) Color(0xFF2E7D32) else MaterialTheme.colors.primary, fontWeight = FontWeight.Bold)
                                    if (node.description.isNotEmpty()) {
                                        Text(text = node.description, style = MaterialTheme.typography.body2, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    if (node.completionRate.isNotEmpty()) Text(text = node.completionRate, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    if (node.rarityText.isNotEmpty()) Text(text = node.rarityText.uppercase(), style = MaterialTheme.typography.overline, color = Color.Gray)
                                    Spacer(Modifier.height(4.dp))
                                    Box(modifier = Modifier.size(18.dp).background(rColor, shape = CircleShape))
                                }
                            }
                            // Details natively rendered inside the card
                            if (node.details.isNotEmpty()) {
                                Divider(color = Color(0xFFE0E0E0))
                                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                    StaticGuideNodes(node.details, onAnchorClick)
                                }
                            }
                        }
                    }
                }
            }
            is GuideNode.GenericBox -> {
                item {
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                        elevation = 1.dp,
                        backgroundColor = Color(0xFFFDFDFD),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            StaticGuideNodes(node.details, onAnchorClick)
                        }
                    }
                }
            }
            // For stray nodes at the top level (if any), render them inside an item wrapper
            else -> {
                item {
                    StaticGuideNode(node, onAnchorClick)
                }
            }
        }
    }
}

@Composable
fun StaticGuideNodes(nodes: List<GuideNode>, onAnchorClick: (String) -> Unit) {
    Column {
        nodes.forEach { node ->
            StaticGuideNode(node, onAnchorClick)
        }
    }
}

@Composable
fun StaticGuideNode(node: GuideNode, onAnchorClick: (String) -> Unit) {
    when (node) {
        is GuideNode.Paragraph -> {
            if (node.spans.isNotEmpty()) {
                val annotatedString = buildAnnotatedString {
                    for (span in node.spans) {
                        pushStringAnnotation(tag = "URL", annotation = span.linkAnchor ?: "")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = if (span.isBold) FontWeight.Bold else null,
                                fontStyle = if (span.isItalic) FontStyle.Italic else null,
                                color = if (span.linkAnchor != null) MaterialTheme.colors.primary else Color.Unspecified,
                                textDecoration = if (span.linkAnchor != null) TextDecoration.Underline else null
                            )
                        ) {
                            append(span.text)
                        }
                        pop()
                    }
                }
                ClickableText(
                    text = annotatedString,
                    style = if (node.isHeader) MaterialTheme.typography.h6 else MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface),
                    modifier = Modifier.padding(bottom = 6.dp).fillMaxWidth(),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { annotation ->
                            if (annotation.item.isNotEmpty()) {
                                onAnchorClick(annotation.item)
                            }
                        }
                    }
                )
            }
        }
        is GuideNode.ImageNode -> {
            KamelImage(
                resource = asyncPainterResource(data = node.url),
                contentDescription = "Guide Image",
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                onFailure = { Text("Failed to load image") }
            )
        }
        is GuideNode.YouTubeNode -> {
            val uriHandler = LocalUriHandler.current
            val thumbnailUrl = "https://img.youtube.com/vi/${node.videoId}/maxresdefault.jpg"
            androidx.compose.material.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 6.dp)
                    .clickable {
                        uriHandler.openUri("https://www.youtube.com/watch?v=${node.videoId}")
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                elevation = 2.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    KamelImage(
                        resource = asyncPainterResource(data = thumbnailUrl),
                        contentDescription = "YouTube Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onFailure = { Text("Failed to load video thumbnail") }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Red, androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("▶", color = Color.White, style = MaterialTheme.typography.h5, modifier = Modifier.offset(x = 2.dp))
                        }
                    }
                }
            }
        }
        is GuideNode.TagNode -> {
            val parsedColor = try { Color(node.colorHex.removePrefix("#").toLong(16) or 0xFF000000) } catch (e: Exception) { Color.DarkGray }
            Box(modifier = Modifier.padding(bottom = 8.dp).background(parsedColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(node.text.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
            }
        }
        is GuideNode.RoadmapGrid -> {
            val chunks = node.trophies.chunked(2)
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                chunks.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chunk.forEach { rmTrophy ->
                            val rColor = when (rmTrophy.rarityLabel) {
                                "Platinum" -> Color(0xFFE5E4E2)
                                "Gold" -> Color(0xFFFFD700)
                                "Silver" -> Color(0xFFC0C0C0)
                                else -> Color(0xFFCD7F32)
                            }
                            val bgColor = if (rmTrophy.isEarned) Color(0xFFEDF7ED) else Color(0xFFF8F8F8)
                            val borderColor = if (rmTrophy.isEarned) Color(0xFF81C784) else Color(0xFFDDDDDD)
                            androidx.compose.material.Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(enabled = rmTrophy.anchor.isNotEmpty()) {
                                        onAnchorClick(rmTrophy.anchor)
                                    },
                                elevation = 1.dp,
                                backgroundColor = bgColor,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(10.dp).background(rColor, shape = CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rmTrophy.name,
                                            style = MaterialTheme.typography.subtitle2,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (rmTrophy.isEarned) Color(0xFF2E7D32) else MaterialTheme.colors.primary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if (rmTrophy.desc.isNotEmpty()) {
                                            Text(
                                                text = rmTrophy.desc,
                                                style = MaterialTheme.typography.caption,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        else -> {}
    }
}
