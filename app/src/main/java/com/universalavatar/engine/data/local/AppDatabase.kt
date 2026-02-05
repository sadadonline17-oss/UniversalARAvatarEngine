package com.universalavatar.engine.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         APP DATABASE                                         ║
 * ║                                                                              ║
 * ║  Room database for the Universal AR Avatar Engine.                           ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Database(
    entities = [
        AvatarEntity::class,
        EffectEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun avatarDao(): AvatarDao
    abstract fun effectDao(): EffectDao
    abstract fun sessionDao(): SessionDao
}
