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
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.OutputStream
import java.net.Socket

class ScreenCaptureService : Service() {

    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var imageReader: ImageReader

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        startForeground(1, createNotification())

        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        startCapture()
        return START_STICKY
    }

    private fun startCapture() {
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
                    val socket = Socket("192.168.1.145", 12346) // Заменить на IP своего ПК
                    val output: OutputStream = socket.getOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, output)
                    output.flush()
                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        }, null)
    }

    private fun createNotification(): Notification {
        val channelId = "screen_capture_channel"
        val channelName = "Screen Capture"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Передача экрана")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
