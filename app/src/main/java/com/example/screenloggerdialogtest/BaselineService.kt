package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.example.screenloggerdialogtest.FirebaseUtils.ScreenEvent
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry
import java.util.concurrent.TimeUnit

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

const val MULTIPLE_SCREEN_EVENT_DELAY : Int = 1000

open class BaselineService : Service() {
    private lateinit var screenStateReceiver: ScreenStateReceiver
    private lateinit var heartbeat: HeartbeatBeater

    // 0 = baseline, 1=int1, 2=int2
    var studyPhase = 0;

    val channelId = "ScreenLoggerServiceChannel"
    val channelName = "BaselineService"

    override fun onCreate() {
        super.onCreate()

        // Start as a foreground service (if necessary)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

        notificationManager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Smartphone Interventions")
            .setContentText("Monitoring smartphone usage...")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Set your app icon here
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.deep_purple_300))
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOGIC", "Baseline starting")
        // Start as a foreground service (if necessary)
        val notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

        notificationManager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Smartphone Interventions")
            .setContentText("Monitoring smartphone usage...")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Set your app icon here
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.deep_purple_300))
            .build()

        startForeground(1, notification)

        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "BASELINE_SERVICE_STARTED"))

        // Run this only in baseline mode == 0
        if (studyPhase == 0) {
            screenStateReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                //addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenStateReceiver, filter)
        }

        heartbeat = HeartbeatBeater(this)
        heartbeat.startHeartbeat()

        return START_STICKY // Or other flags depending on your use case
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "BASELINE_SERVICE_STOPPED"))

        if (::screenStateReceiver.isInitialized) {
            unregisterReceiver(screenStateReceiver)
        }
        if (::heartbeat.isInitialized) heartbeat.stopHeartbeat()
    }

    open inner class ScreenStateReceiver : BroadcastReceiver() {
        lateinit var screenEvent : ScreenEvent
        var previousEventTimestamp : Long = 0L
        lateinit var previousEventType : String

        override fun onReceive(p0: Context?, p1: Intent?) {
            val (ts, type) = getPreviousEvent(p0)
            previousEventTimestamp = ts
            previousEventType = type

            // have to skip incase we dont have a timestamp, otherwise WILL
            // cause weird bugs with duration
            // the getPreviousEvent() defaults to System.currentTimeMillis() to this should not occur
            if (previousEventTimestamp == 0L) return

            val now = System.currentTimeMillis()

            /*
            // something weird happening that causes double triggers with 1-10ms delay
            // presumably the firebase process takes longer than few milliseconds
            // which causes the delay in updating previousEventTimestamp
            // if its updated in the updatePreviousEvent() function
            // so done manually for now.
             29.01.2025 updated the value from 100 milliseconds to 1000 due to odd bugs?
                - perhaps connected to multiple services running and each service having its own previousEventTimestamp
                - this should be fixed by only using the variables from sharedPreferences with getPreviousEvent()
            */
            when (p1?.action) {
                // if the phone is unlocked with thumb id the order is sometimes Off -> Present -> On
                // so lets just ignore all screen on events, since you cant go from present -> on
                /*
                Intent.ACTION_SCREEN_ON -> {
                    // Screen turned on
                    Log.d("ScreenStateReceiver", "Screen ON")
                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    previousEventTimestamp = now
                    updatePreviousEvent(now, "ACTION_SCREEN_ON", p0)
                }
                */

                Intent.ACTION_SCREEN_OFF -> {
                    // Screen turned off
                    Log.d("ScreenStateReceiver", "Screen OFF")
                    uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/screen_events/${System.currentTimeMillis()}",
                        FirebaseUtils.FirebaseDataLoggingObject(event = "ACTION_SCREEN_OFF"))

                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    previousEventTimestamp = now
                    updatePreviousEvent(now, "ACTION_SCREEN_OFF", p0)
                }

                Intent.ACTION_USER_PRESENT -> {
                    // User unlocked the screen
                    Log.d("ScreenStateReceiver", "User Present (Unlocked)")
                    uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/screen_events/${System.currentTimeMillis()}",
                        FirebaseUtils.FirebaseDataLoggingObject(event = "ACTION_USER_PRESENT"))

                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    previousEventTimestamp = now
                    updatePreviousEvent(now, "ACTION_USER_PRESENT", p0)
                }

            }
        }

        private fun uploadEvent(eventType : String, time : Long, c : Context?) {
            screenEvent = ScreenEvent(eventType, time, System.currentTimeMillis()-time)

            FirebaseUtils.sendEntryToDatabase(
                path = "users/${FirebaseUtils.getCurrentUserUID()}/screen/${time}", // Path in the database (e.g., "users/user_1")
                data = screenEvent,
                onSuccess = {
                },
                onFailure = { exception ->
                }
            )
        }

        private fun updatePreviousEvent(time : Long, type : String, c : Context?) {
            previousEventType = type
            putPreviousEvent(time, type, c)
        }
    }

    class HeartbeatBeater (context: Context) {

        private var c : Context
        private val HB_NOTIFICATION_ID : String = "HB_NOTIFICATION_ID"
        private val HB_NOTIFICATION_CHANNEL_NAME : String = "HB_NOTIFICATION_CHANNEL_NAME"
        private val HB_NOTIFICATION_NUMBER = 123123

        private var notificationManager: NotificationManager
        private var notificationChannel : NotificationChannel

        private val handler = Handler()

        init {
            c = context
            notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationChannel = NotificationChannel(HB_NOTIFICATION_ID, HB_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Runnable to create a Heartbeat every hour
        private val heartbeatRunnable = object : Runnable {
            override fun run() {
                storeBeat()
                // Schedule the next heartbeat in one hour
                handler.postDelayed(this, TimeUnit.HOURS.toMillis(1))
            }
        }

        // Function to start the heartbeat
        fun startHeartbeat() {
            handler.post(heartbeatRunnable)
        }

        // Function to stop the heartbeat (if necessary)
        fun stopHeartbeat() {
            handler.removeCallbacks(heartbeatRunnable)
        }

        fun storeBeat() {

            val studyDay = calculateStudyPeriodDay(BASELINE_START_TIMESTAMP, c)

            val notification: Notification = Notification.Builder(c, HB_NOTIFICATION_ID)
                .setContentTitle("Android Interventions Updates")
                .setContentText("Its currently day ${studyDay} of the study.")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setColorized(true)
                .setColor(ContextCompat.getColor(c, R.color.deep_purple_200))
                .build()
            notificationManager.notify(HB_NOTIFICATION_NUMBER, notification)

            val heartbeat = Heartbeat(timestamp = System.currentTimeMillis())
            FirebaseUtils.sendEntryToDatabase(
                path = "/users/${getCurrentUserUID()}/heartbeat/${heartbeat.timestamp}",
                data = heartbeat,
                onSuccess = {
                },
                onFailure = { exception ->
                }
            )
        }
    }

    private data class Heartbeat(
        val timestamp: Long = 0L
    )

}