package com.example.screenloggerdialogtest

import android.content.Context
import android.content.Intent
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {



        return super.onStartCommand(intent, flags, startId)
    }

    inner class INT1ScreenReceiver : INT2Service.INT2ScreenReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            super.onReceive(p0, p1) // Retain functionality from Service A

            // Additional functionality for Service B
            if (p1?.action == Intent.ACTION_SCREEN_ON) {
                // Do something additional when the screen is turned on
                Log.d("AugmentedScreenStateReceiver", "Additional functionality in Service INT1")
                // e.g., start a new service or send a broadcast, etc.

                showDialog(p0)
            }
        }
    }
}