@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.guide.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.icons.trail.Info
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

/*
 * Article typography for the guides (user guide, survival guide, field guide): markdown read as paragraphs with a
 * comfortable line height, bullets as small primary shapes, numbered steps in numbered badges, quotes as tinted notes,
 * bold as SemiBold in the accent, and links that open.
 */

internal sealed interface ArticleBlock {
    data class Paragraph(val text: String) : ArticleBlock
    data class Bullet(val text: String, val depth: Int) : ArticleBlock
    data class Step(val number: String, val text: String) : ArticleBlock
    data class Note(val text: String) : ArticleBlock
    data class Heading(val text: String) : ArticleBlock
}

private val bulletPattern = Regex("""^(\s*)[-*•]\s+(.*)$""")
private val stepPattern = Regex("""^\s*(\d+)[.)]\s+(.*)$""")
private val headingPattern = Regex("""^#{1,6}\s+(.*)$""")

/** Splits markdown into reading blocks; consecutive plain lines join into one paragraph. */
internal fun parseArticle(markdown: String): List<ArticleBlock> {
    val blocks = mutableListOf<ArticleBlock>()
    val paragraph = StringBuilder()
    fun flush() {
        if (paragraph.isNotBlank()) {
            blocks += ArticleBlock.Paragraph(paragraph.toString().trim())
        }
        paragraph.clear()
    }
    markdown.lineSequence().forEach { raw ->
        val line = raw.trimEnd()
        val bullet = bulletPattern.find(line)
        val step = stepPattern.find(line)
        val heading = headingPattern.find(line.trim())
        when {
            line.isBlank() -> flush()
            heading != null -> {
                flush()
                blocks += ArticleBlock.Heading(heading.groupValues[1].trim())
            }

            bullet != null -> {
                flush()
                blocks += ArticleBlock.Bullet(bullet.groupValues[2].trim(), if (bullet.groupValues[1].length >= 2) 1 else 0)
            }

            step != null -> {
                flush()
                blocks += ArticleBlock.Step(step.groupValues[1], step.groupValues[2].trim())
            }

            line.trimStart().startsWith(">") -> {
                flush()
                blocks += ArticleBlock.Note(line.trimStart().removePrefix(">").trim())
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line.trim())
            }
        }
    }
    flush()
    return blocks
}

/** Inline markdown (bold, italics, links) as styled text. */
@Composable
internal fun rememberInlineMarkdown(text: String, accent: Color, link: Color): AnnotatedString {
    return remember(text, accent, link) {
        buildAnnotatedString {
            val pattern = Regex("""\*\*(.+?)\*\*|__(.+?)__|\[(.+?)]\((.+?)\)|(?<![*\w])\*(?!\s)(.+?)\*(?!\*)""")
            var last = 0
            pattern.findAll(text).forEach { match ->
                append(text.substring(last, match.range.first))
                val (bold1, bold2, linkText, url, italic) = match.destructured
                when {
                    bold1.isNotEmpty() || bold2.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = accent)) {
                        append(bold1.ifEmpty { bold2 })
                    }

                    linkText.isNotEmpty() -> withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
                        )
                    ) { append(linkText) }

                    italic.isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
                }
                last = match.range.last + 1
            }
            append(text.substring(last))
        }
    }
}

/** The body text style for reading: large, airy, full contrast. */
@Composable
internal fun articleBodyStyle() = MaterialTheme.typography.bodyLarge.copy(
    lineHeight = 27.sp,
    letterSpacing = 0.2.sp,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
)

/** Renders markdown as a column of reading blocks. */
@Composable
internal fun ArticleBody(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { parseArticle(markdown) }
    ArticleBlocks(blocks, modifier)
}

@Composable
internal fun ArticleBlocks(blocks: List<ArticleBlock>, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.onSurface
    val link = MaterialTheme.colorScheme.primary
    val body = articleBodyStyle()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)) {
        blocks.forEach { block ->
            when (block) {
                is ArticleBlock.Paragraph -> Text(rememberInlineMarkdown(block.text, accent, link), style = body)

                is ArticleBlock.Heading -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = TrailSenseSpacing.sm)
                        .semantics { heading() }
                )

                is ArticleBlock.Bullet -> Row(Modifier.padding(start = (block.depth * 20).dp)) {
                    Box(Modifier.padding(top = 9.dp, end = TrailSenseSpacing.md)) {
                        Surface(
                            shape = if (block.depth == 0) MaterialShapes.Cookie4Sided.toShape() else MaterialShapes.Circle.toShape(),
                            color = if (block.depth == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(if (block.depth == 0) 10.dp else 7.dp)
                        ) {}
                    }
                    Text(rememberInlineMarkdown(block.text, accent, link), style = body, modifier = Modifier.weight(1f))
                }

                is ArticleBlock.Step -> Row(verticalAlignment = Alignment.Top) {
                    TrailSenseShapeBadge(
                        text = block.number,
                        shape = MaterialShapes.Cookie7Sided.toShape(),
                        size = 30.dp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.md))
                    Text(
                        rememberInlineMarkdown(block.text, accent, link),
                        style = body,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 2.dp)
                    )
                }

                is ArticleBlock.Note -> Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(TrailSenseSpacing.lg)) {
                        Icon(TrailIcons.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(TrailSenseSpacing.md))
                        Text(
                            rememberInlineMarkdown(block.text, MaterialTheme.colorScheme.onTertiaryContainer, link),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                        )
                    }
                }
            }
        }
    }
}

/** A numbered section heading: the number in a cookie badge, the title beside it. */
@Composable
internal fun ArticleSectionHeading(number: Int?, title: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (number != null) {
            TrailSenseShapeBadge(
                text = number.toString(),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 40.dp,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(TrailSenseSpacing.md))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
    }
}
