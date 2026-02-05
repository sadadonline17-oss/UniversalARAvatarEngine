package com.universalavatar.engine.model

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         AR EFFECT MODEL                                      ║
 * ║                                                                              ║
 * ║  Represents an AR effect that can be applied to the composed scene.          ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Parcelize
data class AREffect(
    val id: String,
    val name: String,
    val type: Type,
    val color: Int = Color.WHITE,
    val opacity: Float = 1.0f,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f,
    val animationSpeed: Float = 1.0f,
    val isEnabled: Boolean = true,
    val overlayBitmap: Bitmap? = null,
    val colorMatrix: ColorMatrix = ColorMatrix(),
    val particles: List<Particle> = emptyList(),
    val animationFrames: List<Bitmap> = emptyList(),
    private var currentFrameIndex: Int = 0
) : Parcelable {
    
    enum class Type {
        PARTICLE,    // Floating particles (sparkles, dust, etc.)
        FILTER,      // Color filters (sepia, black & white, etc.)
        OVERLAY,     // Image overlays (stickers, frames, etc.)
        ANIMATION,   // Animated effects
        LIGHTING,    // Lighting effects (glow, bloom, etc.)
        DISTORTION,  // Distortion effects (ripple, wave, etc.)
        MASK,        // Face masks and filters
        BACKGROUND   // Background replacement
    }
    
    /**
     * Gets the current animation frame.
     */
    fun getCurrentFrame(): Bitmap? {
        if (animationFrames.isEmpty()) return null
        return animationFrames[currentFrameIndex]
    }
    
    /**
     * Advances to the next animation frame.
     */
    fun nextFrame() {
        if (animationFrames.isNotEmpty()) {
            currentFrameIndex = (currentFrameIndex + 1) % animationFrames.size
        }
    }
    
    /**
     * Updates particle positions.
     */
    fun updateParticles(deltaTime: Float) {
        particles.forEach { particle ->
            particle.update(deltaTime)
        }
    }
    
    companion object {
        /**
         * Creates a sparkle particle effect.
         */
        fun createSparkleEffect(
            count: Int = 50,
            color: Int = Color.YELLOW
        ): AREffect {
            val particles = List(count) {
                Particle(
                    x = Math.random().toFloat(),
                    y = Math.random().toFloat(),
                    size = (2 + Math.random() * 4).toFloat(),
                    velocityX = (Math.random() - 0.5).toFloat() * 0.01f,
                    velocityY = (Math.random() - 0.5).toFloat() * 0.01f,
                    lifetime = (1000 + Math.random() * 2000).toFloat()
                )
            }
            
            return AREffect(
                id = "sparkle_${System.currentTimeMillis()}",
                name = "Sparkles",
                type = Type.PARTICLE,
                color = color,
                particles = particles
            )
        }
        
        /**
         * Creates a sepia filter effect.
         */
        fun createSepiaFilter(): AREffect {
            val colorMatrix = ColorMatrix().apply {
                setSaturation(0f)
                postConcat(ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            
            return AREffect(
                id = "sepia_filter",
                name = "Sepia",
                type = Type.FILTER,
                colorMatrix = colorMatrix
            )
        }
        
        /**
         * Creates a black & white filter effect.
         */
        fun createBlackAndWhiteFilter(): AREffect {
            val colorMatrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            
            return AREffect(
                id = "bw_filter",
                name = "Black & White",
                type = Type.FILTER,
                colorMatrix = colorMatrix
            )
        }
        
        /**
         * Creates a neon glow effect.
         */
        fun createNeonGlowEffect(glowColor: Int = Color.CYAN): AREffect {
            val colorMatrix = ColorMatrix().apply {
                // Enhance colors and add glow effect
                set(floatArrayOf(
                    1.5f, 0f, 0f, 0f, 0f,
                    0f, 1.5f, 0f, 0f, 0f,
                    0f, 0f, 1.5f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            
            return AREffect(
                id = "neon_glow_${glowColor}",
                name = "Neon Glow",
                type = Type.LIGHTING,
                color = glowColor,
                colorMatrix = colorMatrix
            )
        }
        
        /**
         * Creates a vintage film effect.
         */
        fun createVintageEffect(): AREffect {
            val colorMatrix = ColorMatrix().apply {
                // Reduce contrast and add yellow tint
                set(floatArrayOf(
                    0.8f, 0.1f, 0.1f, 0f, 20f,
                    0.1f, 0.8f, 0.1f, 0f, 10f,
                    0.1f, 0.1f, 0.7f, 0f, 0f,
                    0f, 0f, 0f, 0.9f, 0f
                ))
            }
            
            return AREffect(
                id = "vintage_filter",
                name = "Vintage",
                type = Type.FILTER,
                colorMatrix = colorMatrix
            )
        }
        
        /**
         * Creates a cyberpunk effect.
         */
        fun createCyberpunkEffect(): AREffect {
            val colorMatrix = ColorMatrix().apply {
                // Enhance cyan and magenta
                set(floatArrayOf(
                    1.2f, 0f, 0.2f, 0f, 0f,
                    0f, 1.5f, 0.5f, 0f, 0f,
                    0.2f, 0.5f, 1.5f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            
            return AREffect(
                id = "cyberpunk_filter",
                name = "Cyberpunk",
                type = Type.FILTER,
                colorMatrix = colorMatrix
            )
        }
    }
}

/**
 * Particle for particle effects.
 */
data class Particle(
    var x: Float,
    var y: Float,
    val size: Float,
    var velocityX: Float,
    var velocityY: Float,
    var lifetime: Float,
    var age: Float = 0f
) {
    /**
     * Updates particle position.
     */
    fun update(deltaTime: Float) {
        x += velocityX * deltaTime
        y += velocityY * deltaTime
        age += deltaTime
        
        // Wrap around screen
        if (x < 0) x = 1f
        if (x > 1) x = 0f
        if (y < 0) y = 1f
        if (y > 1) y = 0f
    }
    
    /**
     * Checks if particle is still alive.
     */
    fun isAlive(): Boolean = age < lifetime
    
    /**
     * Gets current opacity based on lifetime.
     */
    fun getOpacity(): Float {
        val progress = age / lifetime
        return when {
            progress < 0.2f -> progress / 0.2f // Fade in
            progress > 0.8f -> (1 - progress) / 0.2f // Fade out
            else -> 1f
        }
    }
}

/**
 * Predefined AR effects collection.
 */
object AREffectsCollection {
    
    val ALL_EFFECTS = listOf(
        AREffect.createSparkleEffect(),
        AREffect.createSepiaFilter(),
        AREffect.createBlackAndWhiteFilter(),
        AREffect.createNeonGlowEffect(),
        AREffect.createVintageEffect(),
        AREffect.createCyberpunkEffect()
    )
    
    val PARTICLE_EFFECTS = listOf(
        AREffect.createSparkleEffect(count = 30, color = Color.YELLOW),
        AREffect.createSparkleEffect(count = 50, color = Color.CYAN),
        AREffect.createSparkleEffect(count = 40, color = Color.MAGENTA)
    )
    
    val FILTER_EFFECTS = listOf(
        AREffect.createSepiaFilter(),
        AREffect.createBlackAndWhiteFilter(),
        AREffect.createVintageEffect(),
        AREffect.createCyberpunkEffect()
    )
    
    val LIGHTING_EFFECTS = listOf(
        AREffect.createNeonGlowEffect(Color.CYAN),
        AREffect.createNeonGlowEffect(Color.MAGENTA),
        AREffect.createNeonGlowEffect(Color.GREEN)
    )
    
    /**
     * Gets effects by type.
     */
    fun getEffectsByType(type: AREffect.Type): List<AREffect> {
        return ALL_EFFECTS.filter { it.type == type }
    }
    
    /**
     * Gets effect by ID.
     */
    fun getEffectById(id: String): AREffect? {
        return ALL_EFFECTS.find { it.id == id }
    }
}
