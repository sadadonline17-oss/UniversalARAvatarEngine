package com.universalavatar.engine.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         FACE DATA MODEL                                      ║
 * ║                                                                              ║
 * ║  Represents real-time face tracking data from MediaPipe Face Mesh.           ║
 * ║  Contains 468 facial landmarks plus derived features.                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Parcelize
data class FaceData(
    val timestamp: Long,
    val landmarks: List<FloatArray>, // 468 landmarks, each with [x, y, z]
    val yaw: Float,                  // Head rotation around Y axis (-90 to 90)
    val pitch: Float,                // Head rotation around X axis (-90 to 90)
    val roll: Float,                 // Head rotation around Z axis (-90 to 90)
    val eyeRatio: Float,             // Eye openness ratio (0.0 to 1.0)
    val mouthRatio: Float,           // Mouth openness ratio (0.0 to 1.0)
    val emotionVector: FloatArray,   // 52-dimension blendshape vector
    val processingTimeMs: Long       // Processing latency
) : Parcelable {
    
    companion object {
        // Landmark indices for key facial features
        const val NOSE_TIP = 4
        const val CHIN = 152
        const val LEFT_EYE_LEFT_CORNER = 33
        const val LEFT_EYE_RIGHT_CORNER = 133
        const val RIGHT_EYE_LEFT_CORNER = 362
        const val RIGHT_EYE_RIGHT_CORNER = 263
        const val LEFT_EYE_TOP = 159
        const val LEFT_EYE_BOTTOM = 145
        const val RIGHT_EYE_TOP = 386
        const val RIGHT_EYE_BOTTOM = 374
        const val MOUTH_LEFT = 61
        const val MOUTH_RIGHT = 291
        const val MOUTH_TOP = 13
        const val MOUTH_BOTTOM = 17
        const val LEFT_EYEBROW_INNER = 105
        const val LEFT_EYEBROW_OUTER = 334
        const val RIGHT_EYEBROW_INNER = 105
        const val RIGHT_EYEBROW_OUTER = 334
        
        // Emotion detection thresholds
        const val EYE_CLOSED_THRESHOLD = 0.15f
        const val MOUTH_OPEN_THRESHOLD = 0.3f
        const val SMILE_THRESHOLD = 0.5f
    }
    
    /**
     * Checks if eyes are closed.
     */
    fun areEyesClosed(): Boolean = eyeRatio < EYE_CLOSED_THRESHOLD
    
    /**
     * Checks if mouth is open.
     */
    fun isMouthOpen(): Boolean = mouthRatio > MOUTH_OPEN_THRESHOLD
    
    /**
     * Detects if person is smiling based on mouth shape.
     */
    fun isSmiling(): Boolean {
        // Use blendshape scores for smile detection
        val smileLeft = emotionVector.getOrNull(43) ?: 0f // mouthSmileLeft
        val smileRight = emotionVector.getOrNull(44) ?: 0f // mouthSmileRight
        return (smileLeft + smileRight) / 2 > SMILE_THRESHOLD
    }
    
    /**
     * Gets eye gaze direction.
     */
    fun getEyeGaze(): EyeGaze {
        val lookLeft = emotionVector.getOrNull(14) ?: 0f // eyeLookInLeft
        val lookRight = emotionVector.getOrNull(15) ?: 0f // eyeLookInRight
        val lookUp = emotionVector.getOrNull(16) ?: 0f // eyeLookUpLeft
        val lookDown = emotionVector.getOrNull(10) ?: 0f // eyeLookDownLeft
        
        return EyeGaze(
            x = lookRight - lookLeft,
            y = lookDown - lookUp
        )
    }
    
    /**
     * Gets facial expression as enum.
     */
    fun getExpression(): FacialExpression {
        return when {
            areEyesClosed() && isMouthOpen() -> FacialExpression.SURPRISED
            isSmiling() -> FacialExpression.HAPPY
            emotionVector.getOrNull(30) ?: 0f > 0.5f -> FacialExpression.ANGRY // browDown
            emotionVector.getOrNull(2) ?: 0f > 0.5f -> FacialExpression.SAD // browInnerUp
            isMouthOpen() -> FacialExpression.SURPRISED
            else -> FacialExpression.NEUTRAL
        }
    }
    
    /**
     * Gets head position relative to camera.
     */
    fun getHeadPosition(): HeadPosition {
        return HeadPosition(
            x = landmarks.getOrNull(NOSE_TIP)?.getOrNull(0) ?: 0.5f,
            y = landmarks.getOrNull(NOSE_TIP)?.getOrNull(1) ?: 0.5f,
            z = landmarks.getOrNull(NOSE_TIP)?.getOrNull(2) ?: 0f
        )
    }
    
    /**
     * Converts face data to motion parameters for avatar animation.
     */
    fun toMotionParameters(): MotionParameters {
        return MotionParameters(
            headYaw = yaw / 90f,      // Normalize to -1 to 1
            headPitch = pitch / 90f,
            headRoll = roll / 90f,
            eyeOpenness = eyeRatio,
            mouthOpenness = mouthRatio,
            expressionWeights = emotionVector
        )
    }
}

/**
 * Eye gaze direction.
 */
data class EyeGaze(
    val x: Float, // -1 (left) to 1 (right)
    val y: Float  // -1 (up) to 1 (down)
)

/**
 * Head position in 3D space.
 */
data class HeadPosition(
    val x: Float, // 0 to 1 (normalized)
    val y: Float, // 0 to 1 (normalized)
    val z: Float  // Depth estimate
)

/**
 * Facial expression types.
 */
enum class FacialExpression {
    NEUTRAL,
    HAPPY,
    SAD,
    ANGRY,
    SURPRISED,
    DISGUSTED,
    FEARFUL
}

/**
 * Motion parameters for avatar animation.
 */
data class MotionParameters(
    val headYaw: Float,
    val headPitch: Float,
    val headRoll: Float,
    val eyeOpenness: Float,
    val mouthOpenness: Float,
    val expressionWeights: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as MotionParameters
        
        if (headYaw != other.headYaw) return false
        if (headPitch != other.headPitch) return false
        if (headRoll != other.headRoll) return false
        if (eyeOpenness != other.eyeOpenness) return false
        if (mouthOpenness != other.mouthOpenness) return false
        if (!expressionWeights.contentEquals(other.expressionWeights)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = headYaw.hashCode()
        result = 31 * result + headPitch.hashCode()
        result = 31 * result + headRoll.hashCode()
        result = 31 * result + eyeOpenness.hashCode()
        result = 31 * result + mouthOpenness.hashCode()
        result = 31 * result + expressionWeights.contentHashCode()
        return result
    }
}
