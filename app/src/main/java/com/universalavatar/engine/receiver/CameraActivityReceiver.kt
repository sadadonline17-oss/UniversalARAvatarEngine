package com.universalavatar.engine.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.universalavatar.engine.service.OverlayARService
import com.universalavatar.engine.util.Constants
import timber.log.Timber

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    CAMERA ACTIVITY RECEIVER                                  ║
 * ║                                                                              ║
 * ║  Receives broadcasts when camera activity is detected.                       ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
class CameraActivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Constants.ACTION_CAMERA_DETECTED -> {
                val packageName = intent.getStringExtra(Constants.EXTRA_TARGET_PACKAGE)
                Timber.d("Camera detected in package: $packageName")
                
                // The overlay service is already started by SystemWatcherService
                // This receiver can be used for additional handling
            }
            
            Constants.ACTION_FACE_DATA -> {
                // Face data received - can be used for analytics or debugging
            }
            
            Constants.ACTION_AVATAR_STYLE_CHANGED -> {
                val style = intent.getStringExtra(Constants.EXTRA_AVATAR_STYLE)
                Timber.d("Avatar style changed to: $style")
            }
        }
    }
}
