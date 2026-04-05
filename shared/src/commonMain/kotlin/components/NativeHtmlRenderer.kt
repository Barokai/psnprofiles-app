package components

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import repository.GuideNode

@Composable
fun NativeHtmlRenderer(nodes: List<GuideNode>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (node in nodes) {
            when (node) {
                is GuideNode.Paragraph -> {
                    if (node.spans.isNotEmpty()) {
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
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                is GuideNode.ImageNode -> {
                    KamelImage(
                        resource = asyncPainterResource(data = node.url),
                        contentDescription = "Guide Image",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        onFailure = { Text("Failed to load image: ${it.message}") }
                    )
                }
                is GuideNode.YouTubeNode -> {
                    YouTubeEmbedNode(node.videoId)
                }
                is GuideNode.TrophyGroup -> {
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                                if (node.img.isNotEmpty()) {
                                    KamelImage(
                                        resource = asyncPainterResource(data = node.img),
                                        contentDescription = "Trophy Image",
                                        modifier = Modifier.size(50.dp).padding(end = 12.dp)
                                    )
                                }
                                Text(
                                    text = node.name,
                                    style = MaterialTheme.typography.subtitle1,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            NativeHtmlRenderer(node.details)
                        }
                    }
                }
            }
        }
    }
}
