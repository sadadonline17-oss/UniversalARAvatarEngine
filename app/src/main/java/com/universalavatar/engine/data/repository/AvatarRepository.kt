package com.universalavatar.engine.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.universalavatar.engine.data.local.AvatarDao
import com.universalavatar.engine.data.local.AvatarEntity
import com.universalavatar.engine.model.Avatar
import com.universalavatar.engine.model.AvatarStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         AVATAR REPOSITORY                                    ║
 * ║                                                                              ║
 * ║  Repository for avatar data operations.                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Singleton
class AvatarRepository @Inject constructor(
    private val context: Context,
    private val avatarDao: AvatarDao
) {
    /**
     * Gets all avatars as flow.
     */
    fun getAllAvatars(): Flow<List<Avatar>> {
        return avatarDao.getAllAvatars().map { entities ->
            entities.map { it.toAvatar() }
        }
    }

    /**
     * Gets avatars by style.
     */
    fun getAvatarsByStyle(style: AvatarStyle): Flow<List<Avatar>> {
        return avatarDao.getAvatarsByStyle(style.name).map { entities ->
            entities.map { it.toAvatar() }
        }
    }

    /**
     * Gets avatar by ID.
     */
    suspend fun getAvatarById(id: String): Avatar? {
        return avatarDao.getAvatarById(id)?.toAvatar()
    }

    /**
     * Gets favorite avatars.
     */
    fun getFavoriteAvatars(): Flow<List<Avatar>> {
        return avatarDao.getFavoriteAvatars().map { entities ->
            entities.map { it.toAvatar() }
        }
    }

    /**
     * Inserts a new avatar.
     */
    suspend fun insertAvatar(avatar: Avatar) {
        avatarDao.insertAvatar(avatar.toEntity())
    }

    /**
     * Updates an avatar.
     */
    suspend fun updateAvatar(avatar: Avatar) {
        avatarDao.updateAvatar(avatar.toEntity())
    }

    /**
     * Deletes an avatar.
     */
    suspend fun deleteAvatar(avatar: Avatar) {
        avatarDao.deleteAvatar(avatar.toEntity())
        
        // Delete thumbnail file if exists
        avatar.thumbnail?.let { bitmap ->
            deleteThumbnailFile(avatar.id)
        }
    }

    /**
     * Sets avatar as favorite.
     */
    suspend fun setFavorite(avatarId: String, isFavorite: Boolean) {
        avatarDao.setFavorite(avatarId, isFavorite)
    }

    /**
     * Updates avatar usage statistics.
     */
    suspend fun updateUsage(avatarId: String) {
        avatarDao.updateUsage(avatarId)
    }

    /**
     * Saves avatar thumbnail to storage.
     */
    suspend fun saveThumbnail(avatarId: String, bitmap: Bitmap): String {
        val file = File(context.filesDir, "thumbnails/$avatarId.png")
        file.parentFile?.mkdirs()
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        return file.absolutePath
    }

    /**
     * Loads avatar thumbnail from storage.
     */
    fun loadThumbnail(path: String?): Bitmap? {
        if (path == null) return null
        return BitmapFactory.decodeFile(path)
    }

    /**
     * Deletes thumbnail file.
     */
    private fun deleteThumbnailFile(avatarId: String) {
        val file = File(context.filesDir, "thumbnails/$avatarId.png")
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Converts AvatarEntity to Avatar.
     */
    private fun AvatarEntity.toAvatar(): Avatar {
        return Avatar(
            id = id,
            name = name,
            style = AvatarStyle.valueOf(style),
            assetPath = assetPath,
            isDefault = isDefault,
            isCustom = isCustom,
            isPlaceholder = isPlaceholder,
            thumbnail = loadThumbnail(thumbnailPath),
            metadata = com.universalavatar.engine.model.AvatarMetadata(
                createdAt = createdAt,
                lastUsedAt = lastUsedAt,
                useCount = useCount,
                isFavorite = isFavorite,
                tags = tags
            )
        )
    }

    /**
     * Converts Avatar to AvatarEntity.
     */
    private fun Avatar.toEntity(): AvatarEntity {
        return AvatarEntity(
            id = id,
            name = name,
            style = style.name,
            assetPath = assetPath,
            isDefault = isDefault,
            isCustom = isCustom,
            isPlaceholder = isPlaceholder,
            thumbnailPath = thumbnail?.let { bitmap ->
                // Save thumbnail and return path
                // This should be done asynchronously in practice
                ""
            },
            createdAt = metadata.createdAt,
            lastUsedAt = metadata.lastUsedAt,
            useCount = metadata.useCount,
            isFavorite = metadata.isFavorite,
            tags = metadata.tags
        )
    }
}
