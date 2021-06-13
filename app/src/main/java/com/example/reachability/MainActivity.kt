package com.example.reachability

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private val REQUEST_SCREENSHOT = 59706
    private var mgr: MediaProjectionManager? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.overlay)
        mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mgr!!.createScreenCaptureIntent(),
            REQUEST_SCREENSHOT
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENSHOT) {
            if (resultCode == RESULT_OK) {
                val i = Intent(this, ScreenshotService::class.java)
                    .putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode)
                    .putExtra(ScreenshotService.EXTRA_RESULT_INTENT, data)
                startService(i)
            }
        }
        finish()
    }
}