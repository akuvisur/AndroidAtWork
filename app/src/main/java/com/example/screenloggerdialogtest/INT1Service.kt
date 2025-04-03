package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat.startForeground
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.screenloggerdialogtest.FirebaseUtils.ScreenEvent
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry

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

class INT1Service : Service() {

    private var INT1receiverRegistered : Boolean = false

    private lateinit var notificationManager : NotificationManager

    var dailyUsageGoal : Long = 0L
    var bedtimeGoal : Int = 0

    lateinit var screenEvent : ScreenEvent
    var previousEventTimestamp : Long = 0L
    lateinit var previousEventType : String

    private lateinit var INT1Receiver : INT1ScreenReceiver

    val channelId1 = "ScreenLoggerServiceChannel_INT1"
    val channelName = "INT1Service"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOGIC", "INT1 starting")

        dailyUsageGoal = getStudyVariable(this, INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0L)
        bedtimeGoal = getStudyVariable(this, BEDTIME_GOAL, BEDTIME_GOAL_DEFAULT_VALUE)

        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "INT1_SERVICE_STARTED"))

        INT1Receiver = INT1ScreenReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (!INT1receiverRegistered) {
            registerReceiver(INT1Receiver, filter)
            INT1receiverRegistered = true
        }

        notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelId1, channelName, NotificationManager.IMPORTANCE_LOW)

        notificationManager.createNotificationChannel(channel)
        val studyState = getStudyState(this)
        val notificationColor = when (studyState) {
            4 -> ContextCompat.getColor(this, R.color.blue_500)  // Blue for study state 4
            6 -> ContextCompat.getColor(this, R.color.deep_purple_500)  // Deep Purple for study state 6
            else -> ContextCompat.getColor(this, R.color.blue_gray_500)  // Default color if studyState is neither 4 nor 6
        }

        notificationManager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, channelId1)
            .setContentTitle("Smartphone Interventions")
            .setContentText("Intervention phase")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Set your app icon here
            .setColorized(true)
            .setColor(notificationColor)
            .build()

        startForeground(1, notification)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    override fun onDestroy() {
        super.onDestroy()
        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "INT1_SERVICE_STOPPED"))
        if (::INT1Receiver.isInitialized) {
            unregisterReceiver(INT1Receiver)
        }
        notificationManager = getSystemService(NotificationManager::class.java)
        if (::notificationManager.isInitialized) notificationManager.cancel(1)
    }

    inner class INT1ScreenReceiver : BroadcastReceiver() {
        private lateinit var dialog : UnlockDialog

        override fun onReceive(p0: Context?, p1: Intent?) {

            val now = System.currentTimeMillis()

            val (ts, type) = getPreviousEvent(p0)
            previousEventTimestamp = ts
            previousEventType = type

            // Additional functionality for Service B
            if (p1?.action == Intent.ACTION_USER_PRESENT) {
                val dailyUsageGoal = getStudyVariable(p0, INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0L)
                val dailyUsage = getDailyUsage(p0, "INT1 onReceive()")
                val dailyUsageGoalDiff = dailyUsageGoal - dailyUsage

                Log.d("INT1", "usage: ${dailyUsage} goal: ${dailyUsageGoal} diff: ${dailyUsageGoalDiff}")
                Log.d("INT1", "bed goal ${bedtimeGoal} minutes until bed: ${calculateMinutesUntilBedtime(bedtimeGoal)}")
                Log.d("INT1", "usage within 45 seconds: ${usageWithin45Seconds(p0, now, previousEventTimestamp)}")
                // Prioritise showing the bedtime dialog
                if (calculateMinutesUntilBedtime(bedtimeGoal) <= 60 && !usageWithin45Seconds(p0, now, previousEventTimestamp)) {
                    dialog = UnlockDialog()
                    dialog.showDialog(p0, now, bedtimeGoal, dailyUsage, dailyUsageGoal, DIALOG_TYPE_BEDTIME, false)
                    return
                }
                if (dailyUsage > dailyUsageGoal && !usageWithin45Seconds(p0, now, previousEventTimestamp)) {
                    dialog = UnlockDialog()
                    dialog.showDialog(p0, now, bedtimeGoal, dailyUsage, dailyUsageGoal, DIALOG_TYPE_GOAL_EXCEEDED, false)
                }
            }
            else if (p1?.action == Intent.ACTION_SCREEN_OFF) {
                if (System.currentTimeMillis() - previousEventTimestamp <= 10000 && ::dialog.isInitialized) {
                    val data = UnlockDialog.AdheredResponse(
                        dialogType = dialog.dialogType,
                        dialogClosedTimestamp = System.currentTimeMillis(),
                        dialogCreatedTimestamp = dialog.dialogCreatedTimestamp,
                        response = DIALOG_RESPONSE_ADHERED
                    )
                    FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/dialog_responses/${System.currentTimeMillis()}", data)
                }
                if (::dialog.isInitialized) dialog.close(p0)
            }
        }
    }

    fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    // within 45 seconds continues previous use, no further disruptions after unlock
    fun usageWithin45Seconds(c : Context?, time : Long, previous : Long) : Boolean {
        return((time - previous) < 45000)
    }
}