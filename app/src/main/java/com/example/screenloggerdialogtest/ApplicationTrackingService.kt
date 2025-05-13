package com.example.screenloggerdialogtest

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry

class ApplicationTrackingService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            packageName?.let {
                Toast.makeText(this, "Foreground app: $it", Toast.LENGTH_SHORT).show()

                val entryPath = "/users/${getCurrentUserUID()}/foreground_apps/${System.currentTimeMillis()}"
                val data = FirebaseUtils.FirebaseForegroundApplicationObject(packageName = it)

                // Upload the entry to Firebase
                uploadFirebaseEntry(entryPath, data)
            }
        }
    }


    override fun onInterrupt() {
        // Required override
    }

}
