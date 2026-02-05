package com.universalavatar.engine.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.universalavatar.engine.service.SystemWatcherService
import com.universalavatar.engine.util.Constants
import timber.log.Timber

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         BOOT RECEIVER                                        ║
 * ║                                                                              ║
 * ║  Automatically starts the SystemWatcherService on device boot.               ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Timber.i("Boot completed - starting Universal AR Avatar services")
            
            // Check if auto-start is enabled
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            val autoStart = prefs.getBoolean(Constants.PREF_AUTO_START, true)
            
            if (autoStart) {
                startServices(context)
            } else {
                Timber.d("Auto-start disabled - skipping service initialization")
            }
        }
    }

    /**
     * Starts all required services.
     */
    private fun startServices(context: Context) {
        // Start System Watcher Service
        val systemWatcherIntent = Intent(context, SystemWatcherService::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(systemWatcherIntent)
        } else {
            context.startService(systemWatcherIntent)
        }
        
        Timber.i("SystemWatcherService started on boot")
    }
}
