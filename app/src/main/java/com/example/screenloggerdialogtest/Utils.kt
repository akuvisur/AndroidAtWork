package com.example.screenloggerdialogtest

import android.app.Service.WINDOW_SERVICE
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.time.LocalTime


const val BASELINE_DURATION = 14
const val INT1_DURATION = 7
const val INT2_DURATION = 21

const val DEBUG = true

// variable name for Shared Preferences filename
const val STUDY_STATE_SHAREDPREFS : String = "STUDY_STATE_SHAREDPREFS"
const val SCREEN_EVENTS_SHAREDPREFS : String = "SCREEN_EVENTS_SHAREDPREFS"

// variable names for STUDY_STATE sharedPrefs objects
const val CONSENT_GIVEN: String = "CONSENT_GIVEN"
const val SPAI_1_SUBMITTED: String = "SPAI_1_SUBMITTED"
const val SASSV_1_SUBMITTED: String = "SASSV_1_SUBMITTED"
const val ONBOARDING_COMPLETED: String = "ONBOARDING_COMPLETED"
const val BASELINE_START_TIMESTAMP: String = "BASELINE_START_TIMESTAMP"
const val BASELINE_COMPLETED: String = "BASELINE_COMPLETED"
const val INT_SMARTPHONE_USAGE_LIMIT_GOAL: String = "INT_SMARTPHONE_USAGE_LIMIT_GOAL"
const val BEDTIME_GOAL: String = "BEDTIME_GOAL"
const val INT1_START_TIMESTAMP: String = "INT1_START_TIMESTAMP"
const val INT2_START_TIMESTAMP: String = "INT2_START_TIMESTAMP"
const val EXIT_SURVEY_COMPLETED: String = "EXIT_SURVEY_COMPLETED"
const val SPAI_2_SUBMITTED: String = "SPAI_2_SUBMITTED"
const val SASSV_2_SUBMITTED: String = "SASSV_2_SUBMITTED"
const val STUDY_COMPLETE_TIMESTAMP: String = "STUDY_COMPLETE_TIMESTAMP"

private fun getStudyStateSharedPreferences(c: Context?): SharedPreferences? {
    return c?.applicationContext?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
}

fun setStudyVariable(c : Context?, variable : String, value : Int) {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putInt(variable, value)
        editor.apply()
    }
    if (c != null) Toast.makeText(c, "Study variable $variable changed to $value", Toast.LENGTH_SHORT).show()
}

// overloaded function to store Long values (INT_SMARTPHONE_USAGE_LIMIT_GOAL, INT_BEDTIME)
fun setStudyVariable(c : Context?, variable : String, value : Long) {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putLong(variable, value)
        editor.apply()
    }
    if (c != null) Toast.makeText(c, "Study variable $variable changed to $value", Toast.LENGTH_SHORT).show()
}

// Getter for Int values
fun getStudyVariable(c: Context?, variable: String, defaultValue: Int = 0): Int {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    return sharedPrefs?.getInt(variable, defaultValue) ?: defaultValue
}

// Overloaded getter for Long values
fun getStudyVariable(c: Context?, variable: String, defaultValue: Long = 0L): Long {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    return sharedPrefs?.getLong(variable, defaultValue) ?: defaultValue
}


fun setStudyTimestamp(c: Context?, variable: String, value: Long) {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putLong(variable, value)
        editor.apply()
    }
    if (c != null) {
        Toast.makeText(c, "Study variable $variable changed to $value", Toast.LENGTH_SHORT).show()
    }
}

fun getStudyStateVariables(c: Context?): Map<String, Number?> {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val studyVariables = mapOf(
        CONSENT_GIVEN to sharedPrefs?.getInt(CONSENT_GIVEN, 0),
        SPAI_1_SUBMITTED to sharedPrefs?.getInt(SPAI_1_SUBMITTED, 0),
        SASSV_1_SUBMITTED to sharedPrefs?.getInt(SASSV_1_SUBMITTED, 0),
        ONBOARDING_COMPLETED to sharedPrefs?.getInt(ONBOARDING_COMPLETED, 0),
        BASELINE_COMPLETED to sharedPrefs?.getInt(BASELINE_COMPLETED, 0),
        INT_SMARTPHONE_USAGE_LIMIT_GOAL to sharedPrefs?.getLong(INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0),
        BEDTIME_GOAL to sharedPrefs?.getInt(BEDTIME_GOAL, 0),
        EXIT_SURVEY_COMPLETED to sharedPrefs?.getInt(EXIT_SURVEY_COMPLETED, 0),
        SPAI_2_SUBMITTED to sharedPrefs?.getInt(SPAI_2_SUBMITTED, 0),
        SASSV_2_SUBMITTED to sharedPrefs?.getInt(SASSV_2_SUBMITTED, 0)
    )
    return studyVariables
}

fun getStudyStateTimestamps(c: Context?): Map<String, Long?> {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val studyVariables = mapOf(
        BASELINE_START_TIMESTAMP to sharedPrefs?.getLong(BASELINE_START_TIMESTAMP, 0L),
        INT1_START_TIMESTAMP to sharedPrefs?.getLong(INT1_START_TIMESTAMP, 0L),
        INT2_START_TIMESTAMP to sharedPrefs?.getLong(INT2_START_TIMESTAMP, 0L),
        STUDY_COMPLETE_TIMESTAMP to sharedPrefs?.getLong(STUDY_COMPLETE_TIMESTAMP, 0L)
    )
    return studyVariables
}

fun calculateStudyPeriodDay(type: String, c: Context?): Int {
    // Retrieve the study state timestamps
    val studyTimestamps = getStudyStateTimestamps(c)

    // Determine the start timestamp for the given type
    val periodStartTimestamp = when (type) {
        "BASELINE_START_TIMESTAMP" -> studyTimestamps[BASELINE_START_TIMESTAMP]
        "INT1_START_TIMESTAMP" -> studyTimestamps[INT1_START_TIMESTAMP]
        "INT2_START_TIMESTAMP" -> studyTimestamps[INT2_START_TIMESTAMP]
        else -> null
    }

    // If the period start timestamp is null or not set, return -1
    if (periodStartTimestamp == null || periodStartTimestamp == 0L) {
        return -1
    }

    // Calculate the elapsed days for the selected period
    val now = System.currentTimeMillis()
    val elapsedMillis = now - periodStartTimestamp
    val elapsedDays = (elapsedMillis / (1000 * 60 * 60 * 24)).toInt()

    Log.d("UTILS", "${type} studyday: ${elapsedDays+1}")

    return elapsedDays + 1 // Adding 1 as the first day is "Day 1"
}


/*
    0 = installed, no consent
    1 = consent, no baseline started
    2 = baseline
    3 = baseline, going to INT1 [need user verification]
    4 = INT1
    5 = INT1, going to INT2
    6 = INT2
    7 = post-INT2, need exit surveys
    8 = Study complete

 */
const val STUDY_STATE : String = "STUDY_STATE"

const val STUDY_STATE_FIRST_LAUNCH = 0
const val STUDY_STATE_CONSENT_GIVEN = 1
const val STUDY_STATE_BASELINE_ONGOING = 2
const val STUDY_STATE_POST_BASELINE = 3
const val STUDY_STATE_INT1 = 4
const val STUDY_STATE_POST_INT1 = 5
const val STUDY_STATE_INT2 = 6
const val STUDY_STATE_POST_INT2_SURVEY_REQUIRED = 7
const val STUDY_STATE_COMPLETE = 8

fun setStudyState(c : Context?, state : Int) {
    var sharedPrefs = getStudyStateSharedPreferences(c)
    var editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putInt(STUDY_STATE, state)
        editor.apply()
    }
    if (c != null) Toast.makeText(c, "Study state changed to $state", Toast.LENGTH_SHORT).show()
}

fun getStudyState(c : Context?): Int {
    var sharedPrefs = getStudyStateSharedPreferences(c)
    if (sharedPrefs != null) {
        return sharedPrefs.getInt(STUDY_STATE, 0)
    }
    else return 0
}

/*

Dialog generation for INT1 phase

 */

class UnlockDialog() {

    lateinit var dialogView: View

    private lateinit var closeButton : Button
    private lateinit var submitButton : Button

    private lateinit var contextHomeButton: Button
    private lateinit var contextWorkButton: Button
    private lateinit var contextCommuteButton: Button
    private lateinit var contextOtherButton: Button
    private lateinit var workSchoolButton: Button
    private lateinit var leisureButton: Button
    private lateinit var otherButton: Button
    private lateinit var selfInitiatedButton: Button
    private lateinit var notificationInitiatedButton: Button

    private val row1 by lazy {
        listOf(contextHomeButton, contextWorkButton, contextCommuteButton, contextOtherButton)
    }
    private val row2 by lazy {
        listOf(workSchoolButton, leisureButton, otherButton)
    }
    private val row3 by lazy {
        listOf(selfInitiatedButton, notificationInitiatedButton)
    }

    private lateinit var extraInfo : EditText

    fun showGoalSurpassedDialog(
        c: Context?,
        curTime: Long,
        bedTime: Int,
        usage: Long,
        goal: Long
    ) {
        // copy from after
    }

    fun showBedtimeClosingDialog(
        c: Context?,
        curTime: Long,
        bedTime: Int,
        usage: Long,
        goal: Long
    ) {
        if (c == null) {
            return // Exit the function if context is null
        }

        val wm = c.getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(c)
        val dialogView = inflater.inflate(R.layout.dialog_layout_unlocked_bedtime, null)
        closeButton = dialogView.findViewById(R.id.closeButton)
        submitButton = dialogView.findViewById(R.id.submitButton)
        extraInfo = dialogView.findViewById(R.id.extraInfoEditText)

        // First row
        contextHomeButton = dialogView.findViewById(R.id.contextHomeButton)
        contextWorkButton = dialogView.findViewById(R.id.contextWorkButton)
        contextCommuteButton = dialogView.findViewById(R.id.contextCommuteButton)
        contextOtherButton = dialogView.findViewById(R.id.contextOtherButton)

        // Second row
        workSchoolButton = dialogView.findViewById(R.id.workSchoolButton)
        leisureButton = dialogView.findViewById(R.id.leisureButton)
        otherButton = dialogView.findViewById(R.id.otherButton)

        // Third row
        selfInitiatedButton = dialogView.findViewById(R.id.selfInitiatedButton)
        notificationInitiatedButton = dialogView.findViewById(R.id.notificationInitiatedButton)

        setupButtonGroups(c)

        // Set up the layout parameters for the WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER // Center the view on the screen

        closeButton.setOnClickListener {
            close(c)
        }

        submitButton.setOnClickListener {
            val row1Selection = getSelectedButtonText(c, row1)
            val row2Selection = getSelectedButtonText(c, row2)
            val row3Selection = getSelectedButtonText(c, row3)

            // Collect additional text from EditText
            val extraInfo = extraInfo.text.toString()

            // Create a Response object
            val data = Response(
                contextSelection = row1Selection,
                purposeSelection = row2Selection,
                initiationSelection = row3Selection,
                extraInfo = extraInfo
            )

            FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/dialog_responses/",data) // does this need onSuccesss?

            close(c)
        }

        // Add the view to the window
        show(dialogView, wm, layoutParams, c)
    }

    fun showTestDialogScreen(
        c: Context?
    ) {
        // show a test dialog but dont store anything
        // override submit button behaviour
        // TODO the UnlockDialog class needs separate build() and show() functions that are called externally
        // TODO or just a hacky code duplication method to show a test view.
    }

    private fun show(dialogView : View, wm : WindowManager, layoutParams : WindowManager.LayoutParams, c : Context) {
        wm.addView(dialogView, layoutParams)
        // TODO add note to database that a dialog was created
    }

    fun close(c: Context?) {
        if (c == null) return
        val wm = c.getSystemService(WINDOW_SERVICE) as WindowManager
        wm.removeView(dialogView)


    }

    // Function to initialize button click listeners
    private fun setupButtonGroups(c : Context) {
        setupRowButtons(c, row1)
        setupRowButtons(c, row2)
        setupRowButtons(c, row3)
    }

    // Function to set up selection logic for a row of buttons
    private fun setupRowButtons(c : Context, buttons: List<Button>) {
        // Store original background for each button in this row
        val originalBackgrounds = buttons.associateWith { it.background }

        buttons.forEach { button ->
            button.setOnClickListener {
                // Reset all buttons in the row to their original background
                buttons.forEach { btn ->
                    btn.background = originalBackgrounds[btn]
                }

                // Highlight the selected button
                button.setBackgroundColor(ContextCompat.getColor(c, R.color.white)) // Replace with desired highlight color
            }
        }
    }

    private fun getSelectedButtonText(c: Context, buttons: List<Button>): String? {
        return buttons.find { it.background.constantState == ContextCompat.getDrawable(c, R.color.white)?.constantState }?.text?.toString()
    }

    data class Response(
        val contextSelection: String?,
        val purposeSelection: String?,
        val initiationSelection: String?,
        val extraInfo: String
    )

}


/*

Screen event tracking

 */
private fun getScreenSharedPreferences(c: Context?): SharedPreferences? {
    return c?.applicationContext?.getSharedPreferences(SCREEN_EVENTS_SHAREDPREFS, Context.MODE_PRIVATE)
}

private const val PREVIOUS_SCREEN_EVENT_TS = "PREVIOUS_SCREEN_EVENT_TS" //when
private const val PREVIOUS_SCREEN_EVENT_TYPE = "PREVIOUS_SCREEN_EVENT_TYPE" //what

// Function to save the previous event time in SharedPreferences
fun putPreviousEvent(time: Long, type: String, c: Context?) {
    val sharedPreferences = getScreenSharedPreferences(c)
    sharedPreferences?.edit()?.putLong(PREVIOUS_SCREEN_EVENT_TS, time)?.apply()
    sharedPreferences?.edit()?.putString(PREVIOUS_SCREEN_EVENT_TYPE, type)?.apply()
}

// Function to retrieve the previous event time from SharedPreferences
fun getPreviousEvent(c: Context?): Pair<Long, String> {
    val sharedPreferences = getScreenSharedPreferences(c)

    val previousTime = sharedPreferences?.getLong(PREVIOUS_SCREEN_EVENT_TS, System.currentTimeMillis()) ?: System.currentTimeMillis()
    val previousType = sharedPreferences?.getString(PREVIOUS_SCREEN_EVENT_TYPE, "") ?: ""

    return Pair(previousTime, previousType)
}


private const val DAILY_USAGE = "DAILY_USAGE"
fun storeDailyUsage(duration : Long, c: Context?) {
    val sharedPreferences = getScreenSharedPreferences(c)
    sharedPreferences?.edit()?.putLong(DAILY_USAGE, duration)?.apply()
}

fun getDailyUsage(c : Context?) : Long {
    val sharedPreferences = getScreenSharedPreferences(c)
    val previousUsage = sharedPreferences?.getLong(DAILY_USAGE, 0L)
    return(previousUsage!!)
}

private const val LAST_USAGE_SESSION_TS = "LAST_USAGE_SESSION_TS"
fun storeLastDailyUsageTime(time : Long, c: Context?) {
    val sharedPreferences = getScreenSharedPreferences(c)
    sharedPreferences?.edit()?.putLong(LAST_USAGE_SESSION_TS, time)?.apply()
}

fun getLastDailyUsageTime(c : Context?) : Long {
    val sharedPreferences = getScreenSharedPreferences(c)
    val previousUsage = sharedPreferences?.getLong(LAST_USAGE_SESSION_TS, 0L)
    return(previousUsage!!)
}

/*
    random utils
 */

fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return String.format("%02d:%02d", hours, mins)
}

fun calculateMinutesUntilBedtime(bedtimeInMinutes: Int): Int {
    val now = LocalTime.now()
    val currentMinutes = (now.hour * 60) + now.minute
    val minutesUntilBedtime = if (currentMinutes <= bedtimeInMinutes) {
        bedtimeInMinutes - currentMinutes
    } else {
        // If bedtime is on the next day
        (1440 - currentMinutes) + bedtimeInMinutes
    }
    return minutesUntilBedtime
}

fun parseBedtimeToMinutes(input : String): Int {
    val regex = Regex("(\\d+)h\\s*(\\d+)m")
    val matchResult = regex.find(input)

    if (matchResult != null) {
        val (hours, minutes) = matchResult.destructured
        val bedtimeHours = hours.toInt()
        val bedtimeMinutes = minutes.toInt()
        val bedtimeInMinutes = (bedtimeHours * 60) + bedtimeMinutes
        return (bedtimeInMinutes)
    }
    return(0)
}