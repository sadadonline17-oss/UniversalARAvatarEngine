package com.universalavatar.engine.util

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         CONSTANTS                                            ║
 * ║                                                                              ║
 * ║  Application-wide constants for the Universal AR Avatar Engine.              ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
object Constants {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION CHANNELS
    // ═══════════════════════════════════════════════════════════════════════════
    const val CHANNEL_SYSTEM_WATCHER = "system_watcher_channel"
    const val CHANNEL_OVERLAY_AR = "overlay_ar_channel"
    const val CHANNEL_FACE_TRACKING = "face_tracking_channel"
    const val CHANNEL_DEEPFAKE_RENDERER = "deepfake_renderer_channel"
    const val CHANNEL_VIRTUAL_SCENE = "virtual_scene_channel"
    const val CHANNEL_SCREEN_SHARE = "screen_share_channel"
    const val CHANNEL_AVATAR_MANAGER = "avatar_manager_channel"
    const val CHANNEL_GENERAL = "general_channel"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION IDs
    // ═══════════════════════════════════════════════════════════════════════════
    const val NOTIFICATION_ID_SYSTEM_WATCHER = 1001
    const val NOTIFICATION_ID_OVERLAY_AR = 1002
    const val NOTIFICATION_ID_FACE_TRACKING = 1003
    const val NOTIFICATION_ID_DEEPFAKE_RENDERER = 1004
    const val NOTIFICATION_ID_VIRTUAL_SCENE = 1005
    const val NOTIFICATION_ID_SCREEN_SHARE = 1006
    const val NOTIFICATION_ID_AVATAR_MANAGER = 1007
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BROADCAST ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════
    const val ACTION_CAMERA_DETECTED = "com.universalavatar.action.CAMERA_DETECTED"
    const val ACTION_FACE_DATA = "com.universalavatar.action.FACE_DATA"
    const val ACTION_AVATAR_STYLE_CHANGED = "com.universalavatar.action.AVATAR_STYLE_CHANGED"
    const val ACTION_STOP_OVERLAY = "com.universalavatar.action.STOP_OVERLAY"
    const val ACTION_START_STREAMING = "com.universalavatar.action.START_STREAMING"
    const val ACTION_STOP_STREAMING = "com.universalavatar.action.STOP_STREAMING"
    const val ACTION_SETTINGS_CHANGED = "com.universalavatar.action.SETTINGS_CHANGED"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INTENT EXTRAS
    // ═══════════════════════════════════════════════════════════════════════════
    const val EXTRA_TARGET_PACKAGE = "target_package"
    const val EXTRA_ACTIVATION_TIME = "activation_time"
    const val EXTRA_FACE_DATA = "face_data"
    const val EXTRA_AVATAR_STYLE = "avatar_style"
    const val EXTRA_RESULT_CODE = "result_code"
    const val EXTRA_RESULT_DATA = "result_data"
    const val EXTRA_SETTINGS_KEY = "settings_key"
    const val EXTRA_SETTINGS_VALUE = "settings_value"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE TARGETS
    // ═══════════════════════════════════════════════════════════════════════════
    const val TARGET_FPS = 30
    const val TARGET_LATENCY_MS = 40
    const val MONITORING_INTERVAL_MS = 500L
    const val USAGE_QUERY_WINDOW_MS = 1000L
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FACE TRACKING CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    const val FACE_MESH_LANDMARKS_COUNT = 468
    const val FACE_BLENDSHAPES_COUNT = 52
    const val FACE_DETECTION_CONFIDENCE = 0.5f
    const val FACE_TRACKING_CONFIDENCE = 0.5f
    const val FACE_PRESENCE_CONFIDENCE = 0.5f
    
    // ═══════════════════════════════════════════════════════════════════════════
    // AVATAR CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    const val AVATAR_RESOLUTION = 256
    const val AVATAR_DEFAULT_SCALE = 0.5f
    const val AVATAR_MIN_SCALE = 0.1f
    const val AVATAR_MAX_SCALE = 1.0f
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MODEL FILE NAMES
    // ═══════════════════════════════════════════════════════════════════════════
    const val MODEL_FACE_LANDMARKER = "face_landmarker.task"
    const val MODEL_FIRST_ORDER_MOTION = "first_order_motion.tflite"
    const val MODEL_EXPRESSION_ENCODER = "expression_encoder.tflite"
    const val MODEL_AVATAR_GENERATOR = "avatar_generator.tflite"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHARED PREFERENCES KEYS
    // ═══════════════════════════════════════════════════════════════════════════
    const val PREFS_NAME = "universal_avatar_prefs"
    const val PREF_CURRENT_AVATAR_STYLE = "current_avatar_style"
    const val PREF_CURRENT_AVATAR_ID = "current_avatar_id"
    const val PREF_OVERLAY_ENABLED = "overlay_enabled"
    const val PREF_AUTO_START = "auto_start"
    const val PREF_PERFORMANCE_MODE = "performance_mode"
    const val PREF_GPU_ACCELERATION = "gpu_acceleration"
    const val PREF_BATTERY_OPTIMIZATION = "battery_optimization"
    const val PREF_FIRST_RUN = "first_run"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE MODES
    // ═══════════════════════════════════════════════════════════════════════════
    const val PERFORMANCE_MODE_HIGH = "high"
    const val PERFORMANCE_MODE_BALANCED = "balanced"
    const val PERFORMANCE_MODE_POWER_SAVE = "power_save"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // WEBRTC CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    const val WEBRTC_VIDEO_WIDTH = 1280
    const val WEBRTC_VIDEO_HEIGHT = 720
    const val WEBRTC_VIDEO_FPS = 30
    const val WEBRTC_VIDEO_BITRATE = 2500000 // 2.5 Mbps
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ERROR CODES
    // ═══════════════════════════════════════════════════════════════════════════
    const val ERROR_CAMERA_PERMISSION = 1001
    const val ERROR_OVERLAY_PERMISSION = 1002
    const val ERROR_MEDIA_PROJECTION = 1003
    const val ERROR_ML_MODEL_LOAD = 1004
    const val ERROR_WEBRTC_INIT = 1005
    const val ERROR_FACE_TRACKING = 1006
    
    // ═══════════════════════════════════════════════════════════════════════════
    // REQUEST CODES
    // ═══════════════════════════════════════════════════════════════════════════
    const val REQUEST_OVERLAY_PERMISSION = 1001
    const val REQUEST_MEDIA_PROJECTION = 1002
    const val REQUEST_CAMERA_PERMISSION = 1003
    const val REQUEST_USAGE_STATS = 1004
    const val REQUEST_ACCESSIBILITY = 1005
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DELAYS
    // ═══════════════════════════════════════════════════════════════════════════
    const val DELAY_SERVICE_RESTART = 1000L
    const val DELAY_OVERLAY_ANIMATION = 300L
    const val DELAY_CAMERA_WARMUP = 500L
    
    // ═══════════════════════════════════════════════════════════════════════════
    // FILE PATHS
    // ═══════════════════════════════════════════════════════════════════════════
    const val PATH_AVATARS = "avatars"
    const val PATH_MODELS = "ml-models"
    const val PATH_CACHE = "cache"
    const val PATH_RECORDINGS = "recordings"
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ANIMATION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════
    const val ANIMATION_DURATION_SHORT = 150L
    const val ANIMATION_DURATION_MEDIUM = 300L
    const val ANIMATION_DURATION_LONG = 500L
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BATTERY OPTIMIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    const val BATTERY_LOW_THRESHOLD = 20
    const val BATTERY_CRITICAL_THRESHOLD = 10
    const val THROTTLE_FPS_LOW_BATTERY = 15
    const val THROTTLE_FPS_CRITICAL_BATTERY = 10
}
