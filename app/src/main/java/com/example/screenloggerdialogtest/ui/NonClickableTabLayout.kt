package com.example.screenloggerdialogtest.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.tabs.TabLayout

class NonClickableTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Prevent touch events from reaching the tabs
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // Prevent touch events from reaching the tabs
        return true
    }
}
