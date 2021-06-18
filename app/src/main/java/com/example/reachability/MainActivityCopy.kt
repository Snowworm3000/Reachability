package com.example.reachability

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivityCopy : AppCompatActivity() {
    private val CAST_PERMISSION_CODE = 22
    private val mDisplayMetrics: DisplayMetrics = DisplayMetrics();
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mProjectionManager: MediaProjectionManager? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMediaRecorder = MediaRecorder()
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        windowManager.defaultDisplay.getMetrics(mDisplayMetrics)
        prepareRecording()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun startRecording() {
        // If mMediaProjection is null that means we didn't get a context, lets ask the user
        if (mMediaProjection == null) {
            // This asks for user permissions to capture the screen
            startActivityForResult(mProjectionManager?.createScreenCaptureIntent(), CAST_PERMISSION_CODE)
            return
        }
        mVirtualDisplay = getVirtualDisplay()
        mMediaRecorder!!.start()
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun stopRecording() {
        if (mMediaRecorder != null) {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay!!.release()
        }
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
        }
        prepareRecording()
    }

    fun getCurSysDate(): String {
        return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date())
    }

    private fun prepareRecording() {
        try {
            mMediaRecorder!!.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        val directory: String = Environment.getExternalStorageDirectory().toString() + File.separator.toString() + "Recordings"
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show()
            return
        }
        val folder = File(directory)
        var success = true
        if (!folder.exists()) {
            success = folder.mkdir()
        }
        val filePath: String
        filePath = if (success) {
            val videoName = "capture_" + getCurSysDate() + ".mp4"
            directory + File.separator.toString() + videoName
        } else {
            Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show()
            return
        }
        val width = mDisplayMetrics!!.widthPixels
        val height = mDisplayMetrics.heightPixels
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mMediaRecorder!!.setVideoEncodingBitRate(512 * 1000)
        mMediaRecorder!!.setVideoFrameRate(30)
        mMediaRecorder!!.setVideoSize(width, height)
        mMediaRecorder!!.setOutputFile(filePath)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CAST_PERMISSION_CODE) {
            // Where did we get this request from ? -_-
            Log.w(TAG, "Unknown request code: $requestCode")
            return
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show()
            return
        }
        mMediaProjection = mProjectionManager?.getMediaProjection(resultCode, data!!)
        // TODO Register a callback that will listen onStop and release & prepare the recorder for next recording
        // mMediaProjection.registerCallback(callback, null);
        mVirtualDisplay = getVirtualDisplay()
        mMediaRecorder!!.start()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getVirtualDisplay(): VirtualDisplay? {
        val screenDensity = mDisplayMetrics!!.densityDpi
        val width = mDisplayMetrics.widthPixels
        val height = mDisplayMetrics.heightPixels
        return mMediaProjection!!.createVirtualDisplay(this.javaClass.simpleName,
            width, height, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface, null /*Callbacks*/, null /*Handler*/)
    }
}