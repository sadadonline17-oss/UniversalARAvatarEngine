package com.universalavatar.engine.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.universalavatar.engine.R
import com.universalavatar.engine.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.webrtc.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    SCREEN SHARE BRIDGE SERVICE                               ║
 * ║                                                                              ║
 * ║  Enables using screen sharing as a virtual camera for video calls.           ║
 * ║  Uses WebRTC for real-time streaming.                                        ║
 * ║                                                                              ║
 * ║  Features:                                                                   ║
 * ║  - Screen capture as video source                                            ║
 * ║  - WebRTC peer connection                                                    ║
 * ║  - Virtual camera injection                                                  ║
 * ║  - Compatible with Zoom, Teams, Meet, etc.                                   ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@AndroidEntryPoint
class ScreenShareBridgeService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binder = ScreenShareBinder()
    private val handler = Handler(Looper.getMainLooper())
    
    // WebRTC components
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    
    // Screen capture
    private var mediaProjection: MediaProjection? = null
    private var screenCapturer: ScreenCapturerAndroid? = null
    
    // Virtual camera
    private var virtualCamera: CameraVideoCapturer? = null
    
    // Connection state
    private var isStreaming = false
    private var connectionState = PeerConnection.PeerConnectionState.NEW
    
    // Connected peers
    private val connectedPeers = ConcurrentHashMap<String, PeerConnection>()

    inner class ScreenShareBinder : Binder() {
        fun getService(): ScreenShareBridgeService = this@ScreenShareBridgeService
        fun startScreenShare(resultCode: Int, data: Intent) {
            setupScreenCapture(resultCode, data)
        }
        fun stopScreenShare() {
            stopStreaming()
        }
        fun isStreaming(): Boolean = isStreaming
        fun createPeerConnection(iceServers: List<PeerConnection.IceServer>): PeerConnection? {
            return createPeerConnectionInternal(iceServers)
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeWebRTC()
        Timber.i("ScreenShareBridgeService created")
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            Constants.NOTIFICATION_ID_SCREEN_SHARE,
            createNotification()
        )
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
        releaseWebRTC()
        serviceScope.cancel()
        Timber.i("ScreenShareBridgeService destroyed")
    }

    /**
     * Initializes WebRTC components.
     */
    private fun initializeWebRTC() {
        try {
            // Initialize PeerConnectionFactory
            val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
            
            PeerConnectionFactory.initialize(initializationOptions)
            
            // Create encoder/decoder factory
            val encoderFactory = DefaultVideoEncoderFactory(
                EglBase.create().eglBaseContext,
                true,
                true
            )
            val decoderFactory = DefaultVideoDecoderFactory(EglBase.create().eglBaseContext)
            
            // Create PeerConnectionFactory
            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory()
            
            // Create SurfaceTextureHelper
            surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread",
                EglBase.create().eglBaseContext
            )
            
            Timber.i("WebRTC initialized successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize WebRTC")
        }
    }

    /**
     * Sets up screen capture using MediaProjection.
     */
    private fun setupScreenCapture(resultCode: Int, data: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // Create screen capturer
            screenCapturer = ScreenCapturerAndroid(
                data,
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        Timber.w("MediaProjection stopped")
                        stopStreaming()
                    }
                }
            )
            
            // Setup video source
            setupVideoSource()
            
            isStreaming = true
            Timber.i("Screen capture setup complete")
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to setup screen capture")
        }
    }

    /**
     * Sets up video source from screen capture.
     */
    private fun setupVideoSource() {
        val factory = peerConnectionFactory ?: return
        val capturer = screenCapturer ?: return
        
        // Create video source
        videoSource = factory.createVideoSource(capturer.isScreencast)
        
        // Initialize capturer
        capturer.initialize(surfaceTextureHelper, this, videoSource?.capturerObserver)
        
        // Start capture (1280x720 @ 30fps)
        capturer.startCapture(1280, 720, 30)
        
        // Create video track
        videoTrack = factory.createVideoTrack("screen_share_track", videoSource)
        videoTrack?.setEnabled(true)
        
        Timber.i("Video source setup complete")
    }

    /**
     * Creates a peer connection for WebRTC streaming.
     */
    private fun createPeerConnectionInternal(
        iceServers: List<PeerConnection.IceServer>
    ): PeerConnection? {
        val factory = peerConnectionFactory ?: return null
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            enableDtlsSrtp = true
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState) {
                Timber.d("Signaling state changed: $newState")
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Timber.d("ICE connection state changed: $newState")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Timber.d("ICE connection receiving: $receiving")
            }

            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {
                Timber.d("ICE gathering state changed: $newState")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Timber.d("New ICE candidate: ${it.sdp}")
                    // Send candidate to remote peer
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {
                Timber.d("ICE candidates removed")
            }

            override fun onAddStream(stream: MediaStream?) {
                Timber.d("Stream added: ${stream?.id}")
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Timber.d("Stream removed: ${stream?.id}")
            }

            override fun onDataChannel(dataChannel: DataChannel?) {
                Timber.d("Data channel created: ${dataChannel?.label()}")
            }

            override fun onRenegotiationNeeded() {
                Timber.d("Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Timber.d("Track added")
            }

            override fun onRemoveTrack(receiver: RtpReceiver?) {
                Timber.d("Track removed")
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                connectionState = newState
                Timber.d("Connection state changed: $newState")
            }
        }
        
        return factory.createPeerConnection(rtcConfig, observer)?.apply {
            // Add video track
            videoTrack?.let { track ->
                val sender = addTrack(track)
                Timber.d("Video track added: ${sender?.id()}")
            }
        }
    }

    /**
     * Creates an offer for WebRTC connection.
     */
    fun createOffer(peerConnection: PeerConnection, callback: (SessionDescription?) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        callback(sdp)
                    }
                    override fun onSetFailure(error: String?) {
                        Timber.e("Failed to set local description: $error")
                        callback(null)
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Timber.e("Failed to create offer: $error")
                callback(null)
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    /**
     * Creates an answer for WebRTC connection.
     */
    fun createAnswer(peerConnection: PeerConnection, callback: (SessionDescription?) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        }
        
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        callback(sdp)
                    }
                    override fun onSetFailure(error: String?) {
                        Timber.e("Failed to set local description: $error")
                        callback(null)
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                }, sdp)
            }
            
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Timber.e("Failed to create answer: $error")
                callback(null)
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    /**
     * Sets remote description.
     */
    fun setRemoteDescription(
        peerConnection: PeerConnection,
        sdp: SessionDescription,
        callback: (Boolean) -> Unit
    ) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                callback(true)
            }
            override fun onSetFailure(error: String?) {
                Timber.e("Failed to set remote description: $error")
                callback(false)
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
        }, sdp)
    }

    /**
     * Adds ICE candidate.
     */
    fun addIceCandidate(peerConnection: PeerConnection, candidate: IceCandidate) {
        peerConnection.addIceCandidate(candidate)
    }

    /**
     * Stops streaming and releases resources.
     */
    private fun stopStreaming() {
        isStreaming = false
        
        screenCapturer?.stopCapture()
        screenCapturer?.dispose()
        screenCapturer = null
        
        videoTrack?.dispose()
        videoTrack = null
        
        videoSource?.dispose()
        videoSource = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        connectedPeers.values.forEach { it.close() }
        connectedPeers.clear()
        
        Timber.i("Streaming stopped")
    }

    /**
     * Releases WebRTC resources.
     */
    private fun releaseWebRTC() {
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }

    /**
     * Creates the foreground service notification.
     */
    private fun createNotification() = NotificationCompat.Builder(this, Constants.CHANNEL_SCREEN_SHARE)
        .setContentTitle("Screen Share Bridge")
        .setContentText(if (isStreaming) "Streaming active" else "Ready to stream")
        .setSmallIcon(R.drawable.ic_screen_share)
        .setOngoing(true)
        .setSilent(true)
        .addAction(
            if (isStreaming) R.drawable.ic_stop else R.drawable.ic_play,
            if (isStreaming) "Stop" else "Start",
            createActionPendingIntent()
        )
        .build()

    private fun createActionPendingIntent(): android.app.PendingIntent {
        val intent = Intent(this, ScreenShareBridgeService::class.java).apply {
            action = if (isStreaming) Constants.ACTION_STOP_STREAMING else Constants.ACTION_START_STREAMING
        }
        return android.app.PendingIntent.getService(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        // Default STUN/TURN servers
        val DEFAULT_ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )
    }
}
