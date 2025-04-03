package my.rem.client

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast

class FloatingOverlayView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatView: View? = null

    fun createFloatingView(resultCode: Int, data: Intent) {
        if (floatView != null) return

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatView = inflater.inflate(R.layout.overlay_floating_view, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 300

        floatView?.findViewById<Button>(R.id.btnStart)?.setOnClickListener {
            try {
                val intent = Intent(context, ScreenCaptureService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                context.startForegroundService(intent)
                Toast.makeText(context, "Захват экрана начат", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("FloatingOverlay", "Ошибка запуска сервиса: ${e.message}")
            }
        }

        floatView?.findViewById<Button>(R.id.btnStop)?.setOnClickListener {
            val stopIntent = Intent(context, ScreenCaptureService::class.java).apply {
                action = "STOP_CAPTURE"
            }
            context.startService(stopIntent)
            Toast.makeText(context, "Захват экрана остановлен", Toast.LENGTH_SHORT).show()
        }

        floatView?.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
            try {
                val stopIntent = Intent(context, ScreenCaptureService::class.java).apply {
                    action = "STOP_CAPTURE"
                }
                context.startService(stopIntent)
                Toast.makeText(context, "Приложение закрыто", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("FloatingOverlay", "Ошибка остановки сервиса: ${e.message}")
            }

            removeFloatingView()
        }


        windowManager.addView(floatView, layoutParams)
    }

    fun removeFloatingView() {
        floatView?.let {
            windowManager.removeView(it)
            floatView = null
        }
    }
}
