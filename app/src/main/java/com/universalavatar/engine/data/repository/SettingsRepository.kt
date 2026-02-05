package com.universalavatar.engine.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.universalavatar.engine.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         SETTINGS REPOSITORY                                  ║
 * ║                                                                              ║
 * ║  Repository for app settings using DataStore.                                ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Keys
    private val CURRENT_AVATAR_STYLE = stringPreferencesKey(Constants.PREF_CURRENT_AVATAR_STYLE)
    private val CURRENT_AVATAR_ID = stringPreferencesKey(Constants.PREF_CURRENT_AVATAR_ID)
    private val OVERLAY_ENABLED = booleanPreferencesKey(Constants.PREF_OVERLAY_ENABLED)
    private val AUTO_START = booleanPreferencesKey(Constants.PREF_AUTO_START)
    private val PERFORMANCE_MODE = stringPreferencesKey(Constants.PREF_PERFORMANCE_MODE)
    private val GPU_ACCELERATION = booleanPreferencesKey(Constants.PREF_GPU_ACCELERATION)
    private val BATTERY_OPTIMIZATION = booleanPreferencesKey(Constants.PREF_BATTERY_OPTIMIZATION)
    private val FIRST_RUN = booleanPreferencesKey(Constants.PREF_FIRST_RUN)

    /**
     * Current avatar style.
     */
    val currentAvatarStyle: Flow<String> = dataStore.data.map { prefs ->
        prefs[CURRENT_AVATAR_STYLE] ?: "REALISTIC"
    }

    suspend fun setCurrentAvatarStyle(style: String) {
        dataStore.edit { prefs ->
            prefs[CURRENT_AVATAR_STYLE] = style
        }
    }

    /**
     * Current avatar ID.
     */
    val currentAvatarId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[CURRENT_AVATAR_ID]
    }

    suspend fun setCurrentAvatarId(id: String) {
        dataStore.edit { prefs ->
            prefs[CURRENT_AVATAR_ID] = id
        }
    }

    /**
     * Overlay enabled state.
     */
    val isOverlayEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[OVERLAY_ENABLED] ?: true
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[OVERLAY_ENABLED] = enabled
        }
    }

    /**
     * Auto-start on boot.
     */
    val isAutoStart: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTO_START] ?: true
    }

    suspend fun setAutoStart(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTO_START] = enabled
        }
    }

    /**
     * Performance mode.
     */
    val performanceMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[PERFORMANCE_MODE] ?: Constants.PERFORMANCE_MODE_BALANCED
    }

    suspend fun setPerformanceMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[PERFORMANCE_MODE] = mode
        }
    }

    /**
     * GPU acceleration enabled.
     */
    val isGpuAccelerationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[GPU_ACCELERATION] ?: true
    }

    suspend fun setGpuAcceleration(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[GPU_ACCELERATION] = enabled
        }
    }

    /**
     * Battery optimization enabled.
     */
    val isBatteryOptimizationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BATTERY_OPTIMIZATION] ?: true
    }

    suspend fun setBatteryOptimization(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BATTERY_OPTIMIZATION] = enabled
        }
    }

    /**
     * First run flag.
     */
    val isFirstRun: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[FIRST_RUN] ?: true
    }

    suspend fun setFirstRunComplete() {
        dataStore.edit { prefs ->
            prefs[FIRST_RUN] = false
        }
    }

    /**
     * Clears all settings.
     */
    suspend fun clearSettings() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
