package my.rem.client

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.startStreamBtn).setOnClickListener {
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(intent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
