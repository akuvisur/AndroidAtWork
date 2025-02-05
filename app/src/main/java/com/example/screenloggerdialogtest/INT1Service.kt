package com.example.screenloggerdialogtest

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

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

class INT1Service : INT2Service() {

    private lateinit var INT1Receiver : INT1ScreenReceiver

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SERVICE_LOGIC", "INT1 starting")

        // this is largely irrelevant since its reset in INT2Service (super.onStart...)
        studyPhase = 1

        INT1Receiver = INT1ScreenReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(INT1Receiver, filter)

        return super.onStartCommand(intent, flags, startId)
    }

    inner class INT1ScreenReceiver : INT2Service.INT2ScreenReceiver() {
        private lateinit var dialog : UnlockDialog

        override fun onReceive(p0: Context?, p1: Intent?) {

            // Additional functionality for Service B
            if (p1?.action == Intent.ACTION_USER_PRESENT) {
                Log.d("INT1", "usage: ${dailyUsage} goal: ${dailyUsageGoal} diff: ${dailyUsageGoalDiff}")
                Log.d("INT1", "bed goal ${bedtimeGoal} minutes until bed: ${calculateMinutesUntilBedtime(bedtimeGoal)}")
                Log.d("INT1", "usage within 45 seconds: ${usageWithin45Seconds(p0, now)}")
                // Prioritise showing the bedtime dialog
                if (calculateMinutesUntilBedtime(bedtimeGoal) <= 60 && !usageWithin45Seconds(p0, now)) {
                    dialog = UnlockDialog()
                    dialog.showDialog(p0, now, bedtimeGoal, dailyUsage, dailyUsageGoal, DIALOG_TYPE_BEDTIME, false)
                }
                else if (dailyUsage > dailyUsageGoal && !usageWithin45Seconds(p0, now)) {
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
                    FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/dialog_responses/", data)
                }
                if (::dialog.isInitialized) dialog.close(p0)
            }

            super.onReceive(p0, p1) // Retain functionality from Service A
        }
    }
}