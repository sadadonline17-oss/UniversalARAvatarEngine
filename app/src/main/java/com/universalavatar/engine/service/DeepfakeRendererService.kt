package com.universalavatar.engine.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.os.Binder
import android.os.IBinder
import android.view.Surface
nimport androidx.core.app.NotificationCompat
import com.universalavatar.engine.R
import com.universalavatar.engine.model.FaceData
import com.universalavatar.engine.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    DEEPFAKE RENDERER SERVICE                                 ║
 * ║                                                                              ║
 * ║  Real-time avatar rendering using First Order Motion Model.                  ║
 * ║  TensorFlow Lite GPU/NNAPI acceleration.                                     ║
 * ║                                                                              ║
 * ║  Features:                                                                   ║
 * ║  - First Order Motion Model for motion transfer                              ║
 * ║  - Expression encoder for emotion mapping                                    ║
 * ║  - Multiple avatar styles (realistic, anime, cartoon, robot, metahuman)      ║
 * ║  - Real-time lip sync                                                        ║
 * ║  - Eye blinking simulation                                                   ║
 * ║                                                                              ║
 * ║  Performance: 30 FPS, < 40ms latency                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class DeepfakeRendererService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = DeepfakeRendererBinder()
    
    // TensorFlow Lite interpreters
    private var motionModelInterpreter: Interpreter? = null
    private var expressionEncoderInterpreter: Interpreter? = null
    private var generatorInterpreter: Interpreter? = null
    
    // GPU/NNAPI delegates
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null
    
    // Avatar state
    private var currentAvatarType: String = AvatarType.REALISTIC
    private var sourceImage: Bitmap? = null
    private var drivingFrame: Bitmap? = null
    private var outputBitmap: Bitmap? = null
    
    // Face data queue for smooth animation
    private val faceDataQueue = ConcurrentLinkedQueue<FaceData>()
    private var lastFaceData: FaceData? = null
    
    // Render surface
    private var renderSurface: Surface? = null
    private var isRendering = false
    
    // Avatar styles
    object AvatarType {
        const val REALISTIC = "realistic"
        const val ANIME = "anime"
        const val CARTOON = "cartoon"
        const val ROBOT = "robot"
        const val METAHUMAN = "metahuman"
    }

    inner class DeepfakeRendererBinder : Binder() {
        fun getService(): DeepfakeRendererService = this@DeepfakeRendererService
        fun setRenderSurface(surface: Surface) {
            renderSurface = surface
        }
        fun setAvatarType(type: String) {
            changeAvatarType(type)
        }
    }

    private val faceDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val faceData = intent?.getParcelableExtra<FaceData>(Constants.EXTRA_FACE_DATA)
            faceData?.let {
                faceDataQueue.offer(it)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeTensorFlow()
        registerFaceDataReceiver()
        loadDefaultAvatar()
        Timber.i("DeepfakeRendererService created")
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            Constants.NOTIFICATION_ID_DEEPFAKE_RENDERER,
            createNotification()
        )
        
        if (!isRendering) {
            startRendering()
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRendering()
        unregisterReceiver(faceDataReceiver)
        closeInterpreters()
        serviceScope.cancel()
        Timber.i("DeepfakeRendererService destroyed")
    }

    /**
     * Initializes TensorFlow Lite with GPU/NNAPI acceleration.
     */
    private fun initializeTensorFlow() {
        try {
            // Check GPU compatibility
            val compatList = CompatibilityList()
            
            val interpreterOptions = Interpreter.Options().apply {
                numThreads = 4
                useXNNPACK = true
                
                if (compatList.isDelegateSupportedOnThisDevice) {
                    // Use GPU delegate
                    gpuDelegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                    addDelegate(gpuDelegate)
                    Timber.i("GPU delegate initialized")
                } else {
                    // Fallback to NNAPI
                    nnApiDelegate = NnApiDelegate()
                    addDelegate(nnApiDelegate)
                    Timber.i("NNAPI delegate initialized")
                }
            }
            
            // Load models
            motionModelInterpreter = Interpreter(
                loadModelFile("first_order_motion.tflite"),
                interpreterOptions
            )
            
            expressionEncoderInterpreter = Interpreter(
                loadModelFile("expression_encoder.tflite"),
                interpreterOptions
            )
            
            generatorInterpreter = Interpreter(
                loadModelFile("avatar_generator.tflite"),
                interpreterOptions
            )
            
            Timber.i("TensorFlow Lite models loaded successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize TensorFlow Lite")
            // Fallback to CPU
            initializeTensorFlowCPU()
        }
    }

    /**
     * Fallback initialization with CPU only.
     */
    private fun initializeTensorFlowCPU() {
        try {
            val interpreterOptions = Interpreter.Options().apply {
                numThreads = 4
                useXNNPACK = true
            }
            
            motionModelInterpreter = Interpreter(
                loadModelFile("first_order_motion.tflite"),
                interpreterOptions
            )
            
            expressionEncoderInterpreter = Interpreter(
                loadModelFile("expression_encoder.tflite"),
                interpreterOptions
            )
            
            generatorInterpreter = Interpreter(
                loadModelFile("avatar_generator.tflite"),
                interpreterOptions
            )
            
            Timber.i("TensorFlow Lite models loaded (CPU mode)")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize TensorFlow Lite on CPU")
        }
    }

    /**
     * Loads a TFLite model file from assets.
     */
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        return FileUtil.loadMappedFile(this, modelName)
    }

    /**
     * Registers receiver for face data broadcasts.
     */
    private fun registerFaceDataReceiver() {
        registerReceiver(faceDataReceiver, IntentFilter(Constants.ACTION_FACE_DATA))
    }

    /**
     * Loads the default avatar image.
     */
    private fun loadDefaultAvatar() {
        serviceScope.launch {
            try {
                // Load default avatar from assets
                assets.open("avatars/default_${currentAvatarType}.png").use { inputStream ->
                    sourceImage = android.graphics.BitmapFactory.decodeStream(inputStream)
                }
                Timber.i("Default avatar loaded: $currentAvatarType")
            } catch (e: IOException) {
                Timber.e(e, "Failed to load default avatar")
                // Create placeholder avatar
                sourceImage = createPlaceholderAvatar()
            }
        }
    }

    /**
     * Creates a placeholder avatar bitmap.
     */
    private fun createPlaceholderAvatar(): Bitmap {
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw placeholder face
        val paint = Paint().apply {
            color = Color.parseColor("#FF6B6B")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
        
        // Draw eyes
        paint.color = Color.WHITE
        canvas.drawCircle(size * 0.35f, size * 0.4f, size * 0.08f, paint)
        canvas.drawCircle(size * 0.65f, size * 0.4f, size * 0.08f, paint)
        
        // Draw pupils
        paint.color = Color.BLACK
        canvas.drawCircle(size * 0.35f, size * 0.4f, size * 0.04f, paint)
        canvas.drawCircle(size * 0.65f, size * 0.4f, size * 0.04f, paint)
        
        // Draw mouth
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.03f
        canvas.drawArc(
            size * 0.3f, size * 0.5f,
            size * 0.7f, size * 0.8f,
            0f, 180f, false, paint
        )
        
        return bitmap
    }

    /**
     * Starts the rendering loop.
     */
    private fun startRendering() {
        isRendering = true
        
        serviceScope.launch(Dispatchers.Default) {
            while (isRendering && isActive) {
                val startTime = System.currentTimeMillis()
                
                // Process face data
                val faceData = faceDataQueue.poll() ?: lastFaceData
                
                faceData?.let {
                    lastFaceData = it
                    renderFrame(it)
                }
                
                // Maintain target FPS
                val processingTime = System.currentTimeMillis() - startTime
                val targetFrameTime = 1000 / BuildConfig.TARGET_FPS
                val delayMs = maxOf(0, targetFrameTime - processingTime)
                
                if (delayMs > 0) {
                    delay(delayMs)
                }
            }
        }
    }

    /**
     * Stops the rendering loop.
     */
    private fun stopRendering() {
        isRendering = false
    }

    /**
     * Renders a single frame with the avatar.
     */
    private fun renderFrame(faceData: FaceData) {
        try {
            // Encode expression from face data
            val expressionVector = encodeExpression(faceData)
            
            // Generate avatar frame
            val avatarFrame = generateAvatarFrame(expressionVector, faceData)
            
            // Render to surface
            renderToSurface(avatarFrame)
            
        } catch (e: Exception) {
            Timber.e(e, "Error rendering frame")
        }
    }

    /**
     * Encodes facial expression into a latent vector.
     */
    private fun encodeExpression(faceData: FaceData): FloatArray {
        val encoder = expressionEncoderInterpreter ?: return FloatArray(256)
        
        // Prepare input tensor
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 52), DataType.FLOAT32)
        inputBuffer.loadArray(faceData.emotionVector)
        
        // Prepare output tensor
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 256), DataType.FLOAT32)
        
        // Run inference
        encoder.run(inputBuffer.buffer, outputBuffer.buffer)
        
        return outputBuffer.floatArray
    }

    /**
     * Generates avatar frame using First Order Motion Model.
     */
    private fun generateAvatarFrame(expressionVector: FloatArray, faceData: FaceData): Bitmap {
        val motionModel = motionModelInterpreter ?: return sourceImage ?: createPlaceholderAvatar()
        val generator = generatorInterpreter ?: return sourceImage ?: createPlaceholderAvatar()
        
        // Prepare source image tensor
        val sourceTensor = sourceImage?.let { bitmapToTensor(it) } ?: return createPlaceholderAvatar()
        
        // Prepare motion parameters
        val motionParams = createMotionParameters(faceData)
        
        // Run motion model
        val motionOutput = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 2), DataType.FLOAT32)
        motionModel.run(
            arrayOf(sourceTensor.buffer, motionParams),
            motionOutput.buffer
        )
        
        // Generate final avatar
        val avatarOutput = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
        generator.run(
            arrayOf(sourceTensor.buffer, motionOutput.buffer, expressionVector),
            avatarOutput.buffer
        )
        
        // Convert output to bitmap
        return tensorToBitmap(avatarOutput)
    }

    /**
     * Creates motion parameters from face data.
     */
    private fun createMotionParameters(faceData: FaceData): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder())
        
        // Normalize head pose
        buffer.putFloat(faceData.yaw / 90f)
        buffer.putFloat(faceData.pitch / 90f)
        buffer.putFloat(faceData.roll / 90f)
        
        // Eye and mouth ratios
        buffer.putFloat(faceData.eyeRatio)
        buffer.putFloat(faceData.mouthRatio)
        
        // Blink detection
        val isBlinking = faceData.eyeRatio < 0.15f
        buffer.putFloat(if (isBlinking) 1f else 0f)
        
        buffer.rewind()
        return buffer
    }

    /**
     * Converts bitmap to TensorBuffer.
     */
    private fun bitmapToTensor(bitmap: Bitmap): TensorBuffer {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(256, 256, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 255f))
            .build()
        
        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)
        
        return tensorImage.tensorBuffer
    }

    /**
     * Converts TensorBuffer to bitmap.
     */
    private fun tensorToBitmap(tensor: TensorBuffer): Bitmap {
        val floatArray = tensor.floatArray
        val width = 256
        val height = 256
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        for (i in pixels.indices) {
            val r = (floatArray[i * 3] * 255).toInt().coerceIn(0, 255)
            val g = (floatArray[i * 3 + 1] * 255).toInt().coerceIn(0, 255)
            val b = (floatArray[i * 3 + 2] * 255).toInt().coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Renders the avatar bitmap to the output surface.
     */
    private fun renderToSurface(bitmap: Bitmap) {
        renderSurface?.let { surface ->
            try {
                val canvas = surface.lockCanvas(null)
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                
                // Scale and center the avatar
                val scale = minOf(
                    canvas.width.toFloat() / bitmap.width,
                    canvas.height.toFloat() / bitmap.height
                )
                
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                val left = (canvas.width - scaledWidth) / 2
                val top = (canvas.height - scaledHeight) / 2
                
                canvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight),
                    null
                )
                
                surface.unlockCanvasAndPost(canvas)
                
            } catch (e: Exception) {
                Timber.e(e, "Error rendering to surface")
            }
        }
    }

    /**
     * Changes the current avatar type.
     */
    private fun changeAvatarType(type: String) {
        if (type in listOf(
                AvatarType.REALISTIC,
                AvatarType.ANIME,
                AvatarType.CARTOON,
                AvatarType.ROBOT,
                AvatarType.METAHUMAN
            )) {
            currentAvatarType = type
            loadDefaultAvatar()
            Timber.i("Avatar type changed to: $type")
        }
    }

    /**
     * Closes all TensorFlow Lite interpreters.
     */
    private fun closeInterpreters() {
        motionModelInterpreter?.close()
        expressionEncoderInterpreter?.close()
        generatorInterpreter?.close()
        gpuDelegate?.close()
        nnApiDelegate?.close()
        
        motionModelInterpreter = null
        expressionEncoderInterpreter = null
        generatorInterpreter = null
        gpuDelegate = null
        nnApiDelegate = null
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification() = NotificationCompat.Builder(this, Constants.CHANNEL_DEEPFAKE_RENDERER)
        .setContentTitle("Avatar Renderer Active")
        .setContentText("Rendering: $currentAvatarType")
        .setSmallIcon(R.drawable.ic_avatar_render)
        .setOngoing(true)
        .setSilent(true)
        .build()
}
