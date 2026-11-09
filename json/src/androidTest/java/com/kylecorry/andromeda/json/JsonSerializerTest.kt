package com.kylecorry.andromeda.json

import org.junit.Test

import org.junit.Assert.*
import kotlinx.serialization.Serializable

class JsonSerializerTest {

    @Test
    fun serialize() {
        val obj = TestClass(1, 1.2, "test")
        val bytes = KotlinJsonSerializer(TestClass.serializer()).serialize(obj)
        assertEquals("{\"something\":1,\"other\":1.2,\"thing\":\"test\"}", bytes.decodeToString())
    }

    @Test
    fun deserialize(){
        val json = "{\"other\":1.2,\"something\":1,\"thing\":\"test\"}"
        val obj =KotlinJsonSerializer(TestClass.serializer()).deserialize(json.encodeToByteArray())
        val expected = TestClass(1, 1.2, "test")
        assertEquals(expected, obj)
    }

    @Serializable
    data class TestClass(val something: Int, val other: Double, val thing: String)

}