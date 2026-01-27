package com.vci.vectorcamapp.core.data.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HierarchySerializer {
    private val gson = Gson()
    private val mapType = object : TypeToken<Map<String, String>>() {}.type

    fun serialize(hierarchy: Map<String, String>?): String? {
        if (hierarchy == null) return null
        return gson.toJson(hierarchy)
    }

    fun deserialize(json: String?): Map<String, String> {
        if (json.isNullOrEmpty()) return emptyMap()
        return try {
            gson.fromJson(json, mapType)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
