package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry
import java.util.Random


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

open class INT2Service : Service() {

    private val usageNotificationId = 876
    private val bedtimeNotificationId = 654

    private lateinit var INT2Receiver : INT2ScreenReceiver
    var dailyUsageGoalDiff : Long = 0L
    var bedtimeGoal : Int = 0

    private lateinit var notificationManager : NotificationManager
    private lateinit var notificationChannel: NotificationChannel

    var previousEventTimestamp : Long = 0L
    lateinit var previousEventType : String

    private val channelIdInt2 = "ScreenLoggerServiceChannelInt2"
    private val channelNameInt2 = "Int2ChannelName"
    private val channelIdINT2ImportantNotifications = "channelIdINT2ImportantNotifications"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOGIC", "INT2 starting")

        // Start as a foreground service (if necessary)
        notificationManager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(channelIdInt2, channelNameInt2, NotificationManager.IMPORTANCE_LOW)

        // Determine the notification color based on study state
        val studyState = getStudyState(this)
        val notificationColor = when (studyState) {
            4 -> ContextCompat.getColor(this, R.color.blue_500)  // Blue for study state 4
            6 -> ContextCompat.getColor(this, R.color.deep_purple_500)  // Deep Purple for study state 6
            else -> ContextCompat.getColor(this, R.color.blue_gray_500)  // Default color if studyState is neither 4 nor 6
        }

        notificationManager.createNotificationChannel(channel)
        val notification: Notification = Notification.Builder(this, channelIdInt2)
            .setContentTitle("Smartphone Interventions")
            .setContentText("Intervention phase")
            .setSmallIcon(android.R.drawable.ic_popup_sync) // Set your app icon here
            .setColorized(true)
            .setColor(notificationColor)
            .build()

        startForeground(1, notification)

        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "INT2_SERVICE_STARTED"))

        bedtimeGoal = getStudyVariable(this, BEDTIME_GOAL, BEDTIME_GOAL_DEFAULT_VALUE)

        // no need to start service here if we are in INT1 phase (studyPhase == 1)
        // we start the receiver already in INT1Service.kt

        INT2Receiver = INT2ScreenReceiver()
        val filter = IntentFilter().apply {
            //addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(INT2Receiver, filter)

        notificationChannel = NotificationChannel(channelIdINT2ImportantNotifications, channelNameInt2, NotificationManager.IMPORTANCE_HIGH)
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

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "INT2_SERVICE_STOPPED"))
        if (::INT2Receiver.isInitialized) {
            unregisterReceiver(INT2Receiver)
        }
        notificationManager = getSystemService(NotificationManager::class.java)
        if (::notificationManager.isInitialized) notificationManager.cancel(1)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    open inner class INT2ScreenReceiver : BroadcastReceiver() {
        var now : Long = 0L
        private var timer: CountDownTimer? = null
        private var timerCount = 0

        override fun onReceive(p0: Context?, p1: Intent?) {
            // Additional functionality for Service B
            now = System.currentTimeMillis()
            bedtimeGoal = getStudyVariable(p0, BEDTIME_GOAL, BEDTIME_GOAL_DEFAULT_VALUE)

            if (previousEventTimestamp < 1) {
                val (ts, type) = getPreviousEvent(p0)
                previousEventTimestamp = ts
                previousEventType = type
            }

            // clean old notifications when turning screen off
            if (p1?.action == Intent.ACTION_SCREEN_OFF) {
                if (::notificationManager.isInitialized) {
                    notificationManager.cancel(bedtimeNotificationId)
                    notificationManager.cancel(usageNotificationId)
                }
                cancelUsageTimer()
            }

            if (p1?.action == Intent.ACTION_USER_PRESENT) {
                if (p0 != null) startUsageTimer(p0)
            }

            if (p1?.action == Intent.ACTION_USER_PRESENT && (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) && !usageWithin45Seconds(p0, now, previousEventTimestamp)) {
                // show notification if above usage goal
                val dailyUsageGoal = getStudyVariable(p0, INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0L)
                val dailyUsage = getDailyUsage(p0, "INT2 onReceive()")

                if (calculateMinutesUntilBedtime(bedtimeGoal) <= 60 && !usageWithin45Seconds(p0, now, previousEventTimestamp)) {
                    if (p0 != null) {
                        val largeIcon = BitmapFactory.decodeResource(p0.resources, R.drawable.drowsy)

                        val notificationBuilder = Notification.Builder(p0, channelIdINT2ImportantNotifications)
                            .setContentTitle("Smartphone Interventions")
                            .setContentText("Bedtime near, put your phone away!")
                            .setSmallIcon(R.drawable.bedtime2) // Set your app icon here
                            .setLargeIcon(largeIcon)
                            .setColorized(true)
                            .setColor(ContextCompat.getColor(p0, R.color.deep_purple_700))

                        val notification: Notification = notificationBuilder.build()

                        notificationManager.notify(bedtimeNotificationId, notification)
                        return
                    }
                }
                if (dailyUsage > dailyUsageGoal && !usageWithin45Seconds(p0, now, previousEventTimestamp)) {
                    dailyUsageGoalDiff = dailyUsageGoal - dailyUsage
                    if (p0 != null) {
                        val largeIcon = BitmapFactory.decodeResource(p0.resources, R.drawable.alert)
                        val notificationBuilder = Notification.Builder(p0, channelIdINT2ImportantNotifications)
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
                        if (!::notificationManager.isInitialized) notificationManager = getSystemService(NotificationManager::class.java)
                        notificationManager.notify(usageNotificationId, notification)
                    }
                }
                // within an hour of bedtime goal
                // show a notification
                // if no "alerts" then just show usage notification
                else if (p0 != null) {
                    dailyUsageGoalDiff = dailyUsageGoal - dailyUsage
                    val notificationBuilder = Notification.Builder(p0, channelIdINT2ImportantNotifications)
                        .setContentTitle("Smartphone Interventions")
                        .setContentText("Screen time today ${formatTime(dailyUsage)} ${if (dailyUsageGoalDiff > 0) "(${formatTime(dailyUsageGoalDiff)} left)" else "(Exceeded daily usage)"}")
                        .setSmallIcon(R.drawable.goal_reached) // Set your app icon here
                        .setColorized(true)
                        .setColor(ContextCompat.getColor(p0, R.color.blue_500))

                    val notification = notificationBuilder.build()
                    if (!::notificationManager.isInitialized) notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(usageNotificationId, notification)
                }
            }
        }

        private fun startUsageTimer(c : Context) {
            timer?.cancel() // Cancel any existing timer
            timer = object : CountDownTimer(CONTINUOUS_USAGE_NOTIFICATION_TIMELIMIT*6, CONTINUOUS_USAGE_NOTIFICATION_TIMELIMIT) {
                override fun onTick(millisUntilFinished: Long) {
                    if (timerCount > 0) sendUsageNotification(c)
                    timerCount++
                }
                override fun onFinish() {
                }
            }.start()
        }

        private fun cancelUsageTimer() {
            timer?.cancel()
            timer = null
            timerCount = 0
        }

        private fun sendUsageNotification(c : Context) {
            val actualMinutes = timerCount * 10
            val randomMessage = CONTINUOUS_USAGE_TEXT[Random().nextInt(CONTINUOUS_USAGE_TEXT.size)]
            val formattedMessage = String.format(randomMessage, actualMinutes)

            val notificationBuilder = Notification.Builder(c, channelIdINT2ImportantNotifications)
                .setContentTitle("Smartphone Interventions")
                .setContentText(formattedMessage)
                .setStyle(Notification.BigTextStyle().bigText(formattedMessage))
                .setSmallIcon(R.drawable.goal_reached) // Set your app icon here
            val notification = notificationBuilder.build()
            if (!::notificationManager.isInitialized) notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(usageNotificationId, notification)
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