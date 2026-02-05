package com.universalavatar.engine.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.os.Binder
import android.os.IBinder
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.universalavatar.engine.R
import com.universalavatar.engine.model.FaceData
import com.universalavatar.engine.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    FACE TRACKING SERVICE                                     ║
 * ║                                                                              ║
 * ║  Real-time face tracking using MediaPipe Face Mesh (468 points).             ║
 * ║                                                                              ║
 * ║  Output:                                                                     ║
 * ║  - yaw, pitch, roll (head pose)                                              ║
 * ║  - eye_ratio (eye openness)                                                  ║
 * ║  - mouth_ratio (mouth openness)                                              ║
 * ║  - emotion_vector (expression encoding)                                      ║
 * ║                                                                              ║
 * ║  Performance: 30 FPS, < 40ms latency                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class FaceTrackingService : LifecycleService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = FaceTrackingBinder()
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var faceLandmarker: FaceLandmarker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    
    private var targetPackage: String = ""
    private var isTracking = false
    private var lastProcessingTimeMs = 0L
    
    // Face data callbacks
    private val faceDataListeners = mutableListOf<(FaceData) -> Unit>()

    inner class FaceTrackingBinder : Binder() {
        fun getService(): FaceTrackingService = this@FaceTrackingService
        fun addFaceDataListener(listener: (FaceData) -> Unit) {
            faceDataListeners.add(listener)
        }
        fun removeFaceDataListener(listener: (FaceData) -> Unit) {
            faceDataListeners.remove(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeFaceLandmarker()
        Timber.i("FaceTrackingService created")
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        targetPackage = intent?.getStringExtra(Constants.EXTRA_TARGET_PACKAGE) ?: ""
        
        startForeground(
            Constants.NOTIFICATION_ID_FACE_TRACKING,
            createNotification()
        )
        
        if (!isTracking) {
            startCamera()
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        faceLandmarker?.close()
        executor.shutdown()
        serviceScope.cancel()
        Timber.i("FaceTrackingService destroyed")
    }

    /**
     * Initializes MediaPipe Face Landmarker with GPU acceleration.
     */
    private fun initializeFaceLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.GPU)
                .setModelAssetPath("face_landmarker.task")
                .build()
            
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setOutputFaceBlendshapes(true)
                .setOutputFacialTransformationMatrixes(true)
                .setResultListener { result, inputImage ->
                    processFaceResult(result, inputImage)
                }
                .setErrorListener { error ->
                    Timber.e("FaceLandmarker error: $error")
                }
                .build()
            
            faceLandmarker = FaceLandmarker.createFromOptions(this, options)
            Timber.i("FaceLandmarker initialized with GPU delegate")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize FaceLandmarker")
            // Fallback to CPU
            initializeFaceLandmarkerCPU()
        }
    }

    /**
     * Fallback initialization with CPU delegate.
     */
    private fun initializeFaceLandmarkerCPU() {
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath("face_landmarker.task")
                .build()
            
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setOutputFaceBlendshapes(true)
                .setOutputFacialTransformationMatrixes(true)
                .setResultListener { result, inputImage ->
                    processFaceResult(result, inputImage)
                }
                .build()
            
            faceLandmarker = FaceLandmarker.createFromOptions(this, options)
            Timber.i("FaceLandmarker initialized with CPU delegate")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize FaceLandmarker on CPU")
        }
    }

    /**
     * Starts the camera for face tracking.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Setup image analysis
                imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                    .apply {
                        setAnalyzer(executor) { imageProxy ->
                            processImage(imageProxy)
                        }
                    }
                
                // Select front camera
                cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                
                // Bind use cases
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    imageAnalysis
                )
                
                isTracking = true
                Timber.i("Camera started for face tracking")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to start camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Stops the camera.
     */
    private fun stopCamera() {
        cameraProvider?.unbindAll()
        isTracking = false
        Timber.i("Camera stopped")
    }

    /**
     * Processes camera image for face detection.
     */
    private fun processImage(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()
        
        try {
            val bitmap = imageProxy.toBitmap()
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            faceLandmarker?.detectAsync(mpImage, System.currentTimeMillis())
            
            lastProcessingTimeMs = System.currentTimeMillis() - startTime
            
        } catch (e: Exception) {
            Timber.e(e, "Error processing image")
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Processes face detection result.
     */
    private fun processFaceResult(result: FaceLandmarkerResult, image: MPImage) {
        if (result.faceLandmarks().isEmpty()) {
            return
        }
        
        val landmarks = result.faceLandmarks()[0]
        val blendshapes = result.faceBlendshapes()
        val transformationMatrix = result.facialTransformationMatrixes()
        
        // Extract head pose
        val (yaw, pitch, roll) = extractHeadPose(transformationMatrix)
        
        // Extract eye ratio
        val eyeRatio = calculateEyeRatio(landmarks)
        
        // Extract mouth ratio
        val mouthRatio = calculateMouthRatio(landmarks)
        
        // Extract emotion vector from blendshapes
        val emotionVector = extractEmotionVector(blendshapes)
        
        // Create face data
        val faceData = FaceData(
            timestamp = System.currentTimeMillis(),
            landmarks = landmarks.map { landmark ->
                floatArrayOf(landmark.x(), landmark.y(), landmark.z())
            },
            yaw = yaw,
            pitch = pitch,
            roll = roll,
            eyeRatio = eyeRatio,
            mouthRatio = mouthRatio,
            emotionVector = emotionVector,
            processingTimeMs = lastProcessingTimeMs
        )
        
        // Notify listeners
        faceDataListeners.forEach { listener ->
            try {
                listener(faceData)
            } catch (e: Exception) {
                Timber.e(e, "Error in face data listener")
            }
        }
        
        // Broadcast face data
        broadcastFaceData(faceData)
    }

    /**
     * Extracts head pose (yaw, pitch, roll) from transformation matrix.
     */
    private fun extractHeadPose(matrix: java.util.Optional<com.google.mediapipe.formats.proto.MatrixDataProto.MatrixData>): Triple<Float, Float, Float> {
        return if (matrix.isPresent) {
            val m = matrix.get()
            // Extract rotation from 4x4 matrix
            val yaw = atan2(m.getData(8), m.getData(10)) * (180 / PI).toFloat()
            val pitch = atan2(-m.getData(6), sqrt(m.getData(0) * m.getData(0) + m.getData(2) * m.getData(2))) * (180 / PI).toFloat()
            val roll = atan2(m.getData(4), m.getData(5)) * (180 / PI).toFloat()
            Triple(yaw, pitch, roll)
        } else {
            Triple(0f, 0f, 0f)
        }
    }

    /**
     * Calculates eye openness ratio.
     */
    private fun calculateEyeRatio(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        // MediaPipe face mesh eye landmarks
        // Left eye: 33, 160, 158, 133, 153, 144
        // Right eye: 362, 385, 387, 263, 373, 380
        
        val leftEyeTop = landmarks[159]
        val leftEyeBottom = landmarks[145]
        val leftEyeLeft = landmarks[33]
        val leftEyeRight = landmarks[133]
        
        val leftEyeHeight = distance(leftEyeTop, leftEyeBottom)
        val leftEyeWidth = distance(leftEyeLeft, leftEyeRight)
        
        val rightEyeTop = landmarks[386]
        val rightEyeBottom = landmarks[374]
        val rightEyeLeft = landmarks[362]
        val rightEyeRight = landmarks[263]
        
        val rightEyeHeight = distance(rightEyeTop, rightEyeBottom)
        val rightEyeWidth = distance(rightEyeLeft, rightEyeRight)
        
        val leftRatio = leftEyeHeight / (leftEyeWidth + 1e-6f)
        val rightRatio = rightEyeHeight / (rightEyeWidth + 1e-6f)
        
        return (leftRatio + rightRatio) / 2f
    }

    /**
     * Calculates mouth openness ratio.
     */
    private fun calculateMouthRatio(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        // MediaPipe face mesh mouth landmarks
        // Top lip: 0, 11, 12, 13
        // Bottom lip: 16, 17, 18, 19
        // Corners: 61, 291
        
        val mouthTop = landmarks[13]
        val mouthBottom = landmarks[17]
        val mouthLeft = landmarks[61]
        val mouthRight = landmarks[291]
        
        val mouthHeight = distance(mouthTop, mouthBottom)
        val mouthWidth = distance(mouthLeft, mouthRight)
        
        return mouthHeight / (mouthWidth + 1e-6f)
    }

    /**
     * Extracts emotion vector from blendshapes.
     */
    private fun extractEmotionVector(blendshapes: java.util.Optional<com.google.mediapipe.tasks.components.containers.Classifications>): FloatArray {
        val emotionMap = mutableMapOf<String, Float>()
        
        if (blendshapes.isPresent) {
            val categories = blendshapes.get().categories()
            categories.forEach { category ->
                emotionMap[category.categoryName()] = category.score()
            }
        }
        
        // Map blendshapes to emotion vector
        return floatArrayOf(
            emotionMap["browDownLeft"] ?: 0f,
            emotionMap["browDownRight"] ?: 0f,
            emotionMap["browInnerUp"] ?: 0f,
            emotionMap["browOuterUpLeft"] ?: 0f,
            emotionMap["browOuterUpRight"] ?: 0f,
            emotionMap["cheekPuff"] ?: 0f,
            emotionMap["cheekSquintLeft"] ?: 0f,
            emotionMap["cheekSquintRight"] ?: 0f,
            emotionMap["eyeBlinkLeft"] ?: 0f,
            emotionMap["eyeBlinkRight"] ?: 0f,
            emotionMap["eyeLookDownLeft"] ?: 0f,
            emotionMap["eyeLookDownRight"] ?: 0f,
            emotionMap["eyeLookInLeft"] ?: 0f,
            emotionMap["eyeLookInRight"] ?: 0f,
            emotionMap["eyeLookOutLeft"] ?: 0f,
            emotionMap["eyeLookOutRight"] ?: 0f,
            emotionMap["eyeLookUpLeft"] ?: 0f,
            emotionMap["eyeLookUpRight"] ?: 0f,
            emotionMap["eyeSquintLeft"] ?: 0f,
            emotionMap["eyeSquintRight"] ?: 0f,
            emotionMap["eyeWideLeft"] ?: 0f,
            emotionMap["eyeWideRight"] ?: 0f,
            emotionMap["jawForward"] ?: 0f,
            emotionMap["jawLeft"] ?: 0f,
            emotionMap["jawOpen"] ?: 0f,
            emotionMap["jawRight"] ?: 0f,
            emotionMap["mouthClose"] ?: 0f,
            emotionMap["mouthDimpleLeft"] ?: 0f,
            emotionMap["mouthDimpleRight"] ?: 0f,
            emotionMap["mouthFrownLeft"] ?: 0f,
            emotionMap["mouthFrownRight"] ?: 0f,
            emotionMap["mouthFunnel"] ?: 0f,
            emotionMap["mouthLeft"] ?: 0f,
            emotionMap["mouthLowerDownLeft"] ?: 0f,
            emotionMap["mouthLowerDownRight"] ?: 0f,
            emotionMap["mouthPressLeft"] ?: 0f,
            emotionMap["mouthPressRight"] ?: 0f,
            emotionMap["mouthPucker"] ?: 0f,
            emotionMap["mouthRight"] ?: 0f,
            emotionMap["mouthRollLower"] ?: 0f,
            emotionMap["mouthRollUpper"] ?: 0f,
            emotionMap["mouthShrugLower"] ?: 0f,
            emotionMap["mouthShrugUpper"] ?: 0f,
            emotionMap["mouthSmileLeft"] ?: 0f,
            emotionMap["mouthSmileRight"] ?: 0f,
            emotionMap["mouthStretchLeft"] ?: 0f,
            emotionMap["mouthStretchRight"] ?: 0f,
            emotionMap["mouthUpperUpLeft"] ?: 0f,
            emotionMap["mouthUpperUpRight"] ?: 0f,
            emotionMap["noseSneerLeft"] ?: 0f,
            emotionMap["noseSneerRight"] ?: 0f
        )
    }

    /**
     * Calculates distance between two landmarks.
     */
    private fun distance(
        a: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        b: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): Float {
        return sqrt((a.x() - b.x()).pow(2) + (a.y() - b.y()).pow(2))
    }

    /**
     * Broadcasts face data to other services.
     */
    private fun broadcastFaceData(faceData: FaceData) {
        val intent = Intent(Constants.ACTION_FACE_DATA).apply {
            putExtra(Constants.EXTRA_FACE_DATA, faceData)
        }
        sendBroadcast(intent)
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification() = NotificationCompat.Builder(this, Constants.CHANNEL_FACE_TRACKING)
        .setContentTitle("Face Tracking Active")
        .setContentText("468-point mesh tracking enabled")
        .setSmallIcon(R.drawable.ic_face_tracking)
        .setOngoing(true)
        .setSilent(true)
        .build()

    /**
     * Extension function to convert ImageProxy to Bitmap.
     */
    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
        
        return bitmap
    }
}
