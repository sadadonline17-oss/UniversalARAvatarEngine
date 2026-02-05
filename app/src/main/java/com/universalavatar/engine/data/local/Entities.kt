package com.universalavatar.engine.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         DATABASE ENTITIES                                    ║
 * ║                                                                              ║
 * ║  Room entities for the Universal AR Avatar Engine.                           ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */

/**
 * Avatar entity.
 */
@Entity(tableName = "avatars")
data class AvatarEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val style: String,
    val assetPath: String,
    val isDefault: Boolean = false,
    val isCustom: Boolean = false,
    val isPlaceholder: Boolean = false,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 0,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
)

/**
 * Effect entity.
 */
@Entity(tableName = "effects")
data class EffectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val color: Int,
    val opacity: Float,
    val isEnabled: Boolean = true,
    val parameters: Map<String, Float> = emptyMap()
)

/**
 * Session entity for tracking usage.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val avatarId: String,
    val targetPackage: String,
    val durationMs: Long = 0,
    val avgFps: Float = 0f,
    val avgLatencyMs: Float = 0f
)
