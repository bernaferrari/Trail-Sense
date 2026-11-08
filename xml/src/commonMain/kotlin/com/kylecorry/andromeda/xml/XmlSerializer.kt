package com.kylecorry.andromeda.xml

class XmlSerializer(private val isRoot: Boolean = true) {
    fun serialize(value: XMLNode): ByteArray = XMLConvert.toString(value, isRoot).encodeToByteArray()
    fun deserialize(bytes: ByteArray): XMLNode = XMLConvert.parse(bytes.decodeToString())
}
