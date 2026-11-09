package com.kylecorry.andromeda.xml

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlStreaming

public object XMLConvert {
    public fun toString(xml: XMLNode, isRoot: Boolean = true): String = buildString {
        if (isRoot) append("<?xml version=\"1.0\"?>")
        fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;")
        fun write(node: XMLNode) {
            append('<').append(node.tag)
            for ((key, value) in node.attributes) append(' ').append(key).append("=\"").append(escape(value)).append('"')
            append('>')
            node.text?.let { append(escape(it)) }
            node.children.forEach(::write)
            append("</").append(node.tag).append('>')
        }
        write(xml)
    }

    public fun parse(xml: String): XMLNode {
        val reader = xmlStreaming.newGenericReader(xml, expandEntities = true)
        while (reader.hasNext() && reader.next() != EventType.START_ELEMENT) { }
        return readNode(reader)
    }

    private fun readNode(reader: XmlReader): XMLNode {
        val tag = qualified(reader.prefix, reader.localName)
        val attributes = reader.namespaceDecls.associate {
            (if (it.prefix.isEmpty()) "xmlns" else "xmlns:${it.prefix}") to it.namespaceURI
        }.toMutableMap()
        for (index in 0 until reader.attributeCount) {
            attributes[qualified(reader.getAttributePrefix(index), reader.getAttributeLocalName(index))] =
                reader.getAttributeValue(index)
        }
        val text = StringBuilder()
        val children = mutableListOf<XMLNode>()
        while (reader.hasNext()) {
            when (reader.next()) {
                EventType.START_ELEMENT -> children.add(readNode(reader))
                EventType.TEXT, EventType.CDSECT -> text.append(reader.text)
                EventType.END_ELEMENT -> return XMLNode(tag, attributes, text.toString().takeUnless { it.isBlank() }, children)
                else -> Unit
            }
        }
        return XMLNode(tag, attributes, text.toString().takeUnless { it.isBlank() }, children)
    }

    private fun qualified(prefix: String, name: String): String = if (prefix.isEmpty()) name else "$prefix:$name"
}
