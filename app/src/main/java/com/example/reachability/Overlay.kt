package com.example.reachability

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View.inflate
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout


class Overlay : AccessibilityService() {
    var mLayout: FrameLayout? = null

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
        mLayout = FrameLayout(this)
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
//        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
//        lp.gravity = Gravity.TOP
        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.overlay, mLayout)
        wm.addView(mLayout, lp)

    }

    fun configureSwipeListener(){
        val swipe = mLayout.findViewById()
    }


}