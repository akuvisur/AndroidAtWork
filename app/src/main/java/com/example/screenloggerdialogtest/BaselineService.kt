package com.example.screenloggerdialogtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.screenloggerdialogtest.FirebaseUtils.ScreenEvent
import com.example.screenloggerdialogtest.FirebaseUtils.getCurrentUserUID
import com.example.screenloggerdialogtest.FirebaseUtils.uploadFirebaseEntry

const val MULTIPLE_SCREEN_EVENT_DELAY : Int = 1000

// set to 0.5 after debug
const val esm_propability = 1
// set to 360000 (6 minutes) after debug
const val MINIMUM_DIALOG_DELAY = 360000
// set to 360000 (6 minute) after debug
const val SCREEN_OFF_QUESTIONNAIRE_DELAY = 360000

open class BaselineService : Service() {
    private lateinit var screenStateReceiver: ScreenStateReceiver
    private var receiverRegistered : Boolean = false

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

        uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/lifecycle_events/${System.currentTimeMillis()}",
            FirebaseUtils.FirebaseDataLoggingObject(event = "BASELINE_SERVICE_STARTED"))

        screenStateReceiver = ScreenStateReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (!receiverRegistered) {
            registerReceiver(screenStateReceiver, filter)
            receiverRegistered = true
        }

        setStudyVariable(this, SCREEN_OFF_QUESTIONNAIRE_PENDING, 0)

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::screenStateReceiver.isInitialized) {
            unregisterReceiver(screenStateReceiver)
            receiverRegistered = false
        }

        notificationManager = getSystemService(NotificationManager::class.java)
        if (::notificationManager.isInitialized) {
            notificationManager.cancel(1)
            notificationManager.cancel(1001)
        }
    }

    open inner class ScreenStateReceiver : BroadcastReceiver() {

        private lateinit var dialog : UnlockDialog
        private var lastDialogTimestamp : Long = 0L
        private var lastScreenOffQuestionnaire : Long = 0L
        private var userPresentDialogShown : Boolean = false

        lateinit var screenEvent : ScreenEvent
        var previousEventTimestamp : Long = 1L
        lateinit var previousEventType : String
        private var lastScreenEvent : Long = 0L
        private var lastScreenoff : Long = 0L

        private var sessionStartTimestamp : Long = 0L
        private var dialogSkipCount : Int = 0

        override fun onReceive(p0: Context?, p1: Intent?) {
            if (::dialog.isInitialized) dialog.close(p0)
            lastScreenEvent = System.currentTimeMillis()

            dialogSkipCount = getDailySkipCount(p0)

            val (ts, type) = getPreviousEvent(p0)
            previousEventTimestamp = ts
            previousEventType = type

            // have to skip incase we dont have a timestamp, otherwise WILL
            // cause weird bugs with duration
            // the getPreviousEvent() defaults to System.currentTimeMillis() to this should not occur
            if (previousEventTimestamp == 0L) return

            val now = System.currentTimeMillis()
            if (now - previousEventTimestamp < MULTIPLE_SCREEN_EVENT_DELAY && now != previousEventTimestamp) {
                Log.d("Baseline", "Returned early from onReceive()")
                return
            }

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

                    cancelUsageFlowDialogs()

                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    // updating to start a NEW event with type ACTION_SCREEN_OFF
                    // this event is stored when the NEXT even triggers and is then stored as even of THIS type.
                    updatePreviousEvent(now, "ACTION_SCREEN_OFF", p0)

                    lastScreenoff = now

                    if (::dialog.isInitialized) dialog.close(p0)

                    if (getStudyVariable(p0, DIALOG_RESPONSE, 0) == 1 && dialogSkipCount <= MAXIMUM_SKIPS) {
                        if (p0 != null) promptScreenOffQuestionnaire(p0)
                    }
                }

                Intent.ACTION_USER_PRESENT -> {
                    // User unlocked the screen
                    Log.d("ScreenStateReceiver", "User Present (Unlocked)")
                    uploadFirebaseEntry("/users/${getCurrentUserUID()}/logging/screen_events/${System.currentTimeMillis()}",
                        FirebaseUtils.FirebaseDataLoggingObject(event = "ACTION_USER_PRESENT"))

                    if (getStudyVariable(p0, SCREEN_OFF_QUESTIONNAIRE_PENDING, 0) == 1) {
                        Toast.makeText(p0, "AndroidAtWork: Answer 'Why did you stop using your phone just now?' questionnaire by clicking the notification", Toast.LENGTH_SHORT).show()
                    }

                    if (now - previousEventTimestamp > MULTIPLE_SCREEN_EVENT_DELAY) uploadEvent(previousEventType, previousEventTimestamp, p0)
                    // updating to start a NEW event with type ACTION_USER_PRESENT
                    // this event is stored when the NEXT even triggers and is then stored as even of THIS type.
                    updatePreviousEvent(now, "ACTION_USER_PRESENT", p0)

                    // Reset session start
                    sessionStartTimestamp = now

                    if (Math.random() < esm_propability
                            && (now - lastDialogTimestamp > MINIMUM_DIALOG_DELAY)
                            && dialogSkipCount <= MAXIMUM_SKIPS
                            && getStudyVariable(p0, SCREEN_OFF_QUESTIONNAIRE_PENDING, 0) == 0) {
                        dialog = UnlockDialog()
                        dialog.showDialog(p0, DIALOG_TYPE_USAGE_INITIATED)
                        Log.d("dialog", "Calling dialog to be shown")
                        lastDialogTimestamp = now
                        userPresentDialogShown = true

                        if (p0 != null) scheduleUsageFlowDialogs(p0)
                    }
                    else {
                        userPresentDialogShown = false
                    }
                }
            }
        }

        private var usageFlowHandler: Handler? = null
        private var usageFlowRunnable: Runnable? = null
        private var intervalIndex = 0
        private val INITIAL_INTERVAL = 3L
        private val ADDITIONAL_INTERVAL = 10L // every 10 min after

        private fun scheduleUsageFlowDialogs(context: Context) {
            cancelUsageFlowDialogs() // cancel any running timer

            usageFlowHandler = Handler(Looper.getMainLooper())
            intervalIndex = 0

            usageFlowRunnable = object : Runnable {
                override fun run() {
                    Toast.makeText(context, "AndroidAtWork: ESM arriving in 5 seconds", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        val dialog = UnlockDialog()
                        dialog.showDialog(context, DIALOG_TYPE_USAGE_CONTINUED)
                    }, 5000)

                    intervalIndex++
                    usageFlowHandler?.postDelayed(this, intervalToMillis(ADDITIONAL_INTERVAL)) // schedule next after 10 min
                }
            }

            // Schedule first at 3 minutes
            usageFlowHandler?.postDelayed(usageFlowRunnable!!, intervalToMillis(INITIAL_INTERVAL))
        }

        private fun cancelUsageFlowDialogs() {
            usageFlowHandler?.removeCallbacksAndMessages(null)
            usageFlowHandler = null
            usageFlowRunnable = null
        }


        // after debug switch to 'minutes * 60 * 1000'
        private fun intervalToMillis(minutes: Long) = minutes * 60 * 1000

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

        private fun promptScreenOffQuestionnaire(p0 : Context) {
            // dont show screenOffQuestionnaire repeatedly
            if (System.currentTimeMillis() - lastScreenOffQuestionnaire <= SCREEN_OFF_QUESTIONNAIRE_DELAY) return

            val intent = Intent(p0, ScreenOffQuestionnaire::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                p0, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = p0.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "screen_off_channel", "Screen Off Prompts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val notification = NotificationCompat.Builder(p0, "screen_off_channel")
                    .setSmallIcon(R.drawable.ic_notification_important) // your own icon
                    .setContentTitle("Why did you stop using your phone just now?")
                    .setContentText("Please answer a quick questionnaire.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setColorized(true)
                    .setColor(ContextCompat.getColor(p0, R.color.blue_800))
                    .build()

                notificationManager.notify(1001, notification)
                setStudyVariable(p0, SCREEN_OFF_QUESTIONNAIRE_PENDING, 1)

                // Schedule removal after 3 minutes
                Handler(Looper.getMainLooper()).postDelayed({
                    notificationManager.cancel(1001)
                    setStudyVariable(p0, SCREEN_OFF_QUESTIONNAIRE_PENDING, 0)
                }, 180_000)
            }, 1000) // send notification after one second.
        }
    }

}