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
import android.os.CountDownTimer
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
    private var receiverRegistered : Boolean = false

    // set to 0.5 after debug
    private val esm_propability = 1.0
    // set to 360000 (6 minutes) after debug
    private val MINIMUM_DIALOG_DELAY = 1

    private lateinit var notificationManager : NotificationManager

    val channelId = "ScreenLoggerServiceChannel"
    val channelName = "BaselineService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOGIC", "Baseline starting")
        // Start as a foreground service (if necessary)
        notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)

        notificationManager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("AndroidAtWork")
            .setContentText("Monitoring smartphone use")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Set your app icon here
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.teal_500))
            .build()

        startForeground(1, notification)

        screenStateReceiver = ScreenStateReceiver()
        val filter = IntentFilter().apply {
            //addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (!receiverRegistered) {
            registerReceiver(screenStateReceiver, filter)
            receiverRegistered = true
        }

        return START_STICKY // Or other flags depending on your use case
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::screenStateReceiver.isInitialized) {
            unregisterReceiver(screenStateReceiver)
        }

        notificationManager = getSystemService(NotificationManager::class.java)
        if (::notificationManager.isInitialized) notificationManager.cancel(1)
    }

    open inner class ScreenStateReceiver : BroadcastReceiver() {

        private lateinit var dialog : UnlockDialog
        private var lastDialogTimestamp : Long = 0L

        lateinit var screenEvent : ScreenEvent
        var previousEventTimestamp : Long = 0L
        lateinit var previousEventType : String
        private var lastScreenEvent : Long = 0L
        private var lastScreenoff : Long = 0L

        private var sessionStartTimestamp : Long = 0L
        private var sessionDuration : Long = 0L

        override fun onReceive(p0: Context?, p1: Intent?) {
            if (::dialog.isInitialized) dialog.close(p0)
            lastScreenEvent = System.currentTimeMillis()

            val (ts, type) = getPreviousEvent(p0)
            previousEventTimestamp = ts
            previousEventType = type

            // have to skip incase we dont have a timestamp, otherwise WILL
            // cause weird bugs with duration
            // the getPreviousEvent() defaults to System.currentTimeMillis() to this should not occur
            if (previousEventTimestamp == 0L) return

            val now = System.currentTimeMillis()
            if (now - previousEventTimestamp < MULTIPLE_SCREEN_EVENT_DELAY) return

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
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen turned off
                    Log.d("ScreenStateReceiver", "Screen OFF")
                    uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/screen_events/${System.currentTimeMillis()}",
                        FirebaseUtils.FirebaseDataLoggingObject(event = "ACTION_SCREEN_OFF"))

                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    // updating to start a NEW event with type ACTION_SCREEN_OFF
                    // this event is stored when the NEXT even triggers and is then stored as even of THIS type.
                    updatePreviousEvent(now, "ACTION_SCREEN_OFF", p0)

                    sessionDuration = now-sessionStartTimestamp
                    lastScreenoff = now

                    if (::dialog.isInitialized) dialog.close(p0)
                }

                Intent.ACTION_USER_PRESENT -> {
                    // User unlocked the screen
                    Log.d("ScreenStateReceiver", "User Present (Unlocked)")
                    uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/screen_events/${System.currentTimeMillis()}",
                        FirebaseUtils.FirebaseDataLoggingObject(event = "ACTION_USER_PRESENT"))

                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    // updating to start a NEW event with type ACTION_USER_PRESENT
                    // this event is stored when the NEXT even triggers and is then stored as even of THIS type.
                    updatePreviousEvent(now, "ACTION_USER_PRESENT", p0)

                    // Reset session start only if more than 45 seconds have passed since last usage
                    if (!usageWithin45Seconds(p0, now, lastScreenoff)) {
                        sessionStartTimestamp = now
                        sessionDuration = 0L
                        // TODO new WORKING timer required
                        //startEsmTimer(p0, sessionDuration)
                    }
                    //else startEsmTimer(p0, sessionDuration)

                    if (Math.random() < esm_propability && (now - lastDialogTimestamp > MINIMUM_DIALOG_DELAY)) {
                        dialog = UnlockDialog()
                        dialog.showDialog(p0, DIALOG_TYPE_USAGE_INITIATED)
                        Log.d("dialog", "Calling dialog to be shown")
                        lastDialogTimestamp = now
                    }

                }

            }

        }


        private fun sendUsageEsm(c: Context, continued: Boolean) {
            Log.d("esm", "sendUsageEsm $continued")
            dialog = UnlockDialog()
            val dialogType = if (continued) DIALOG_TYPE_USAGE_CONTINUED else DIALOG_TYPE_USAGE_INITIATED
            dialog.showDialog(c, dialogType)
            lastDialogTimestamp = System.currentTimeMillis()
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
            previousEventTimestamp = time
            putPreviousEvent(time, type, c)
        }
    }

    // within 45 seconds continues previous use, no further disruptions after unlock
    fun usageWithin45Seconds(c : Context?, time : Long, previous : Long) : Boolean {
        return((time - previous) < 45000)
    }

}