package com.example.screenloggerdialogtest

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ApplicationTrackingService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events
    }

    override fun onInterrupt() {
        // Required override
    }
}
