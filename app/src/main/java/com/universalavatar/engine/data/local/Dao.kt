package com.universalavatar.engine.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         DATA ACCESS OBJECTS                                  ║
 * ║                                                                              ║
 * ║  Room DAOs for the Universal AR Avatar Engine.                               ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */

/**
 * Avatar DAO.
 */
@Dao
interface AvatarDao {
    
    @Query("SELECT * FROM avatars ORDER BY lastUsedAt DESC")
    fun getAllAvatars(): Flow<List<AvatarEntity>>
    
    @Query("SELECT * FROM avatars WHERE style = :style ORDER BY lastUsedAt DESC")
    fun getAvatarsByStyle(style: String): Flow<List<AvatarEntity>>
    
    @Query("SELECT * FROM avatars WHERE id = :id")
    suspend fun getAvatarById(id: String): AvatarEntity?
    
    @Query("SELECT * FROM avatars WHERE isFavorite = 1")
    fun getFavoriteAvatars(): Flow<List<AvatarEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvatar(avatar: AvatarEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvatars(avatars: List<AvatarEntity>)
    
    @Update
    suspend fun updateAvatar(avatar: AvatarEntity)
    
    @Delete
    suspend fun deleteAvatar(avatar: AvatarEntity)
    
    @Query("DELETE FROM avatars WHERE id = :id")
    suspend fun deleteAvatarById(id: String)
    
    @Query("UPDATE avatars SET lastUsedAt = :timestamp, useCount = useCount + 1 WHERE id = :id")
    suspend fun updateUsage(id: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE avatars SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)
    
    @Query("SELECT COUNT(*) FROM avatars")
    suspend fun getAvatarCount(): Int
}

/**
 * Effect DAO.
 */
@Dao
interface EffectDao {
    
    @Query("SELECT * FROM effects ORDER BY name ASC")
    fun getAllEffects(): Flow<List<EffectEntity>>
    
    @Query("SELECT * FROM effects WHERE type = :type ORDER BY name ASC")
    fun getEffectsByType(type: String): Flow<List<EffectEntity>>
    
    @Query("SELECT * FROM effects WHERE isEnabled = 1")
    fun getEnabledEffects(): Flow<List<EffectEntity>>
    
    @Query("SELECT * FROM effects WHERE id = :id")
    suspend fun getEffectById(id: String): EffectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffect(effect: EffectEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEffects(effects: List<EffectEntity>)
    
    @Update
    suspend fun updateEffect(effect: EffectEntity)
    
    @Delete
    suspend fun deleteEffect(effect: EffectEntity)
    
    @Query("UPDATE effects SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: String, isEnabled: Boolean)
}

/**
 * Session DAO.
 */
@Dao
interface SessionDao {
    
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE startTime >= :startTime ORDER BY startTime DESC")
    fun getSessionsSince(startTime: Long): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE avatarId = :avatarId ORDER BY startTime DESC")
    fun getSessionsByAvatar(avatarId: String): Flow<List<SessionEntity>>
    
    @Query("SELECT SUM(durationMs) FROM sessions WHERE startTime >= :startTime")
    suspend fun getTotalDurationSince(startTime: Long): Long?
    
    @Query("SELECT AVG(avgFps) FROM sessions WHERE startTime >= :startTime")
    suspend fun getAverageFpsSince(startTime: Long): Float?
    
    @Insert
    suspend fun insertSession(session: SessionEntity): Long
    
    @Update
    suspend fun updateSession(session: SessionEntity)
    
    @Query("UPDATE sessions SET endTime = :endTime, durationMs = :durationMs WHERE id = :id")
    suspend fun endSession(id: Long, endTime: Long, durationMs: Long)
    
    @Delete
    suspend fun deleteSession(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE startTime < :beforeTime")
    suspend fun deleteSessionsBefore(beforeTime: Long)
    
    @Query("SELECT COUNT(*) FROM sessions")
    suspend fun getSessionCount(): Int
}
