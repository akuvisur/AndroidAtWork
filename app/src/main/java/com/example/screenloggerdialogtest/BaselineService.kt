package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class BaselineService : Service() {

    private lateinit var screenStateReceiver: ScreenStateReceiver

    private lateinit var windowManager: WindowManager
    private lateinit var dialogView: View

    companion object {
        const val REQUEST_CODE = 100 // Arbitrary value for overlay permission
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ScreenLoggerService", "Service is running...")

        // ServiceCompat.startForeground()
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

        // Do your screen logging logic here

        screenStateReceiver = ScreenStateReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
        Log.d("Service", "receiver registered")

        return START_STICKY // Or other flags depending on your use case
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

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        if (::dialogView.isInitialized) {
            windowManager.removeView(dialogView)
        }
    }

    inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    // Screen turned on
                    Log.d("ScreenStateReceiver", "Screen ON")

                    showNotification()

                    //val dialogIntent = Intent(p0, DialogService::class.java)
                    //dialogIntent.putExtra("message", "Screen Turned On")
                    //p0?.startService(dialogIntent)

                }

                Intent.ACTION_SCREEN_OFF -> {
                    // Screen turned off
                    Log.d("ScreenStateReceiver", "Screen OFF")
                }
                Intent.ACTION_USER_PRESENT -> {
                    // User unlocked the screen
                    Log.d("ScreenStateReceiver", "User Present (Unlocked)")
                    showDialog(p0)
                }
            }
        }

    }

}