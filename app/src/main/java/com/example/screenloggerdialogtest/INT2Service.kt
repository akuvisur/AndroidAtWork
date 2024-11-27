package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ### Service Hierarchy Documentation
 *
 * This hierarchy explains how the services in the study build upon each other to address the
 * requirements of each phase. Each service inherits functionality from the previous phase while
 * introducing new components tailored to its specific needs.
 */

/**
 * **BaseTrackingService**
 * - **Purpose**: Provides the core functionality for tracking basic smartphone usage events.
 * - **Components**:
 *   - Tracks `SCREEN_ON`, `SCREEN_OFF`, and `USER_PRESENT` events.
 *   - Logs basic screen usage data.
 *   - Forms the foundation for additional functionality in later phases.
 */

/**
 * **INT1Service**
 * - **Purpose**: Extends `BaseTrackingService` to include goal-oriented usage interventions.
 * - **Components**:
 *   - Inherits all functionality from  both `BaseTrackingService` and `INT2Service`.
 *   - Adds **blocking dialogs** to notify the user when usage limits are exceeded.
 *   - Encourages conscious use of the smartphone through real-time interventions.
 */

/**
 * **INT2Service**
 * - **Purpose**: Extends `INT1Service` by adding notifications to promote habit formation.
 * - **Components**:
 *   - Inherits all functionality from `BaseTrackingService.
 *   - Introduces **notifications** about smartphone usage to help participants track their progress.
 *   - Provides reminders and feedback to reinforce positive behavioral changes.
 */

/**
 * **Hierarchy Summary**
 * - `BaseTrackingService`: Tracks core events (SCREEN_ON/OFF/USER_PRESENT).
 * - `INT1Service`: Adds **usage blocking dialogs** for goal-oriented interventions.
 * - `INT2Service`: Adds **usage notifications** to promote habit formation.
 *
 * The hierarchy ensures each service logically builds on its predecessor, minimizing redundancy while
 * enabling tailored interventions for each study phase.
 */

open class INT2Service : BaselineService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotificationChannel() // Create notification channel

        // Start as a foreground service (if necessary)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "ScreenLoggerServiceChannel"
            val channelName = "Screen Logger Service"

            val notificationManager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

            notificationManager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(this, channelId)
                .setContentTitle("Screen Logger")
                .setContentText("The service is running...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Set your app icon here
                .build()

            startForeground(1, notification)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Screen State Notifications"
            val channelDescription = "Notifications for screen on events"
            val channelId = "screen_on_channel"

            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = channelDescription
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)

        val notificationId = 25
        val channelId = "screen_on_channel"

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your notification icon
            .setContentTitle("Screen Turned On")
            .setContentText("The screen is now on $currentTime." )
            .setPriority(NotificationCompat.PRIORITY_MAX)

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.areNotificationsEnabled()) {
            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, notificationBuilder.build())
            }
        }

    }

    open inner class INT2ScreenReceiver : ScreenStateReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            super.onReceive(p0, p1) // Retain functionality from Service A

            // Additional functionality for Service B
            if (p1?.action == Intent.ACTION_SCREEN_ON) {
                // Do something additional when the screen is turned on
                Log.d("AugmentedScreenStateReceiver", "Additional functionality in Service INT2")
                // e.g., start a new service or send a broadcast, etc.
            }
        }
    }
}