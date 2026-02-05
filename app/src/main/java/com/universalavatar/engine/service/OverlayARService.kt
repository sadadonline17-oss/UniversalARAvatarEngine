package com.universalavatar.engine.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
nimport android.hardware.display.VirtualDisplay
nimport android.media.projection.MediaProjection
nimport android.media.projection.MediaProjectionManager
nimport android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.universalavatar.engine.R
import com.universalavatar.engine.renderer.ARSurfaceView
import com.universalavatar.engine.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    OVERLAY AR SERVICE                                        ║
 * ║                                                                              ║
 * ║  Creates a system-wide AR overlay using SYSTEM_ALERT_WINDOW permission.      ║
 * ║  Renders avatar on top of all applications.                                  ║
 * ║                                                                              ║
 * ║  Features:                                                                   ║
 * ║  - Full-screen overlay above all apps                                        ║
 * ║  - Transparent background with AR content                                    ║
 * ║  - Touch-through capability                                                  ║
 * ║  - Hardware-accelerated rendering                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class OverlayARService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val binder = OverlayBinder()
    
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var arSurfaceView: ARSurfaceView? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    
    private var targetPackage: String = ""
    private var isOverlayActive = false
    private var overlayParams: WindowManager.LayoutParams? = null

    inner class OverlayBinder : Binder() {
        fun getService(): OverlayARService = this@OverlayARService
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Timber.i("OverlayARService created")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetPackage = intent?.getStringExtra(Constants.EXTRA_TARGET_PACKAGE) ?: ""
        
        startForeground(
            Constants.NOTIFICATION_ID_OVERLAY_AR,
            createNotification()
        )
        
        if (!isOverlayActive) {
            createOverlay()
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        serviceScope.cancel()
        Timber.i("OverlayARService destroyed")
    }

    /**
     * Creates the AR overlay window.
     */
    private fun createOverlay() {
        try {
            // Create the overlay layout
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            overlayView = inflater.inflate(R.layout.overlay_ar_layout, null)
            
            // Setup AR SurfaceView
            arSurfaceView = overlayView?.findViewById<ARSurfaceView>(R.id.ar_surface_view)?.apply {
                setupRenderer()
                setTargetPackage(targetPackage)
            }
            
            // Configure window parameters
            overlayParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            
            // Add the overlay
            overlayParams?.let { params ->
                windowManager.addView(overlayView, params)
                isOverlayActive = true
                Timber.i("AR overlay created for: $targetPackage")
            }
            
            // Start face tracking
            startFaceTracking()
            
            // Start avatar rendering
            startAvatarRendering()
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to create overlay")
        }
    }

    /**
     * Removes the overlay window.
     */
    private fun removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: IllegalArgumentException) {
                Timber.w("Overlay view already removed")
            }
            overlayView = null
            arSurfaceView = null
            isOverlayActive = false
            Timber.i("AR overlay removed")
        }
        
        virtualDisplay?.release()
        mediaProjection?.stop()
    }

    /**
     * Starts the face tracking service.
     */
    private fun startFaceTracking() {
        val intent = Intent(this, FaceTrackingService::class.java).apply {
            putExtra(Constants.EXTRA_TARGET_PACKAGE, targetPackage)
        }
        startService(intent)
    }

    /**
     * Starts the avatar rendering service.
     */
    private fun startAvatarRendering() {
        val intent = Intent(this, DeepfakeRendererService::class.java).apply {
            putExtra(Constants.EXTRA_TARGET_PACKAGE, targetPackage)
        }
        startService(intent)
    }

    /**
     * Sets up screen capture for virtual camera mode.
     */
    fun setupScreenCapture(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        mediaProjection?.let { projection ->
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi
            
            virtualDisplay = projection.createVirtualDisplay(
                "ARVirtualDisplay",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                arSurfaceView?.holder?.surface,
                null,
                null
            )
            
            Timber.i("Screen capture setup complete")
        }
    }

    /**
     * Updates the avatar type.
     */
    fun setAvatarType(avatarType: String) {
        arSurfaceView?.setAvatarType(avatarType)
    }

    /**
     * Updates avatar expression/emotion.
     */
    fun setExpression(expression: FloatArray) {
        arSurfaceView?.updateExpression(expression)
    }

    /**
     * Updates head pose (yaw, pitch, roll).
     */
    fun setHeadPose(yaw: Float, pitch: Float, roll: Float) {
        arSurfaceView?.updateHeadPose(yaw, pitch, roll)
    }

    /**
     * Toggles the overlay visibility.
     */
    fun toggleOverlay() {
        overlayView?.visibility = if (overlayView?.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    /**
     * Checks if overlay is currently active.
     */
    fun isActive(): Boolean = isOverlayActive

    /**
     * Gets the current AR surface for external composition.
     */
    fun getARSurface(): Surface? = arSurfaceView?.holder?.surface

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification() = NotificationCompat.Builder(this, Constants.CHANNEL_OVERLAY_AR)
        .setContentTitle("AR Avatar Active")
        .setContentText("Overlay running on: $targetPackage")
        .setSmallIcon(R.drawable.ic_overlay_active)
        .setOngoing(true)
        .setSilent(true)
        .addAction(R.drawable.ic_stop, "Stop", createStopPendingIntent())
        .build()

    private fun createStopPendingIntent(): android.app.PendingIntent {
        val intent = Intent(this, OverlayARService::class.java).apply {
            action = Constants.ACTION_STOP_OVERLAY
        }
        return android.app.PendingIntent.getService(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }
}
