package com.cover.app.data.intruder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.location.Location
import android.media.Image
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.cover.app.data.repository.VaultRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntruderCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultRepository: VaultRepository
) {
    companion object {
        private const val TAG = "IntruderCapture"
    }

    private var executor: java.util.concurrent.ExecutorService? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var lifecycleOwner: ApplicationLifecycleOwner? = null

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    sealed class CaptureState {
        object Idle : CaptureState()
        object Capturing : CaptureState()
        data class Success(val photoId: String) : CaptureState()
        data class Error(val message: String) : CaptureState()
    }

    /**
     * Initialize camera for silent intruder capture
     */
    suspend fun initializeCamera(): Boolean = withContext(Dispatchers.Main) {
        try {
            // Create executor if needed
            if (executor == null || executor?.isShutdown == true) {
                executor = Executors.newSingleThreadExecutor()
            }
            
            val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                ProcessCameraProvider.getInstance(context)

            cameraProvider = cameraProviderFuture.await()
            
            // Use front camera
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            // Preview not needed for silent capture, but camera needs a use case
            val preview = Preview.Builder()
                .build()

            // Image capture configuration
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()

            // Unbind all use cases before rebinding
            cameraProvider?.unbindAll()

            // Use application lifecycle for camera binding
            lifecycleOwner = ApplicationLifecycleOwner()

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner!!,
                cameraSelector,
                preview,
                imageCapture
            )

            // Wait for camera to be ready
            delay(300)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Camera initialization failed", e)
            false
        }
    }

    /**
     * Capture intruder photo silently
     */
    suspend fun captureIntruder(
        attemptedPin: String?,
        isDecoyVault: Boolean,
        location: Location?
    ): String? = withContext(Dispatchers.IO) {
        _captureState.value = CaptureState.Capturing

        try {
            val capture = imageCapture ?: run {
                _captureState.value = CaptureState.Error("Camera not initialized")
                return@withContext null
            }

            // Take picture
            val safeExecutor = executor ?: return@withContext null
            val photoFile = capture.takePicture(safeExecutor)
            
            // Save to secure storage via repository
            val photoId = vaultRepository.logIntruderAttempt(
                attemptedPin = attemptedPin,
                isDecoyVault = isDecoyVault,
                photoBytes = photoFile,
                location = location?.let { IntruderLog.Location(it.latitude, it.longitude) }
            )

            _captureState.value = CaptureState.Success(photoId)
            photoId
        } catch (e: Exception) {
            Log.e(TAG, "Capture failed", e)
            _captureState.value = CaptureState.Error(e.message ?: "Unknown error")
            null
        }
    }

    /**
     * Capture multiple photos in sequence (immediate + delayed)
     */
    suspend fun captureIntruderSequence(
        attemptedPin: String?,
        isDecoyVault: Boolean,
        location: Location?
    ): List<String> = withContext(Dispatchers.IO) {
        val photoIds = mutableListOf<String>()
        
        // First capture - immediate
        captureIntruder(attemptedPin, isDecoyVault, location)?.let {
            photoIds.add(it)
        }
        
        // Wait 1 second
        delay(1000)
        
        // Second capture - may catch different expression/angle
        captureIntruder(attemptedPin, isDecoyVault, location)?.let {
            photoIds.add(it)
        }
        
        photoIds
    }

    /**
     * Shutdown camera to free resources
     */
    fun shutdown() {
        // Destroy lifecycle owner first to properly clean up camera
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        
        cameraProvider?.unbindAll()
        camera = null
        imageCapture = null
        cameraProvider = null
        _captureState.value = CaptureState.Idle
        
        // Shutdown executor
        executor?.shutdown()
        executor = null
    }

    /**
     * Convert ImageProxy to JPEG bytes
     */
    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Failed to decode image")
    }

    /**
     * Application lifecycle owner that properly handles camera lifecycle
     */
    private inner class ApplicationLifecycleOwner : androidx.lifecycle.LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        
        init {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        
        fun destroy() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        
        override val lifecycle: androidx.lifecycle.Lifecycle get() = registry
    }

    /**
     * Extension to convert ListenableFuture to suspend function (for Guava's ListenableFuture)
     */
    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
        addListener({
            try {
                cont.resume(get()) {}
            } catch (e: Exception) {
                cont.resumeWith(Result.failure(e))
            }
        }, executor ?: Executors.newSingleThreadExecutor())
    }

    /**
     * Extension to capture image and return bytes
     */
    private suspend fun ImageCapture.takePicture(executor: java.util.concurrent.Executor): ByteArray =
        suspendCancellableCoroutine { cont ->
            val callback = object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = image.use { proxy ->
                        val buffer = proxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        bytes
                    }
                    cont.resume(bytes) {}
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWith(Result.failure(exception))
                }
            }
            
            takePicture(executor, callback)
        }
}

data class IntruderLog(val location: Location?) {
    data class Location(val latitude: Double, val longitude: Double)
}
