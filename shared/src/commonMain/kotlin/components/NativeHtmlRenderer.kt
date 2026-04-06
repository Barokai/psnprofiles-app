package components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import repository.GuideNode

fun LazyListScope.renderGuideNodes(nodes: List<GuideNode>, onAnchorClick: (String) -> Unit = {}) {
    for (node in nodes) {
        when (node) {
            is GuideNode.TableOfContents -> {
                // Rendered in the AppBar DropdownMenu — suppressed from inline list.
            }
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
            is GuideNode.Paragraph -> {
                if (node.spans.isNotEmpty()) {
                    item {
                        val annotatedString = buildAnnotatedString {
                            for (span in node.spans) {
                                withStyle(
                                    style = SpanStyle(
                                        fontWeight = if (span.isBold) FontWeight.Bold else null,
                                        fontStyle = if (span.isItalic) FontStyle.Italic else null
                                    )
                                ) {
                                    append(span.text)
                                }
                            }
                        }
                        Text(
                            text = annotatedString,
                            style = if (node.isHeader) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
                            modifier = Modifier.padding(bottom = 2.dp).fillMaxWidth()
                        )
                    }
                }
            }
            is GuideNode.ImageNode -> {
                item {
                    KamelImage(
                        resource = asyncPainterResource(data = node.url),
                        contentDescription = "Guide Image",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, top = 4.dp),
                        onFailure = { Text("Failed to render graphical asset natively") }
                    )
                }
            }
            is GuideNode.YouTubeNode -> {
                item {
                    // YouTubeEmbedNode rendered inline if available
                    Text(
                        text = "▶ Video: youtube.com/watch?v=${node.videoId}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
                            // Top Stats (Difficulty / Hours)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                node.stats.forEach { stat ->
                                    val parsedColor = try {
                                        Color(stat.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
                                    } catch (e: Exception) { Color.Gray }
                                    
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
                            
                            // Trophy Categories (Story, Collectible)
                            node.categories.forEach { cat ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                                    val parsedCatColor = try {
                                        Color(cat.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
                                    } catch (e: Exception) { Color.DarkGray }
                                    
                                    Box(
                                        modifier = Modifier.background(parsedCatColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(cat.name.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.caption)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = cat.trophies.joinToString(" • "),
                                        style = MaterialTheme.typography.body2,
                                        color = Color.DarkGray,
                                        modifier = Modifier.weight(1f).padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            is GuideNode.RoadmapGrid -> {
                val chunks = node.trophies.chunked(2)
                chunks.forEach { chunk ->
                    item {
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
                                        // Rarity color dot (since trophy images are local in the saved HTML)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(rColor, shape = CircleShape)
                                        )
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
                            if (chunk.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            is GuideNode.TrophyGroup -> {
                item {
                    val cardBg = if (node.isEarned) Color(0xFFEDF7ED) else Color.White
                    val borderCol = if (node.isEarned) Color(0xFF81C784) else Color(0xFFEEEEEE)
                    val rColor = when (node.rarity) {
                        "Platinum" -> Color(0xFFB0BEC5)
                        "Gold" -> Color(0xFFFFD700)
                        "Silver" -> Color(0xFFC0C0C0)
                        else -> Color(0xFFCD7F32) // Bronze
                    }
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                        elevation = if (node.isEarned) 0.dp else 2.dp,
                        backgroundColor = cardBg,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, borderCol)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            // Left: Trophy image (CDN on live site) or colored rarity square fallback
                            if (node.img.isNotEmpty() && node.img.startsWith("http")) {
                                KamelImage(
                                    resource = asyncPainterResource(data = node.img),
                                    contentDescription = "Trophy",
                                    modifier = Modifier.size(52.dp).padding(end = 10.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).padding(end = 10.dp)
                                        .background(rColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                )
                            }
                            // Center: Name + description
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.subtitle1,
                                    color = if (node.isEarned) Color(0xFF2E7D32) else MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (node.description.isNotEmpty()) {
                                    Text(
                                        text = node.description,
                                        style = MaterialTheme.typography.body2,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            // Right: completion % + rarity text + rarity dot
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                if (node.completionRate.isNotEmpty()) {
                                    Text(
                                        text = node.completionRate,
                                        style = MaterialTheme.typography.body2,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }
                                if (node.rarityText.isNotEmpty()) {
                                    Text(
                                        text = node.rarityText.uppercase(),
                                        style = MaterialTheme.typography.overline,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(modifier = Modifier.size(18.dp).background(rColor, shape = CircleShape))
                            }
                        }
                    }
                }

                // Flatten details into the lazy list (prevents OOM from nested scrolling)
                renderGuideNodes(node.details, onAnchorClick)

                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFEEEEEE))
                }
            }
        }
    }
}
