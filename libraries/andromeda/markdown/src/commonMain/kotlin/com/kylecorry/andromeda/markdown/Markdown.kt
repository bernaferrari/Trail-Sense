package com.kylecorry.andromeda.markdown

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

public object Markdown {
    public fun toHtml(markdown: String): String {
        val text = markdown.replace("\r\n", "\n").replace("\r", "\n")
        val flavour = GFMFlavourDescriptor()
        val tree = MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable).buildMarkdownTreeFromString(text as CharSequence)
        return HtmlGenerator(text, tree, flavour).generateHtml()
    }
}
