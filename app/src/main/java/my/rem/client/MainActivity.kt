package my.rem.client

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import my.rem.client.FloatingOverlayView

class MainActivity : AppCompatActivity() {

    private val mediaProjectionRequestCode = 1001
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            Toast.makeText(this, "Требуется разрешение Overlay", Toast.LENGTH_LONG).show()
            startActivity(intent)
            return
        }

        requestScreenCapturePermission()
    }

    private fun requestScreenCapturePermission() {
        projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = projectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, mediaProjectionRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mediaProjectionRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            FloatingOverlayView(this).createFloatingView(resultCode, data)
            Toast.makeText(this, "Overlay разрешение получено", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(
                this,
                "Разрешение на захват экрана отклонено",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
