package com.example.screenloggerdialogtest

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry

class ApplicationTrackingService : AccessibilityService() {

    private var notificationVisible = false
    private var notificationHasBeenShownOnce = false
    private var notificationTimer: CountDownTimer? = null
    private val notificationId = 123456
    private val channelId = "accessibility_channel"

    private var resolvedNotificationColor: Int = Color.BLUE

    private var lastApplicationEvent : Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        resolvedNotificationColor = ContextCompat.getColor(this, R.color.blue_500)
        showNotification()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && (now - lastApplicationEvent) > MULTIPLE_SCREEN_EVENT_DELAY) {
            val packageName = event.packageName?.toString()
            packageName?.let {
                //Toast.makeText(this, "Foreground app: $it", Toast.LENGTH_SHORT).show()
                //Log.d("application_tracking", packageName)
                val entryPath = "/users/${getCurrentUserUID()}/foreground_apps/${System.currentTimeMillis()}"
                val data = FirebaseUtils.FirebaseForegroundApplicationObject(packageName = it)

                // Upload the entry to Firebase
                uploadFirebaseEntry(entryPath, data)
            }

            if (!notificationVisible) {
                showNotification()
            }
        }
        lastApplicationEvent = System.currentTimeMillis()
    }


    override fun onInterrupt() {
        // Required override
    }

    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Tracker",
                NotificationManager.IMPORTANCE_HIGH // No sound or vibration
            ).apply {
                setShowBadge(false)
                description = "Tracks foreground apps silently"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "accessibility_channel")
            .setContentTitle("AndroidAtWork")
            .setContentText("Tracking foreground apps")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Make sure this exists
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setColorized(true)
            .setColor(resolvedNotificationColor)
            .setSilent(notificationHasBeenShownOnce)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
        notificationVisible = true
        notificationHasBeenShownOnce = true

        // Reset the timer
        notificationTimer?.cancel()
        notificationTimer = object : CountDownTimer(10 * 60 * 1000, 10 * 60 * 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                notificationManager.cancel(notificationId)
                notificationVisible = false
            }
        }.start()
    }

}
