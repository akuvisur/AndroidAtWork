package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.content.ContextCompat


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

    lateinit var notificationManager : NotificationManager
    private lateinit var notificationChannel : NotificationChannel

    private val usageNotificationId = 876
    private val bedtimeNotificationId = 654

    private lateinit var INT2Receiver : ScreenStateReceiver
    var dailyUsage : Long = 0L
    var lastUsageTimestamp : Long = 0L
    var dailyUsageGoal : Long = 0L
    var dailyUsageGoalDiff : Long = 0L
    var bedtimeGoal : Int = 0


    private val channelIdInt2 = "ScreenLoggerServiceChannelInt2"
    private val channelNameInt2 = "Int2ChannelName"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOGIC", "INT2 starting")

        dailyUsageGoal = getStudyVariable(this, INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0L)
        dailyUsage = getDailyUsage(this)

        bedtimeGoal = getStudyVariable(this, BEDTIME_GOAL, BEDTIME_GOAL_DEFAULT_VALUE)
        lastUsageTimestamp = getLastDailyUsageTime(this)

        INT2Receiver = INT2ScreenReceiver()
        val filter = IntentFilter().apply {
            //addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(INT2Receiver, filter)

        notificationManager = getSystemService(NotificationManager::class.java)
        notificationChannel = NotificationChannel(channelIdInt2, channelNameInt2, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableVibration(true)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        notificationChannel.setSound(soundUri, audioAttributes) // Set default notification sound
        notificationChannel.enableVibration(true)
        notificationChannel.setVibrationPattern(longArrayOf(0, 200))
        notificationManager.createNotificationChannel(notificationChannel)

        studyPhase = 2

        return super.onStartCommand(intent, flags, startId)
    }

    open inner class INT2ScreenReceiver : ScreenStateReceiver() {
        var now : Long = 0L
        override fun onReceive(p0: Context?, p1: Intent?) {
            // Additional functionality for Service B
            now = System.currentTimeMillis()

            // these plus daily usage are reduntant when in INT1 but necesary when in INT2
            val (ts, type) = getPreviousEvent(p0)
            previousEventTimestamp = ts
            previousEventType = type

            dailyUsageGoal = getStudyVariable(p0, INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0L)
            dailyUsage = getDailyUsage(p0)

            if (p1?.action == Intent.ACTION_SCREEN_OFF && (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY)) {
                if (previousEventType == "ACTION_USER_PRESENT") {
                    if (getCurrentDay() == getDayFromMillis(previousEventTimestamp)) {
                        var duration = (now - previousEventTimestamp)
                        if (duration > 3_600_000) duration = 180_000
                        dailyUsage += duration
                    }
                    else {
                        dailyUsage = 0
                    }

                    lastUsageTimestamp = now
                    storeDailyUsage(dailyUsage, p0)
                    storeLastDailyUsageTime(now, p0)
                    dailyUsageGoalDiff = dailyUsageGoal - dailyUsage
                    Log.d("DAILY_USAGE", "dailyusage: ${formatTime(dailyUsage)} diff: ${formatTime(dailyUsageGoalDiff)} goal: ${formatTime(dailyUsageGoal)}")
                }
            }
            else if (p1?.action == Intent.ACTION_USER_PRESENT && (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY)) {
                // reset daily usage if last entry from previous day
                if (getCurrentDay() != getDayFromMillis(previousEventTimestamp)) {
                    dailyUsage = 0
                }
                // show notification if above usage goal
                if (calculateMinutesUntilBedtime(bedtimeGoal) <= 60 && !usageWithin45Seconds(p0, now)) {
                    if (p0 != null) {
                        val largeIcon = BitmapFactory.decodeResource(p0.resources, R.drawable.drowsy)

                        val notificationBuilder = Notification.Builder(p0, channelId)
                            .setContentTitle("Smartphone Interventions")
                            .setContentText("Bedtime near, put your phone away!")
                            .setSmallIcon(R.drawable.bedtime2) // Set your app icon here
                            .setLargeIcon(largeIcon)
                            .setColorized(true)
                            .setColor(ContextCompat.getColor(p0, R.color.deep_purple_700))

                        val notification: Notification = notificationBuilder.build()

                        notificationManager.notify(bedtimeNotificationId, notification)
                    }
                }
                else if (dailyUsage > dailyUsageGoal && !usageWithin45Seconds(p0, now)) {
                    dailyUsageGoalDiff = dailyUsageGoal - dailyUsage
                    if (p0 != null) {
                        val largeIcon = BitmapFactory.decodeResource(p0.resources, R.drawable.alert)
                        val notificationBuilder = Notification.Builder(p0, channelId)
                            .setContentTitle("Smartphone Interventions")
                            .setContentText(
                                "Exceeded daily goal ${
                                    formatTime(
                                        dailyUsageGoal
                                    )
                                } by ${formatTime(-dailyUsageGoalDiff)}."
                            )
                            .setSmallIcon(R.drawable.goal_reached) // Set your app icon here
                            .setLargeIcon(largeIcon)
                            .setColorized(true)
                            .setColor(ContextCompat.getColor(p0, R.color.yellow))

                        val notification = notificationBuilder.build()
                        notificationManager.notify(usageNotificationId, notification)
                    }
                }
                // within an hour of bedtime goal
                // show a notification
                // if no "alerts" then just show usage notification
                else {
                    if (p0 != null) {
                        val notificationBuilder = Notification.Builder(p0, channelId)
                            .setContentTitle("Smartphone Interventions")
                            .setContentText("Today you have used your phone for ${formatTime(dailyUsage)}")
                            .setSmallIcon(R.drawable.goal_reached) // Set your app icon here
                            .setColorized(true)
                            .setColor(ContextCompat.getColor(p0, R.color.teal_300))

                        val notification = notificationBuilder.build()
                        notificationManager.notify(usageNotificationId, notification)
                    }
                    if (getCurrentDay() == getDayFromMillis(now)) {
                        dailyUsage += (now - previousEventTimestamp)
                    }
                    else {
                        dailyUsage = (now - previousEventTimestamp)
                    }
                    lastUsageTimestamp = now
                    //storeDailyUsage(dailyUsage, p0)
                    //storeLastDailyUsageTime(now, p0)
                }

            }

            // this has to occur last because it updates timestamps etc.
            super.onReceive(p0, p1) // Retain functionality from Service A
        }
    }

    fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    // within 45 seconds continues previous use, no further disruptions after unlock
    fun usageWithin45Seconds(c : Context?, time : Long) : Boolean {
        lastUsageTimestamp = getDailyUsage(c)
        return((time - lastUsageTimestamp) < 45000)
    }
}