package components

import androidx.compose.foundation.background
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

fun LazyListScope.renderGuideNodes(nodes: List<GuideNode>) {
    for (node in nodes) {
        when (node) {
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
                            modifier = Modifier.padding(bottom = 4.dp, top = 4.dp).fillMaxWidth()
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
                    YouTubeEmbedNode(node.videoId)
                }
            }
            is GuideNode.TrophyGroup -> {
                item {
                    // Visually identically replicate the PSNProfiles Web Box directly onto Jetpack row spans!
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        elevation = 2.dp,
                        backgroundColor = Color(0xFFF9F9F9)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            if (node.img.isNotEmpty() && node.img.startsWith("http")) {
                                KamelImage(
                                    resource = asyncPainterResource(data = node.img),
                                    contentDescription = "Trophy Image",
                                    modifier = Modifier.size(54.dp).padding(end = 12.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.subtitle1,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (node.description.isNotEmpty()) {
                                    Text(
                                        text = node.description,
                                        style = MaterialTheme.typography.body2,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                val rColor = when (node.rarity) {
                                    "Bronze" -> Color(0xFFCD7F32)
                                    "Silver" -> Color(0xFFC0C0C0)
                                    "Gold" -> Color(0xFFFFD700)
                                    "Platinum" -> Color(0xFFE5E4E2)
                                    else -> Color.Transparent
                                }
                                if (rColor != Color.Transparent) {
                                    Box(modifier = Modifier.size(20.dp).background(rColor, shape = CircleShape))
                                }
                            }
                        }
                    }
                }
                
                // FLATTEN INFINITE MEMORY: Natively inject the recursive node details directly into the LazyColumn
                // rather than attempting to recursively bound them infinitely internally.
                renderGuideNodes(node.details)
                
                item {
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray)
                }
            }
        }
    }
}
