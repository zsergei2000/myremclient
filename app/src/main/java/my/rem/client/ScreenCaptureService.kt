package my.rem.client

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader

    private var socket: Socket? = null
    private var output: DataOutputStream? = null
    private val handler = Handler(Looper.getMainLooper())

    private val isConnecting = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val isCapturing = AtomicBoolean(false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_CAPTURE") {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        startForeground(1, createNotification())

        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)

        startSocketConnection()
        startCapture()

        return START_STICKY
    }

    private fun startSocketConnection() {
        if (isConnected.get() || isConnecting.get()) return
        isConnecting.set(true)

        Thread {
            while (!isConnected.get()) {
                try {
                    socket?.close()
                    output?.close()
                    socket = Socket("192.168.1.145", 12346)
                    output = DataOutputStream(socket!!.getOutputStream())
                    isConnected.set(true)
                    isConnecting.set(false)
                    Log.d("Socket", "Подключение к серверу установлено")
                } catch (e: Exception) {
                    Log.e("Socket", "Ошибка подключения: ${e.message}")
                    Thread.sleep(1000)
                }
            }
        }.start()
    }

    private fun startCapture() {
        if (isCapturing.getAndSet(true)) return

        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            Thread {
                try {
                    synchronized(this) {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                        val byteArray = stream.toByteArray()
                        output?.writeInt(byteArray.size)
                        output?.write(byteArray)
                        output?.flush()
                    }
                } catch (e: Exception) {
                    isConnected.set(false)
                    Log.e("Send", "Ошибка отправки: ${e.message}")
                    startSocketConnection()
                }
            }.start()
        }, Handler(Looper.getMainLooper()))
    }

    private fun createNotification(): Notification {
        val channelId = "screen_capture_channel"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Передача экрана активна")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socket?.close()
            output?.close()
        } catch (_: Exception) {}
        isCapturing.set(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
