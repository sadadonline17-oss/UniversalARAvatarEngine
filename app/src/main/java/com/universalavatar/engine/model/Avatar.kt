package com.universalavatar.engine.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         AVATAR MODEL                                         ║
 * ║                                                                              ║
 * ║  Represents an avatar with its properties and metadata.                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Parcelize
data class Avatar(
    val id: String,
    val name: String,
    val style: AvatarStyle,
    val assetPath: String,
    val isDefault: Boolean = false,
    val isCustom: Boolean = false,
    val isPlaceholder: Boolean = false,
    val thumbnail: Bitmap? = null,
    val metadata: AvatarMetadata = AvatarMetadata()
) : Parcelable {
    
    /**
     * Gets the display name for UI.
     */
    fun getDisplayName(): String {
        return when {
            isPlaceholder -> "$name (Placeholder)"
            isCustom -> "$name (Custom)"
            else -> name
        }
    }
    
    /**
     * Gets the resource path for this avatar.
     */
    fun getResourcePath(): String {
        return if (isCustom) {
            assetPath
        } else {
            "avatars/${style.name.lowercase()}/$id"
        }
    }
    
    companion object {
        /**
         * Creates a default avatar for a style.
         */
        fun createDefault(style: AvatarStyle): Avatar {
            return Avatar(
                id = "default_${style.name.lowercase()}",
                name = "Default ${style.name.lowercase().capitalize()}",
                style = style,
                assetPath = "avatars/default_${style.name.lowercase()}.png",
                isDefault = true
            )
        }
    }
}

/**
 * Avatar style types.
 */
enum class AvatarStyle {
    REALISTIC,
    ANIME,
    CARTOON,
    ROBOT,
    METAHUMAN;
    
    /**
     * Gets display name for UI.
     */
    fun getDisplayName(): String {
        return when (this) {
            REALISTIC -> "Realistic"
            ANIME -> "Anime"
            CARTOON -> "Cartoon"
            ROBOT -> "Robot"
            METAHUMAN -> "Metahuman"
        }
    }
    
    /**
     * Gets description for UI.
     */
    fun getDescription(): String {
        return when (this) {
            REALISTIC -> "Photorealistic human avatars with natural expressions"
            ANIME -> "Stylized anime characters with expressive features"
            CARTOON -> "Fun cartoon-style characters for casual use"
            ROBOT -> "Futuristic robotic avatars with LED effects"
            METAHUMAN -> "High-fidelity 3D characters from MetaHuman"
        }
    }
    
    /**
     * Gets icon resource name.
     */
    fun getIconResource(): String {
        return "ic_avatar_${name.lowercase()}"
    }
}

/**
 * Avatar metadata containing additional information.
 */
@Parcelize
data class AvatarMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val useCount: Int = 0,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val resolution: String = "256x256",
    val fileSize: Long = 0,
    val format: String = "PNG"
) : Parcelable {
    
    /**
     * Creates a copy with updated usage statistics.
     */
    fun withUsage(): AvatarMetadata {
        return copy(
            lastUsedAt = System.currentTimeMillis(),
            useCount = useCount + 1
        )
    }
}

/**
 * Avatar preset configurations.
 */
object AvatarPresets {
    
    val REALISTIC_PRESETS = listOf(
        AvatarPreset("professional", "Professional", "Business attire, neutral expression"),
        AvatarPreset("casual", "Casual", "Everyday look, friendly expression"),
        AvatarPreset("formal", "Formal", "Formal wear, confident expression"),
        AvatarPreset("sporty", "Sporty", "Athletic look, energetic expression")
    )
    
    val ANIME_PRESETS = listOf(
        AvatarPreset("kawaii", "Kawaii", "Cute style with big eyes"),
        AvatarPreset("cool", "Cool", "Stylish and confident"),
        AvatarPreset("moe", "Moe", "Adorable and innocent"),
        AvatarPreset("chibi", "Chibi", "Super deformed style")
    )
    
    val CARTOON_PRESETS = listOf(
        AvatarPreset("classic", "Classic", "Traditional cartoon style"),
        AvatarPreset("modern", "Modern", "Contemporary animation style"),
        AvatarPreset("minimal", "Minimal", "Simple and clean design"),
        AvatarPreset("expressive", "Expressive", "Highly animated features")
    )
    
    val ROBOT_PRESETS = listOf(
        AvatarPreset("android", "Android", "Humanoid robot design"),
        AvatarPreset("mech", "Mech", "Mechanical warrior style"),
        AvatarPreset("cute", "Cute Bot", "Friendly robot companion"),
        AvatarPreset("cyber", "Cyber", "Cyberpunk aesthetic")
    )
    
    val METAHUMAN_PRESETS = listOf(
        AvatarPreset("celebrity", "Celebrity", "Celebrity-like appearance"),
        AvatarPreset("model", "Model", "Fashion model style"),
        AvatarPreset("character", "Character", "Game character quality"),
        AvatarPreset("custom", "Custom", "User-created character")
    )
    
    /**
     * Gets presets for a style.
     */
    fun getPresetsForStyle(style: AvatarStyle): List<AvatarPreset> {
        return when (style) {
            AvatarStyle.REALISTIC -> REALISTIC_PRESETS
            AvatarStyle.ANIME -> ANIME_PRESETS
            AvatarStyle.CARTOON -> CARTOON_PRESETS
            AvatarStyle.ROBOT -> ROBOT_PRESETS
            AvatarStyle.METAHUMAN -> METAHUMAN_PRESETS
        }
    }
}

/**
 * Avatar preset data.
 */
data class AvatarPreset(
    val id: String,
    val name: String,
    val description: String
)
