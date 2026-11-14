package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import com.kylecorry.andromeda.preferences.putAll

import com.kylecorry.andromeda.preferences.getAll

import com.kylecorry.andromeda.preferences.Preference
import com.kylecorry.andromeda.preferences.PreferenceType
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.text.LevenshteinDistance

class MapLayerPreferenceRepo(private val prefs: Preferences) {
    private val distanceMetric = LevenshteinDistance()

    fun getActiveLayerIds(mapId: String): List<String> {
        return prefs.getString("pref_${mapId}_active_layers")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    fun setActiveLayerIds(mapId: String, layerIds: List<String>) {
        prefs.putString("pref_${mapId}_active_layers", layerIds.joinToString(","))
    }

    fun getLayerPreferences(
        mapId: String,
        layerIds: List<String>
    ): Map<String, List<Preference>> {
        val all = prefs.getAll()
        val preferences = mutableMapOf<String, List<Preference>>()
        for (layerId in layerIds) {
            val prefix = getPreferenceKey(mapId, layerId, "")
            preferences[layerId] = all.filter { it.key.startsWith(prefix) }
                .map { it.copy(key = it.key.replaceFirst(prefix, "")) }
        }
        return preferences
    }

    fun getLayerPreferenceValues(
        mapId: String,
        layerIds: List<String>
    ): Map<String, MapLayerValues> {
        val preferences = getLayerPreferences(mapId, layerIds)
        return layerIds.associateWith { layerId ->
            val layerPreferences = preferences[layerId] ?: emptyList()
            MapLayerValues(layerPreferences.associate { preference ->
                preference.key to when (preference.type) {
                    PreferenceType.Int -> preference.value as Int
                    PreferenceType.Boolean -> preference.value as Boolean
                    PreferenceType.String -> preference.value as String
                    PreferenceType.Float -> preference.value as Float
                    PreferenceType.Long -> preference.value as Long
                }
            })
        }
    }

    fun copyLayerPreferences(
        layerId: String,
        fromMapId: String,
        toMapIds: List<String>
    ) {
        val sourceLayerOrder = getActiveLayerIds(fromMapId)

        // Clear existing preferences for the destination
        for (mapId in toMapIds) {
            removeLayerPreferences(mapId, layerId)
        }

        // Copy over the new preferences
        val fromPreferences =
            getLayerPreferences(fromMapId, listOf(layerId))[layerId] ?: emptyList()
        for (mapId in toMapIds) {
            val newPreferences =
                fromPreferences.map { it.copy(key = getPreferenceKey(mapId, layerId, it.key)) }
            prefs.putAll(newPreferences)

            // Add the layer to the destination's active layers if not present
            addLayerInBestPosition(mapId, layerId, sourceLayerOrder)
        }
    }

    fun addLayerInBestPosition(
        mapId: String,
        layerId: String,
        idealOrdering: List<String>
    ) {
        val destinationLayers = getActiveLayerIds(mapId)
        if (!destinationLayers.contains(layerId)) {
            val newLayers =
                insertLayer(layerId, idealOrdering, destinationLayers)
            setActiveLayerIds(mapId, newLayers)
        }
    }

    private fun insertLayer(
        layerId: String,
        sourceOrder: List<String>,
        destinationOrder: List<String>
    ): List<String> {
        val sourceString = sourceOrder.joinToString(",")
        var bestDistance = Int.MAX_VALUE
        var bestOrder = listOf(layerId) + destinationOrder
        for (i in 0..destinationOrder.size) {
            val newOrder = destinationOrder.toMutableList()
            newOrder.add(i, layerId)
            val newString = newOrder.joinToString(",")
            val distance = distanceMetric.editDistance(sourceString, newString)
            if (distance < bestDistance) {
                bestDistance = distance
                bestOrder = newOrder
            }
        }
        return bestOrder
    }


    fun removeLayerPreferences(mapId: String, layerId: String) {
        val toPreferences = getLayerPreferences(mapId, listOf(layerId))[layerId] ?: emptyList()
        for (preference in toPreferences) {
            prefs.remove(getPreferenceKey(mapId, layerId, preference.key))
        }
    }

    companion object {
        fun getPreferenceKey(mapId: String, layerId: String, preferenceId: String): String {
            return "pref_${mapId}_${layerId}_layer_${preferenceId}"
        }
    }

}
