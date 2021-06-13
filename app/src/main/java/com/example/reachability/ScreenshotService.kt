package com.example.reachability

import android.R.attr.data
import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.media.ToneGenerator
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.text.format.DateFormat
import android.util.Log
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class ScreenshotService : Service() {
    private var projection: MediaProjection? = null
    private var vdisplay: VirtualDisplay? = null
    private val handlerThread = HandlerThread(
        javaClass.simpleName,
        Process.THREAD_PRIORITY_BACKGROUND
    )
    private var handler: Handler? = null
    private var mgr: MediaProjectionManager? = null
    private var wmgr: WindowManager? = null
    private var it: ImageTransmogrifier? = null
    private var resultCode = 0
    private var resultData: Intent? = null
    private val beeper = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate() {
        super.onCreate()
        mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        wmgr = getSystemService(WINDOW_SERVICE) as WindowManager
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(i: Intent, flags: Int, startId: Int): Int {
        if (i.action == null) {
            resultCode = i.getIntExtra(EXTRA_RESULT_CODE, 1337)
            resultData = i.getParcelableExtra(EXTRA_RESULT_INTENT)
            foregroundify()
        } else if (ACTION_RECORD == i.action) {
            if (resultData != null) {
                startCapture()
            } else {
                val ui = Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(ui)
            }
        } else if (ACTION_SHUTDOWN == i.action) {
            beeper.startTone(ToneGenerator.TONE_PROP_NACK)
            stopForeground(true)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        throw IllegalStateException("Binding not supported. Go away.")
    }

    val windowManager: WindowManager?
        get() = wmgr

    fun getHandler(): Handler? {
        return handler
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun processImage(png: ByteArray?) {
        screenshot(png!!, "result")
        object : Thread() {
            override fun run() {
                val output = File(
                    getExternalFilesDir(null),
                    "screenshot.png"
                )
                try {
                    val fos = FileOutputStream(output)
                    fos.write(png)
                    fos.flush()
                    fos.fd.sync()
                    fos.close()
                    MediaScannerConnection.scanFile(
                        this@ScreenshotService, arrayOf(output.absolutePath), arrayOf("image/png"),
                        null
                    )
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "Exception writing out screenshot", e)
                }
            }
        }.start()
        beeper.startTone(ToneGenerator.TONE_PROP_ACK)
        stopCapture()


    }

    protected fun screenshot(png: ByteArray, filename: String): File? {
        val date = Date()

        // Here we are initialising the format of our image name
        val format: CharSequence = DateFormat.format("yyyy-MM-dd_hh:mm:ss", date)
        try {
            // Initialising the directory of storage
            val dirpath = Environment.getExternalStorageDirectory().toString() + ""
            val file = File(dirpath)
            if (!file.exists()) {
                val mkdir = file.mkdir()
            }

            // File name
            val path = "$dirpath/$filename-$format.jpeg"
//            view.setDrawingCacheEnabled(true)
//            val bitmap: Bitmap = Bitmap.createBitmap(view.getDrawingCache())
//            view.setDrawingCacheEnabled(false)

            val options = BitmapFactory.Options()
            options.inMutable = true
            val bitmap = BitmapFactory.decodeByteArray(png, 0, png.size, options)

            val imageurl = File(path)
            val outputStream = FileOutputStream(imageurl)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            outputStream.flush()
            outputStream.close()
            return imageurl
        } catch (io: FileNotFoundException) {
            io.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stopCapture() {
        if (projection != null) {
            projection!!.stop()
            vdisplay!!.release()
            projection = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startCapture() {
        projection = mgr!!.getMediaProjection(resultCode, resultData!!)
        it = ImageTransmogrifier(this)
        val cb: MediaProjection.Callback = object : MediaProjection.Callback() {
            override fun onStop() {
                vdisplay!!.release()
            }
        }
        vdisplay = projection!!.createVirtualDisplay(
            "andshooter",
            it!!.getWidth(), it!!.getHeight(),
            resources.displayMetrics.densityDpi,
            VIRT_DISPLAY_FLAGS, it!!.surface, null, handler
        )
        projection!!.registerCallback(cb, handler)
    }

    private fun foregroundify() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            mgr.getNotificationChannel(CHANNEL_WHATEVER) == null
        ) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WHATEVER,
                    "Whatever", NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val b = NotificationCompat.Builder(this, CHANNEL_WHATEVER)
        b.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
        b.setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(getString(R.string.app_name))
        b.addAction(
            R.drawable.ic_record_white_24dp,
            getString(R.string.notify_record),
            buildPendingIntent(ACTION_RECORD)
        )
        b.addAction(
            R.drawable.ic_eject_white_24dp,
            getString(R.string.notify_shutdown),
            buildPendingIntent(ACTION_SHUTDOWN)
        )
        startForeground(NOTIFY_ID, b.build())
    }

    private fun buildPendingIntent(action: String): PendingIntent {
        val i = Intent(this, javaClass)
        i.action = action
        return PendingIntent.getService(this, 0, i, 0)
    }

    companion object {
        private const val CHANNEL_WHATEVER = "channel_whatever"
        private const val NOTIFY_ID = 9906
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_INTENT = "resultIntent"
        const val ACTION_RECORD = BuildConfig.APPLICATION_ID + ".RECORD"
        const val ACTION_SHUTDOWN = BuildConfig.APPLICATION_ID + ".SHUTDOWN"
        const val VIRT_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }
}