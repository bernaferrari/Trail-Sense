package com.kylecorry.andromeda.xml

public class XmlSerializer(private val isRoot: Boolean = true) {
    public fun serialize(value: XMLNode): ByteArray = XMLConvert.toString(value, isRoot).encodeToByteArray()
    public fun deserialize(bytes: ByteArray): XMLNode = XMLConvert.parse(bytes.decodeToString())
}
