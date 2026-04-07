package components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

fun LazyListScope.renderGuideNodes(nodes: List<GuideNode>, onAnchorClick: (String) -> Unit, onImageClick: (String) -> Unit) {
    nodes.forEach { node ->
        when (node) {
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
                item {
                    StaticGuideNode(node, onAnchorClick, onImageClick)
                }
            }
            is GuideNode.TrophyGroup -> {
                item {
                    androidx.compose.material.Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = 4.dp,
                        backgroundColor = if (node.isEarned) Color(0xFFE8F5E9) else Color.White,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (node.isEarned) Color(0xFFC8E6C9) else Color(0xFFEEEEEE))
                    ) {
                        Column {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                KamelImage(
                                    resource = asyncPainterResource(data = node.img),
                                    contentDescription = "Trophy Image",
                                    modifier = Modifier.size(64.dp).padding(end = 16.dp).clickable { onImageClick(node.img) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    val txtColor = if (node.isEarned) Color(0xFF2E7D32) else MaterialTheme.colors.primary
                                    val txtStyle = if (node.isEarned) TextDecoration.LineThrough else null
                                    Text(text = node.name, style = MaterialTheme.typography.h6.copy(textDecoration = txtStyle), color = txtColor)
                                    Text(text = node.description, style = MaterialTheme.typography.body2, color = if (node.isEarned) Color(0xFF43A047) else Color.Gray)
                                }
                                val rColor = when (node.rarity) {
                                    "Platinum" -> Color(0xFF546E7A)
                                    "Gold" -> Color(0xFFFBC02D)
                                    "Silver" -> Color(0xFF90A4AE)
                                    else -> Color(0xFF8D6E63)
                                }
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
                                    StaticGuideNodes(node.details, onAnchorClick, onImageClick)
                                }
                            }
                        }
                    }
                }
            }
            is GuideNode.Table -> {
                item {
                    RenderTable(node, onAnchorClick, onImageClick)
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
                            StaticGuideNodes(node.details, onAnchorClick, onImageClick)
                        }
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
                                        modifier = Modifier.weight(1f).background(parsedColor).padding(vertical = 8.dp),
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
            else -> {
                item {
                    StaticGuideNode(node, onAnchorClick, onImageClick)
                }
            }
        }
    }
}

@Composable
fun StaticGuideNodes(nodes: List<GuideNode>, onAnchorClick: (String) -> Unit, onImageClick: (String) -> Unit) {
    Column {
        nodes.forEach { node ->
            StaticGuideNode(node, onAnchorClick, onImageClick)
        }
    }
}

@Composable
fun StaticGuideNode(node: GuideNode, onAnchorClick: (String) -> Unit, onImageClick: (String) -> Unit) {
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onImageClick(node.url) },
                onFailure = { Text("Failed to load image") }
            )
        }
        is GuideNode.YouTubeNode -> {
            KamelImage(
                resource = asyncPainterResource(data = "https://img.youtube.com/vi/${node.videoId}/0.jpg"),
                contentDescription = "YouTube Thumbnail",
                modifier = Modifier.fillMaxWidth().aspectRatio(16/9f).padding(vertical = 8.dp).clickable { onAnchorClick("https://www.youtube.com/watch?v=${node.videoId}") },
                contentScale = ContentScale.Crop
            )
        }
        is GuideNode.TagNode -> {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .background(Color(node.colorHex.removePrefix("#").toLong(16) or 0xFF000000), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text = node.text, color = Color.White, style = MaterialTheme.typography.button)
            }
        }
        is GuideNode.RoadmapGrid -> {
            val chunks = node.trophies.chunked(2)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                chunks.forEach { chunk ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        chunk.forEach { t ->
                            val rColor = when (t.rarityLabel) {
                                "Platinum" -> Color(0xFF546E7A)
                                "Gold" -> Color(0xFFFBC02D)
                                "Silver" -> Color(0xFF90A4AE)
                                else -> Color(0xFF8D6E63)
                            }
                            androidx.compose.material.Card(
                                modifier = Modifier.weight(1f).padding(4.dp).clickable { onAnchorClick(t.anchor) },
                                elevation = 2.dp,
                                backgroundColor = if (t.isEarned) Color(0xFFE8F5E9) else Color.White,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                border = if (t.isEarned) BorderStroke(1.dp, Color(0xFFC8E6C9)) else null
                            ) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).background(rColor, shape = CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(t.name, style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text(t.desc, style = MaterialTheme.typography.overline, color = Color.Gray, maxLines = 1)
                                    }
                                }
                            }
                        }
                        if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        is GuideNode.Table -> {
            RenderTable(node, onAnchorClick, onImageClick)
        }
        else -> {}
    }
}

@Composable
private fun RenderTable(node: GuideNode.Table, onAnchorClick: (String) -> Unit, onImageClick: (String) -> Unit) {
    val backgroundColor = if (node.isInvisible) Color.Transparent else Color(0xFFF9F9F9)
    val borderColor = if (node.isInvisible) Color.Transparent else Color(0xFFEEEEEE)
    val scrollState = rememberScrollState()
    val firstRow = node.rows.firstOrNull()
    val totalPercent = firstRow?.sumOf { it.widthPercent ?: 0 } ?: 0
    val useWeightLayout = (totalPercent in 80..110) || (firstRow?.size ?: 0) <= 3
    val isScrollable = !useWeightLayout && (firstRow?.size ?: 0) > 3

    // Master Weight Template (from the first row that has the most columns)
    val maxRowCols = node.rows.maxOfOrNull { it.size } ?: 0
    val weightFloor = if (maxRowCols <= 2) 25f else 10f
    val templateRow = node.rows.find { it.size == maxRowCols && it.any { it.widthPercent != null } } 
        ?: node.rows.find { it.size == maxRowCols }
    val masterWeights = templateRow?.map { (it.widthPercent?.toFloat() ?: 20f).coerceAtLeast(weightFloor) }

    androidx.compose.material.Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 0.dp,
        backgroundColor = backgroundColor,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        border = if (node.isInvisible) null else BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isScrollable) Modifier.horizontalScroll(scrollState) else Modifier)
            ) {
                node.rows.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = if (useWeightLayout) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        var templateIdx = 0
                        row.forEachIndexed { cellIndex, cell ->
                            val cellWeight: Float = if (useWeightLayout && masterWeights != null) {
                                // Handle colspan by summing template weights
                                var sum = 0f
                                repeat(cell.colSpan) {
                                    sum += masterWeights.getOrNull(templateIdx + it) ?: 10f
                                }
                                templateIdx += cell.colSpan
                                sum
                            } else {
                                (cell.widthPercent?.toFloat() ?: 10f)
                            }

                            val cellModifier = when {
                                useWeightLayout -> Modifier.weight(cellWeight.coerceAtLeast(1f))
                                isScrollable -> Modifier.widthIn(min = 85.dp, max = 250.dp)
                                else -> Modifier.weight(cellWeight.coerceAtLeast(1f))
                            }

                            Column(
                                modifier = cellModifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalAlignment = if (cell.isIconOnly) Alignment.CenterHorizontally else Alignment.Start
                            ) {
                                StaticGuideNodes(cell.content, onAnchorClick, onImageClick)
                            }
                            if (cellIndex < row.size - 1 && !node.isInvisible) {
                                Box(modifier = Modifier.width(1.dp).height(intrinsicSize = IntrinsicSize.Max).background(Color(0xFFEEEEEE)))
                            }
                        }
                    }
                    if (rowIndex < node.rows.size - 1 && !node.isInvisible) {
                        Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    }
                }
            }
            
            // Shadows Indicators
            if (isScrollable) {
                // Right Shadow
                if (scrollState.value < scrollState.maxValue - 10) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                                )
                            )
                    )
                }
                // Left Shadow
                if (scrollState.value > 10) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)
                                )
                            )
                    )
                }
            }
        }
    }
}
