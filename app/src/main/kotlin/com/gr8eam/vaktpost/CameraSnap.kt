package com.gr8eam.vaktpost

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tar ETT stillbildskort med framkameran utan förhandsvisning (Camera2 med en ImageReader-yta).
 * Allt sker på en egen tråd och allt stängs när bilden är klar eller tiden gått ut. Resultatet (JPEG-bytes
 * eller null vid fel) lämnas via onDone, alltid exakt en gång.
 *
 * Observera: en del telefoner/tillverkare tillåter inte att kameran öppnas medan skärmen är låst. Då
 * misslyckas försöket tyst (onDone(null)). Detta är en begränsning i Android/OEM, inte i appen.
 */
class CameraSnap(private val context: Context) {

    private val done = AtomicBoolean(false)
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var onDone: ((ByteArray?) -> Unit)? = null

    fun capture(onDone: (ByteArray?) -> Unit) {
        this.onDone = onDone
        val t = HandlerThread("vaktpost-cam").also { it.start() }
        thread = t
        handler = Handler(t.looper)
        handler?.post { start() }
        // Säkerhetsventil: ge upp efter 5 sekunder om kameran hänger sig.
        handler?.postDelayed({ finish(null) }, 5000)
    }

    private fun start() {
        try {
            val mgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val camId = pickFront(mgr) ?: run { finish(null); return }
            val chars = mgr.getCameraCharacteristics(camId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val size = pickSize(map?.getOutputSizes(ImageFormat.JPEG))
            val sensor = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

            reader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1).apply {
                setOnImageAvailableListener({ r ->
                    val img = try { r.acquireLatestImage() } catch (e: Exception) { null }
                    if (img == null) { finish(null); return@setOnImageAvailableListener }
                    val buf = img.planes[0].buffer
                    val bytes = ByteArray(buf.remaining())
                    buf.get(bytes)
                    img.close()
                    finish(bytes)
                }, handler)
            }

            @Suppress("MissingPermission")
            mgr.openCamera(camId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    camera = device
                    makeSession(device, sensor)
                }
                override fun onDisconnected(device: CameraDevice) { finish(null) }
                override fun onError(device: CameraDevice, error: Int) { finish(null) }
            }, handler)
        } catch (e: Throwable) {
            finish(null)
        }
    }

    private fun makeSession(device: CameraDevice, sensorOrientation: Int) {
        try {
            val surface = reader!!.surface
            @Suppress("DEPRECATION")
            device.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    session = s
                    try {
                        val req = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(surface)
                            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
                        }
                        s.capture(req.build(), null, handler)
                    } catch (e: Exception) {
                        finish(null)
                    }
                }
                override fun onConfigureFailed(s: CameraCaptureSession) { finish(null) }
            }, handler)
        } catch (e: Exception) {
            finish(null)
        }
    }

    private fun pickFront(mgr: CameraManager): String? {
        var firstAny: String? = null
        for (id in mgr.cameraIdList) {
            if (firstAny == null) firstAny = id
            val facing = mgr.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) return id
        }
        return firstAny
    }

    private fun pickSize(sizes: Array<Size>?): Size {
        if (sizes == null || sizes.isEmpty()) return Size(1280, 960)
        // Minsta storlek som är minst 1280 bred, annars den största tillgängliga. Håller filen liten men skarp nog.
        val sorted = sizes.sortedBy { it.width.toLong() * it.height }
        return sorted.firstOrNull { it.width >= 1280 } ?: sorted.last()
    }

    private fun finish(bytes: ByteArray?) {
        if (!done.compareAndSet(false, true)) return
        val cb = onDone
        onDone = null
        try { session?.close() } catch (e: Exception) {}
        try { camera?.close() } catch (e: Exception) {}
        try { reader?.close() } catch (e: Exception) {}
        session = null; camera = null; reader = null
        try { cb?.invoke(bytes) } catch (e: Exception) {}
        thread?.quitSafely()
        thread = null; handler = null
    }
}
