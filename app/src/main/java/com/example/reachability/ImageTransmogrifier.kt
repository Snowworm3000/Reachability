package com.example.reachability

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream


@SuppressLint("WrongConstant")
@RequiresApi(Build.VERSION_CODES.KITKAT)
class ImageTransmogrifier internal constructor(private val svc: ScreenshotService) :
    OnImageAvailableListener {
    private val width: Int
    private val height: Int
    private val imageReader: ImageReader
    private var latestBitmap: Bitmap? = null
    override fun onImageAvailable(reader: ImageReader) {
        val image = imageReader.acquireLatestImage()
        if (image != null) {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmapWidth = width + rowPadding / pixelStride
            if (latestBitmap == null || latestBitmap!!.width != bitmapWidth || latestBitmap!!.height != height) {
                if (latestBitmap != null) {
                    latestBitmap!!.recycle()
                }
                latestBitmap = Bitmap.createBitmap(
                    bitmapWidth,
                    height, Bitmap.Config.ARGB_8888
                )
            }
            latestBitmap!!.copyPixelsFromBuffer(buffer)
            image.close()
            val baos = ByteArrayOutputStream()
            val cropped = Bitmap.createBitmap(
                latestBitmap!!, 0, 0,
                width, height
            )
            cropped.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val newPng = baos.toByteArray()
            svc.processImage(newPng)
        }
    }

    val surface: Surface
        get() = imageReader.surface

    fun getWidth(): Int {
        return width
    }

    fun getHeight(): Int {
        return height
    }

    fun close() {
        imageReader.close()
    }

    init {
        val display = svc.windowManager!!.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        var width = size.x
        var height = size.y
        while (width * height > 2 shl 19) {
            width = width shr 1
            height = height shr 1
        }
        this.width = width
        this.height = height
        imageReader = ImageReader.newInstance(
            width, height,
            PixelFormat.RGBA_8888, 2
        )
        imageReader.setOnImageAvailableListener(this, svc.getHandler())
    }
}