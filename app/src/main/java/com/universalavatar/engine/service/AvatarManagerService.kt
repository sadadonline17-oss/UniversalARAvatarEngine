package com.universalavatar.engine.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.universalavatar.engine.R
import com.universalavatar.engine.model.Avatar
import com.universalavatar.engine.model.AvatarStyle
import com.universalavatar.engine.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    AVATAR MANAGER SERVICE                                    ║
 * ║                                                                              ║
 * ║  Manages avatar resources and styles:                                        ║
 * ║  - Realistic (photorealistic human avatars)                                  ║
 * ║  - Anime (stylized anime characters)                                         ║
 * ║  - Cartoon (cartoon-style characters)                                        ║
 * ║  - Robot (futuristic robotic avatars)                                        ║
 * ║  - Metahuman (high-fidelity 3D characters)                                   ║
 * ║                                                                              ║
 * ║  Features:                                                                   ║
 * ║  - Avatar loading and caching                                                ║
 * ║  - Custom avatar upload                                                      ║
 * ║  - Style switching                                                           ║
 * ║  - Resource management                                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class AvatarManagerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = AvatarManagerBinder()
    
    // Avatar cache
    private val avatarCache = ConcurrentHashMap<String, Avatar>()
    private val bitmapCache = ConcurrentHashMap<String, Bitmap>()
    
    // Current avatar state
    private var currentAvatar: Avatar? = null
    private var currentStyle: AvatarStyle = AvatarStyle.REALISTIC
    
    // Default avatars for each style
    private val defaultAvatars = mapOf(
        AvatarStyle.REALISTIC to "avatars/default_realistic.png",
        AvatarStyle.ANIME to "avatars/default_anime.png",
        AvatarStyle.CARTOON to "avatars/default_cartoon.png",
        AvatarStyle.ROBOT to "avatars/default_robot.png",
        AvatarStyle.METAHUMAN to "avatars/default_metahuman.png"
    )

    inner class AvatarManagerBinder : Binder() {
        fun getService(): AvatarManagerService = this@AvatarManagerService
        fun getCurrentAvatar(): Avatar? = currentAvatar
        fun getCurrentStyle(): AvatarStyle = currentStyle
        fun switchStyle(style: AvatarStyle) = switchAvatarStyle(style)
        fun loadCustomAvatar(path: String, callback: (Boolean) -> Unit) = loadCustomAvatarInternal(path, callback)
        fun getAvatarBitmap(avatarId: String): Bitmap? = bitmapCache[avatarId]
        fun getAvailableAvatars(style: AvatarStyle): List<Avatar> = getAvatarsByStyle(style)
        fun preloadAvatars() = preloadAllAvatars()
    }

    override fun onCreate() {
        super.onCreate()
        initializeDefaultAvatars()
        Timber.i("AvatarManagerService created")
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            Constants.NOTIFICATION_ID_AVATAR_MANAGER,
            createNotification()
        )
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        clearCache()
        serviceScope.cancel()
        Timber.i("AvatarManagerService destroyed")
    }

    /**
     * Initializes default avatars from assets.
     */
    private fun initializeDefaultAvatars() {
        serviceScope.launch {
            AvatarStyle.values().forEach { style ->
                try {
                    val assetPath = defaultAvatars[style] ?: return@forEach
                    
                    assets.open(assetPath).use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val avatarId = "default_${style.name.lowercase()}"
                        
                        val avatar = Avatar(
                            id = avatarId,
                            name = "Default ${style.name.lowercase().capitalize()}",
                            style = style,
                            assetPath = assetPath,
                            isDefault = true,
                            thumbnail = bitmap
                        )
                        
                        avatarCache[avatarId] = avatar
                        bitmapCache[avatarId] = bitmap
                        
                        Timber.d("Loaded default avatar: $avatarId")
                    }
                    
                } catch (e: IOException) {
                    Timber.e(e, "Failed to load default avatar for style: $style")
                    // Create placeholder
                    createPlaceholderAvatar(style)
                }
            }
            
            // Set current avatar to realistic default
            currentAvatar = avatarCache["default_realistic"]
            Timber.i("Default avatars initialized")
        }
    }

    /**
     * Creates a placeholder avatar for a style.
     */
    private fun createPlaceholderAvatar(style: AvatarStyle) {
        val avatarId = "default_${style.name.lowercase()}_placeholder"
        
        val bitmap = when (style) {
            AvatarStyle.REALISTIC -> createRealisticPlaceholder()
            AvatarStyle.ANIME -> createAnimePlaceholder()
            AvatarStyle.CARTOON -> createCartoonPlaceholder()
            AvatarStyle.ROBOT -> createRobotPlaceholder()
            AvatarStyle.METAHUMAN -> createMetahumanPlaceholder()
        }
        
        val avatar = Avatar(
            id = avatarId,
            name = "${style.name.lowercase().capitalize()} Placeholder",
            style = style,
            assetPath = "",
            isDefault = true,
            isPlaceholder = true,
            thumbnail = bitmap
        )
        
        avatarCache[avatarId] = avatar
        bitmapCache[avatarId] = bitmap
    }

    private fun createRealisticPlaceholder(): Bitmap {
        // Create a simple realistic face placeholder
        return Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#F5D0C5") // Skin tone
            }
            canvas.drawCircle(128f, 128f, 100f, paint)
        }
    }

    private fun createAnimePlaceholder(): Bitmap {
        return Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFE4E1")
            }
            canvas.drawCircle(128f, 128f, 100f, paint)
            // Add anime-style eyes
            paint.color = android.graphics.Color.parseColor("#4A90E2")
            canvas.drawCircle(90f, 110f, 25f, paint)
            canvas.drawCircle(166f, 110f, 25f, paint)
        }
    }

    private fun createCartoonPlaceholder(): Bitmap {
        return Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.YELLOW
            }
            canvas.drawCircle(128f, 128f, 100f, paint)
            // Simple cartoon face
            paint.color = android.graphics.Color.BLACK
            canvas.drawCircle(90f, 100f, 15f, paint)
            canvas.drawCircle(166f, 100f, 15f, paint)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 10f
            canvas.drawArc(80f, 120f, 176f, 200f, 0f, 180f, false, paint)
        }
    }

    private fun createRobotPlaceholder(): Bitmap {
        return Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#C0C0C0")
            }
            canvas.drawRect(56f, 56f, 200f, 200f, paint)
            // Robot eyes
            paint.color = android.graphics.Color.parseColor("#00FF00")
            canvas.drawRect(80f, 90f, 120f, 110f, paint)
            canvas.drawRect(136f, 90f, 176f, 110f, paint)
        }
    }

    private fun createMetahumanPlaceholder(): Bitmap {
        return Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E8D4C4")
                isAntiAlias = true
            }
            canvas.drawCircle(128f, 128f, 100f, paint)
            // High-detail placeholder
            paint.color = android.graphics.Color.parseColor("#4A3728")
            canvas.drawCircle(100f, 110f, 12f, paint)
            canvas.drawCircle(156f, 110f, 12f, paint)
        }
    }

    /**
     * Switches to a different avatar style.
     */
    private fun switchAvatarStyle(style: AvatarStyle) {
        currentStyle = style
        
        val avatarId = "default_${style.name.lowercase()}"
        currentAvatar = avatarCache[avatarId]
        
        // Broadcast style change
        sendBroadcast(Intent(Constants.ACTION_AVATAR_STYLE_CHANGED).apply {
            putExtra(Constants.EXTRA_AVATAR_STYLE, style.name)
        })
        
        Timber.i("Switched to avatar style: $style")
    }

    /**
     * Loads a custom avatar from file path.
     */
    private fun loadCustomAvatarInternal(path: String, callback: (Boolean) -> Unit) {
        serviceScope.launch {
            try {
                val file = File(path)
                if (!file.exists()) {
                    callback(false)
                    return@launch
                }
                
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap == null) {
                    callback(false)
                    return@launch
                }
                
                val avatarId = "custom_${System.currentTimeMillis()}"
                val avatar = Avatar(
                    id = avatarId,
                    name = "Custom Avatar",
                    style = AvatarStyle.REALISTIC,
                    assetPath = path,
                    isDefault = false,
                    isCustom = true,
                    thumbnail = bitmap
                )
                
                avatarCache[avatarId] = avatar
                bitmapCache[avatarId] = bitmap
                currentAvatar = avatar
                
                callback(true)
                Timber.i("Custom avatar loaded: $avatarId")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load custom avatar")
                callback(false)
            }
        }
    }

    /**
     * Gets avatars by style.
     */
    private fun getAvatarsByStyle(style: AvatarStyle): List<Avatar> {
        return avatarCache.values.filter { it.style == style }
    }

    /**
     * Preloads all avatars for smooth switching.
     */
    private fun preloadAllAvatars() {
        serviceScope.launch {
            AvatarStyle.values().forEach { style ->
                if (!avatarCache.containsKey("default_${style.name.lowercase()}")) {
                    createPlaceholderAvatar(style)
                }
            }
            Timber.i("All avatars preloaded")
        }
    }

    /**
     * Clears avatar cache.
     */
    private fun clearCache() {
        bitmapCache.values.forEach { it.recycle() }
        bitmapCache.clear()
        avatarCache.clear()
        currentAvatar = null
    }

    /**
     * Saves avatar to local storage.
     */
    fun saveAvatarToStorage(avatar: Avatar, callback: (String?) -> Unit) {
        serviceScope.launch {
            try {
                val filename = "avatar_${avatar.id}_${System.currentTimeMillis()}.png"
                val file = File(filesDir, filename)
                
                FileOutputStream(file).use { out ->
                    avatar.thumbnail?.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                callback(file.absolutePath)
                Timber.i("Avatar saved: ${file.absolutePath}")
                
            } catch (e: IOException) {
                Timber.e(e, "Failed to save avatar")
                callback(null)
            }
        }
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification() = NotificationCompat.Builder(this, Constants.CHANNEL_AVATAR_MANAGER)
        .setContentTitle("Avatar Manager")
        .setContentText("Current: ${currentStyle.name.lowercase().capitalize()}")
        .setSmallIcon(R.drawable.ic_avatar_manager)
        .setOngoing(true)
        .setSilent(true)
        .build()
}
