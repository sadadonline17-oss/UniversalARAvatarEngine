package com.universalavatar.engine.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         TYPE CONVERTERS                                      ║
 * ║                                                                              ║
 * ║  Room type converters for complex data types.                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
class Converters {
    
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringFloatMap(value: Map<String, Float>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringFloatMap(value: String): Map<String, Float> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
