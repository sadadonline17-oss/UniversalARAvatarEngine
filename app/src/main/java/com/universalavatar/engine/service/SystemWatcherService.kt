package com.universalavatar.engine.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.universalavatar.engine.R
import com.universalavatar.engine.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    SYSTEM WATCHER SERVICE                                    ║
 * ║                                                                              ║
 * ║  Monitors system-wide camera activation using:                               ║
 * ║  1. AccessibilityService - Window state changes                              ║
 * ║  2. UsageStatsManager - App usage detection                                  ║
 * ║                                                                              ║
 * ║  Triggers AR overlay when ANY app opens camera.                              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class SystemWatcherService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    
    private lateinit var usageStatsManager: UsageStatsManager
    private val cameraApps = ConcurrentHashMap<String, CameraAppInfo>()
    private var isMonitoring = false
    
    // Known camera package patterns
    private val cameraPackagePatterns = listOf(
        "camera",
        "cam",
        "photo",
        "video",
        "snap",
        "instagram",
        "snapchat",
        "tiktok",
        "zoom",
        "teams",
        "meet",
        "whatsapp",
        "telegram",
        "messenger",
        "duo",
        "skype",
        "line",
        "wechat",
        "viber",
        "discord",
        "slack"
    )
    
    // Camera activity patterns
    private val cameraActivityPatterns = listOf(
        "camera",
        "capture",
        "record",
        "video",
        "call",
        "stream",
        "broadcast",
        "live"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        Timber.i("SystemWatcherService connected")
        
        // Configure accessibility service
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            packageNames = null // Monitor all packages
        }
        
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        startForeground(
            Constants.NOTIFICATION_ID_SYSTEM_WATCHER,
            createNotification()
        )
        
        startMonitoring()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChange(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChange(event)
            }
        }
    }

    override fun onInterrupt() {
        Timber.w("SystemWatcherService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        serviceScope.cancel()
        Timber.i("SystemWatcherService destroyed")
    }

    /**
     * Handles window state changes to detect camera activation.
     */
    private fun handleWindowStateChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: return
        
        // Check if this is a camera-related activity
        if (isCameraActivity(packageName, className)) {
            Timber.d("Camera activity detected: $packageName / $className")
            triggerAROverlay(packageName)
        }
    }

    /**
     * Handles window content changes for dynamic camera detection.
     */
    private fun handleWindowContentChange(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Check for camera view elements
        event.source?.let { rootNode ->
            if (hasCameraView(rootNode)) {
                Timber.d("Camera view detected in: $packageName")
                triggerAROverlay(packageName)
            }
            rootNode.recycle()
        }
    }

    /**
     * Checks if the activity is camera-related.
     */
    private fun isCameraActivity(packageName: String, className: String): Boolean {
        val lowerPackage = packageName.lowercase()
        val lowerClass = className.lowercase()
        
        // Check package name patterns
        val packageMatch = cameraPackagePatterns.any { pattern ->
            lowerPackage.contains(pattern)
        }
        
        // Check activity name patterns
        val activityMatch = cameraActivityPatterns.any { pattern ->
            lowerClass.contains(pattern)
        }
        
        return packageMatch && activityMatch
    }

    /**
     * Checks if the view hierarchy contains camera-related elements.
     */
    private fun hasCameraView(rootNode: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        // Check for camera-related view IDs or content descriptions
        val cameraViewIds = listOf(
            "camera",
            "preview",
            "surface",
            "viewfinder",
            "capture"
        )
        
        return cameraViewIds.any { id ->
            rootNode.findAccessibilityNodeInfosByViewId(id).isNotEmpty() ||
            rootNode.contentDescription?.toString()?.lowercase()?.contains(id) == true
        }
    }

    /**
     * Starts continuous monitoring using UsageStatsManager.
     */
    private fun startMonitoring() {
        isMonitoring = true
        
        serviceScope.launch {
            while (isMonitoring && isActive) {
                checkRecentUsage()
                delay(Constants.MONITORING_INTERVAL_MS)
            }
        }
    }

    /**
     * Checks recent app usage for camera activation.
     */
    private fun checkRecentUsage() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - Constants.USAGE_QUERY_WINDOW_MS
        
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                val packageName = event.packageName
                
                if (isCameraPackage(packageName)) {
                    Timber.d("Camera app resumed: $packageName")
                    triggerAROverlay(packageName)
                }
            }
        }
    }

    /**
     * Checks if the package is a known camera app.
     */
    private fun isCameraPackage(packageName: String): Boolean {
        return cameraPackagePatterns.any { pattern ->
            packageName.lowercase().contains(pattern)
        }
    }

    /**
     * Triggers the AR overlay service.
     */
    private fun triggerAROverlay(packageName: String) {
        // Prevent duplicate triggers
        if (cameraApps.containsKey(packageName)) {
            return
        }
        
        cameraApps[packageName] = CameraAppInfo(
            packageName = packageName,
            activationTime = System.currentTimeMillis()
        )
        
        Timber.i("Triggering AR overlay for: $packageName")
        
        // Start the overlay service
        val intent = Intent(this, OverlayARService::class.java).apply {
            putExtra(Constants.EXTRA_TARGET_PACKAGE, packageName)
            putExtra(Constants.EXTRA_ACTIVATION_TIME, System.currentTimeMillis())
        }
        
        startService(intent)
        
        // Notify other components
        sendBroadcast(Intent(Constants.ACTION_CAMERA_DETECTED).apply {
            putExtra(Constants.EXTRA_TARGET_PACKAGE, packageName)
        })
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification() = NotificationCompat.Builder(this, Constants.CHANNEL_SYSTEM_WATCHER)
        .setContentTitle("Universal AR Avatar")
        .setContentText("Monitoring for camera activation...")
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setSilent(true)
        .build()

    /**
     * Data class for tracking camera app info.
     */
    data class CameraAppInfo(
        val packageName: String,
        val activationTime: Long
    )
}
