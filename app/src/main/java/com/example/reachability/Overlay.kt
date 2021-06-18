package com.example.reachability

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Environment
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class Overlay : AccessibilityService() {
    var mListener: FrameLayout? = null
    var mOverlay: FrameLayout? = null
    var overlayDisplaying = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        TODO("Reminder: Remind the user to disable battery optimisation for this app to prevent the accessibility service being disabled")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Create an overlay and display it
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mListener = FrameLayout(this)
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.swipe_listener, mListener)
        wm.addView(mListener, lp)
        configureSwipeListener()
    }

    fun configureSwipeListener(){
        val swipe = mListener?.findViewById<View>(R.id.swipeView)
        swipe?.setOnTouchListener(object : OnSwipeTouchListener() {
//            override fun onSwipeTop(): Boolean {
//                Toast.makeText(this@Overlay, "top", Toast.LENGTH_SHORT).show()
//                return true
//            }
//
//            override fun onSwipeRight(): Boolean {
//                Toast.makeText(this@Overlay, "right", Toast.LENGTH_SHORT).show()
//                Log.d(null, "right")
//
//                return true
//            }
//
//            override fun onSwipeLeft(): Boolean {
//                Toast.makeText(this@Overlay, "left", Toast.LENGTH_SHORT).show()
//                Log.d(null, "left")
//                return true
//            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onSwipeBottom(): Boolean {
                Toast.makeText(this@Overlay, "bottom", Toast.LENGTH_SHORT).show()
                Log.d(null, "down")
                if(overlayDisplaying) {
                    Log.d(null, "close")
                    TODO("add closeOverlay back but using some other method than the button")
//                    closeOverlay()
                }else{
                    Log.d(null, "open")
                    screenshot()
                    openOverlay()
                }
                return true
            }
        })

    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun screenshot(){
//        Log.d(null, windows.toString())
        var id = -1
        for(nodeInfo in windows){
            Log.d(null, "$nodeInfo ${nodeInfo.type} ${nodeInfo.id} ${nodeInfo.layer}")
            if(nodeInfo.type == 1 && nodeInfo.layer == 0){
                Log.d(null, "application $nodeInfo")
                id = nodeInfo.displayId
            }
        }
        if(id == -1){
            Log.d(null, "no application found 😥")
        }
        // TODO: This requires android 11. Use takeScreenshot when version >= 11, and use screen recording to take a screenshot when using higher android versions.
        val executor = mainExecutor
        val callback = object : TakeScreenshotCallback{
            override fun onSuccess(screenshot: ScreenshotResult) {
                var buffer = screenshot.hardwareBuffer
                Log.d(null, "${buffer.format} ${buffer.height} ${buffer.width}")
            }

            override fun onFailure(errorCode: Int) {
                Log.d(null, "failure $errorCode")
            }

        }
        Log.d(null, "takeScreenshot now")
        takeScreenshot(id, executor, callback)
    }

    fun screenshot(png: ByteArray, filename: String): File? {
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

    fun openOverlay(){
        if(!overlayDisplaying) {
            overlayDisplaying = true

            val oWm = getSystemService(WINDOW_SERVICE) as WindowManager
            mOverlay = FrameLayout(this)
            val lp = WindowManager.LayoutParams()
            lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            lp.format = PixelFormat.TRANSLUCENT
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
//        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            val inflater = LayoutInflater.from(this)
            inflater.inflate(R.layout.overlay, mOverlay)
            oWm.addView(mOverlay, lp)
            Log.d(null, "added")
        }else{
            Log.d(null, "already displaying overlay")
        }
    }

    fun closeOverlay(v:View){
        var wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.removeView(mOverlay)
        overlayDisplaying = false
    }


}