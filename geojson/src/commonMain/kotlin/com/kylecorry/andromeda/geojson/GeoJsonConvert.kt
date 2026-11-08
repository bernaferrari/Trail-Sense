package com.kylecorry.andromeda.geojson

import kotlinx.serialization.json.*

object GeoJsonConvert {
    fun fromJson(json: String): GeoJsonObject? = runCatching { decode(Json.parseToJsonElement(json).jsonObject) }.getOrNull()
    fun toJson(obj: GeoJsonObject): String = encode(obj).toString()

    private fun decode(obj: JsonObject): GeoJsonObject {
        val box = obj["bbox"]?.takeUnless { it is JsonNull }?.jsonArray?.let(::box)
        val coordinates = obj["coordinates"]?.takeUnless { it is JsonNull }
        fun positions(value: JsonElement) = value.jsonArray.map(::position)
        fun lines(value: JsonElement) = value.jsonArray.map(::positions)
        return when (obj.getValue("type").jsonPrimitive.content) {
            "Feature" -> GeoJsonFeature(obj["id"]?.let(::fromValue), obj["geometry"]?.takeUnless { it is JsonNull }
                ?.jsonObject?.let { decode(it) as GeoJsonGeometry },
                obj["properties"]?.takeUnless { it is JsonNull }?.jsonObject?.mapValues { fromValue(it.value) }, box)
            "FeatureCollection" -> GeoJsonFeatureCollection(obj.getValue("features").jsonArray.map {
                decode(it.jsonObject) as GeoJsonFeature
            }, box)
            "Point" -> GeoJsonPoint(coordinates?.let(::position), box)
            "LineString" -> GeoJsonLineString(coordinates?.let(::positions), box)
            "Polygon" -> GeoJsonPolygon(coordinates?.let(::lines), box)
            "MultiPoint" -> GeoJsonMultiPoint(coordinates?.let(::positions), box)
            "MultiLineString" -> GeoJsonMultiLineString(coordinates?.let(::lines), box)
            "MultiPolygon" -> GeoJsonMultiPolygon(coordinates?.jsonArray?.map(::lines), box)
            "GeometryCollection" -> GeoJsonGeometryCollection(obj.getValue("geometries").jsonArray.map {
                decode(it.jsonObject) as GeoJsonGeometry
            }, box)
            else -> error("Unknown GeoJSON type")
        }
    }
    private fun position(value: JsonElement): GeoJsonPosition {
        val a = value.jsonArray
        return GeoJsonPosition(a[0].jsonPrimitive.double, a[1].jsonPrimitive.double, a.getOrNull(2)?.jsonPrimitive?.double)
    }
    private fun box(a: JsonArray): GeoJsonBoundingBox {
        val values = a.map { it.jsonPrimitive.double }
        return if (values.size == 6) GeoJsonBoundingBox(values[0], values[1], values[3], values[4], values[2], values[5])
        else GeoJsonBoundingBox(values[0], values[1], values[2], values[3])
    }
    private fun fromValue(value: JsonElement): Any? = when (value) {
        is JsonNull -> null
        is JsonObject -> value.mapValues { fromValue(it.value) }
        is JsonArray -> value.map(::fromValue)
        is JsonPrimitive -> if (value.isString) value.content else value.booleanOrNull ?: value.longOrNull ?: value.double
    }
    private fun jsonValue(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(value.entries.associate { it.key.toString() to jsonValue(it.value) })
        is Iterable<*> -> JsonArray(value.map(::jsonValue))
        is Array<*> -> JsonArray(value.map(::jsonValue))
        is IntArray -> JsonArray(value.map(::JsonPrimitive))
        is LongArray -> JsonArray(value.map(::JsonPrimitive))
        is FloatArray -> JsonArray(value.map(::JsonPrimitive))
        is DoubleArray -> JsonArray(value.map(::JsonPrimitive))
        is BooleanArray -> JsonArray(value.map(::JsonPrimitive))
        else -> error("Unsupported GeoJSON property value: ${value::class.simpleName}")
    }
    private fun encode(obj: GeoJsonObject): JsonObject = buildJsonObject {
        put("type", obj.type)
        obj.boundingBox?.let { box ->
            put("bbox", JsonArray(listOfNotNull(box.west, box.south, box.minZ, box.east, box.north, box.maxZ)
                .map(::JsonPrimitive)))
        }
        fun pos(p: GeoJsonPosition) = JsonArray(listOfNotNull(p.x, p.y, p.z).map(::JsonPrimitive))
        fun line(p: List<GeoJsonPosition>) = JsonArray(p.map(::pos))
        fun polygon(p: List<List<GeoJsonPosition>>) = JsonArray(p.map(::line))
        when (obj) {
            is GeoJsonFeature -> {
                obj.id?.let { put("id", jsonValue(it)) }
                put("geometry", obj.geometry?.let(::encode) ?: JsonNull)
                put("properties", jsonValue(obj.properties))
            }
            is GeoJsonFeatureCollection -> put("features", JsonArray(obj.features.map(::encode)))
            is GeoJsonPoint -> obj.point?.let { put("coordinates", pos(it)) }
            is GeoJsonLineString -> obj.line?.let { put("coordinates", line(it)) }
            is GeoJsonPolygon -> obj.polygon?.let { put("coordinates", polygon(it)) }
            is GeoJsonMultiPoint -> obj.points?.let { put("coordinates", line(it)) }
            is GeoJsonMultiLineString -> obj.lines?.let { put("coordinates", polygon(it)) }
            is GeoJsonMultiPolygon -> obj.polygons?.let { put("coordinates", JsonArray(it.map(::polygon))) }
            is GeoJsonGeometryCollection -> put("geometries", JsonArray(obj.geometries.map(::encode)))
        }
    }
}
