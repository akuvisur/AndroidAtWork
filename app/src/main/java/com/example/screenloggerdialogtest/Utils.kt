package com.example.screenloggerdialogtest

import android.animation.ObjectAnimator.*
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Service.WINDOW_SERVICE
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val BASELINE_DURATION = 14
const val INT1_DURATION = 7
const val INT2_DURATION = 21

const val CONTINUOUS_USAGE_NOTIFICATION_TIMELIMIT = 60 * 10 * 1000L
val CONTINUOUS_USAGE_TEXT = arrayOf(
    "You've been on your phone for %s minutes. Consider taking a short break!",
    "Time flies! You've already spent %s minutes on your phone. Maybe it's time to look up?",
    "Are you still being productive? You've used your phone for %s minutes.",
    "Take a moment to stretch and rest your eyes. You've been using your phone for %s minutes.",
    "Consider setting your phone aside for a bit. You've used it for %s minutes.",
    "You've reached %s minutes of continuous screen time. How about engaging in an off-screen activity?",
    "Your screen time since you opened your phone is %s minutes. Remember to balance your online and offline time!",
    "You've been using your phone for %s minutes. Is it time to recharge yourself as well?",
    "Take a break from the screen. You've used your phone for %s minutes.",
    "You've accumulated %s minutes of continuous screen time. How about a quick walk or chat with someone nearby?",
    "Did you have something else to do? You just spent %s minutes on your phone."
)

const val SETTINGS_PASSKEY = "7621"

// variable name for Shared Preferences filename
const val STUDY_STATE_SHAREDPREFS : String = "STUDY_STATE_SHAREDPREFS"
const val SCREEN_EVENTS_SHAREDPREFS : String = "SCREEN_EVENTS_SHAREDPREFS"

// variable names for STUDY_STATE sharedPrefs objects
const val CONSENT_GIVEN: String = "CONSENT_GIVEN"
const val SPAI_1_SUBMITTED: String = "SPAI_1_SUBMITTED"
const val SASSV_1_SUBMITTED: String = "SASSV_1_SUBMITTED"
const val TSRQ_SUBMITTED: String = "TSRQ_SUBMITTED"
const val ONBOARDING_COMPLETED: String = "ONBOARDING_COMPLETED"
const val BASELINE_START_TIMESTAMP: String = "BASELINE_START_TIMESTAMP"
const val BASELINE_COMPLETED: String = "BASELINE_COMPLETED"
const val BASELINE_USAGE_AVERAGE : String = "BASELINE_USAGE_AVERAGE"
const val INT_SMARTPHONE_USAGE_LIMIT_GOAL: String = "INT_SMARTPHONE_USAGE_LIMIT_GOAL"
const val INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE : String = "INT_SMARTPHONE_USAGE_LIMIT_PERCENTAGE"
const val BEDTIME_GOAL: String = "BEDTIME_GOAL"
const val INT1_START_TIMESTAMP: String = "INT1_START_TIMESTAMP"
const val INT2_START_TIMESTAMP: String = "INT2_START_TIMESTAMP"
const val EXIT_SURVEY_COMPLETED: String = "EXIT_SURVEY_COMPLETED"
const val SPAI_2_SUBMITTED: String = "SPAI_2_SUBMITTED"
const val SASSV_2_SUBMITTED: String = "SASSV_2_SUBMITTED"
const val STUDY_COMPLETE_TIMESTAMP: String = "STUDY_COMPLETE_TIMESTAMP"

const val STUDY_DIALOG_LASTSHOWN_TIMESTAMP: String = "STUDY_DIALOG_LASTSHOWN_TIMESTAMP"
const val DIALOG_RESPONSE : String = "DIALOG_RESPONSE"
const val SCREEN_OFF_QUESTIONNAIRE_PENDING : String = "SCREEN_OFF_QUESTIONNAIRE_PENDING"

fun showClearConfirmationDialog(context: Context?) {
    context?.let {
        val builder = AlertDialog.Builder(it)
        builder.setTitle("Confirmation")
            .setMessage("Are you sure you want to clear all saved data?")
            .setPositiveButton("Yes") { _, _ ->
                clearStudyStateSharedPreferences(context)
                Toast.makeText(context, "Data cleared.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }
}

private fun clearStudyStateSharedPreferences(context: Context?) {
    context?.applicationContext?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
        ?.edit()
        ?.clear()
        ?.apply()
}

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
    uploadStudyVariable(c, variable, value)
}

// overloaded function to store Long values (INT_SMARTPHONE_USAGE_LIMIT_GOAL, INT_BEDTIME)
fun setStudyVariable(c : Context?, variable : String, value : Long) {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putLong(variable, value)
        editor.apply()
    }
    uploadStudyVariable(c, variable, value)
}

fun uploadStudyVariable(c : Context?, variable : String, value : Number) {
    val studyVariableData = hashMapOf(
        "variable" to variable,
        "value" to value,
        "timestamp" to System.currentTimeMillis()
    )

    FirebaseUtils.sendEntryToDatabase(
        path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info/${System.currentTimeMillis()}",
        data = studyVariableData,
        onSuccess = {
            //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
        },
        onFailure = { exception ->
            // Handle failure
            //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    )
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
}

fun getStudyStateVariables(c: Context?): Map<String, Number?> {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val studyVariables = mapOf(
        CONSENT_GIVEN to sharedPrefs?.getInt(CONSENT_GIVEN, 0),
        SPAI_1_SUBMITTED to sharedPrefs?.getInt(SPAI_1_SUBMITTED, 0),
        SASSV_1_SUBMITTED to sharedPrefs?.getInt(SASSV_1_SUBMITTED, 0),
        TSRQ_SUBMITTED to sharedPrefs?.getInt(TSRQ_SUBMITTED, 0),
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
        return 1
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
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putInt(STUDY_STATE, state)
        editor.apply()
    }

    val stateData = hashMapOf(
        "study_state" to state,
        "timestamp" to System.currentTimeMillis()
    )

    FirebaseUtils.sendEntryToDatabase(
        path = "users/${FirebaseUtils.getCurrentUserUID()}/study_state_info/${System.currentTimeMillis()}",
        data = stateData,
        onSuccess = {
            //Toast.makeText(requireContext(), "Screen event sent successfully", Toast.LENGTH_SHORT).show()
        },
        onFailure = { exception ->
            // Handle failure
            //Toast.makeText(requireContext(), "Failed to send data: ${exception.message}", Toast.LENGTH_LONG).show()
        }
    )
}

fun getStudyState(c : Context?): Int {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    if (sharedPrefs != null) {
        return sharedPrefs.getInt(STUDY_STATE, 0)
    }
    else return 0
}

/*

Dialog generation for INT1 phase

- Dialog functionality:
    - Create a timestamp as ID (99.9% unique) when dialog is created
    - Show either bedtime closing or goal exceeded according to constructor variable
        - Both have same functionality, just minor layout differences
    - Dialog is automatically closed after five minutes
    - When user clicks either 'close' or 'submit', entry is sent to Firebase with data
        - no input data is submitted even if selected when clicking 'close'
        - see Response and IgnoredResponse data classes at the bottom

 */

const val DIALOG_TYPE_USAGE_INITIATED : String = "DIALOG_TYPE_USAGE_INITIATED"
const val DIALOG_TYPE_USAGE_CONTINUED : String = "DIALOG_TYPE_USAGE_CONTINUED"

const val DIALOG_RESPONSE_SUBMITTED : String = "DIALOG_RESPONSE_SUBMITTED"
const val DIALOG_RESPONSE_IGNORED : String = "DIALOG_RESPONSE_IGNORED"
const val DIALOG_RESPONSE_AUTOMATICALLY_CLOSED : String = "DIALOG_RESPONSE_AUTOMATICALLY_CLOSED"

class UnlockDialog {

    private lateinit var dialogView: View

    private var dialogAnswered : Boolean = false

    var dialogCreatedTimestamp : Long = 0L

    private var localTestDialogVariable : Boolean = false

    private lateinit var closeButton : Button
    private lateinit var submitButton : Button
    private lateinit var infoText : TextView

    private lateinit var whatdoinkCommunicationButton: Button
    private lateinit var whatdoinkLeisureButton: Button
    private lateinit var whatdoinkUtilitiesButton: Button
    private lateinit var whatdoinkOtherButton: Button

    private lateinit var purposeWorkButton: Button
    private lateinit var purposePersonalButton: Button
    private lateinit var purposeMixButton: Button
    private lateinit var purposeNotApplicableButton: Button

    private lateinit var triggerExternalButton: Button
    private lateinit var triggerInnerButton: Button
    private lateinit var triggerPasstimeButton: Button
    private lateinit var triggerNoReasonButton: Button

    private val row1 by lazy {
        listOf(whatdoinkCommunicationButton, whatdoinkLeisureButton, whatdoinkUtilitiesButton, whatdoinkOtherButton)
    }
    private val row2 by lazy {
        listOf(purposeWorkButton, purposePersonalButton, purposeMixButton, purposeNotApplicableButton)
    }
    private val row3 by lazy {
        listOf(triggerExternalButton, triggerInnerButton, triggerPasstimeButton, triggerNoReasonButton)
    }

    @SuppressLint("SetTextI18n")
    fun showDialog(
        c: Context?,
        type: String
    ) {
        if (c == null) {
            return // Exit the function if context is null
        }

        val skipCount = getDailySkipCount(c)
        val lastShown = getStudyVariable(c, STUDY_DIALOG_LASTSHOWN_TIMESTAMP, 0L)
        // Get the current time in milliseconds
        val currentTime = System.currentTimeMillis()

        /*
        // Define ten minutes in milliseconds
        val tenMinutesInMillis = 10 * 60 * 1000
        // Check if ten minutes have passed since the last shown time
        if (currentTime - lastShown < tenMinutesInMillis) {
            return // exit if less than ten minutes since last dialog
        }
        */

        dialogCreatedTimestamp = System.currentTimeMillis()

        val wm = c.getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(c)
        dialogView = when (type) {
            DIALOG_TYPE_USAGE_INITIATED -> {
                inflater.inflate(R.layout.dialog_layout_unlocked, null)
            }
            DIALOG_TYPE_USAGE_CONTINUED -> {
                inflater.inflate(R.layout.dialog_layout_unlocked, null)
            }
            else -> {
                return // ignore creating dialog if you somehow end up here.
            }
        }

        closeButton = dialogView.findViewById(R.id.closeButton)
        submitButton = dialogView.findViewById(R.id.submitButton)

        infoText = dialogView.findViewById(R.id.infoTextTextView)

        whatdoinkCommunicationButton = dialogView.findViewById(R.id.whatdoink_communication)
        whatdoinkLeisureButton = dialogView.findViewById(R.id.whatdoink_leisure)
        whatdoinkUtilitiesButton = dialogView.findViewById(R.id.whatdoink_utilities)
        whatdoinkOtherButton = dialogView.findViewById(R.id.whatdoink_other)

        purposeWorkButton = dialogView.findViewById(R.id.purpose_work)
        purposePersonalButton = dialogView.findViewById(R.id.purpose_personal)
        purposeMixButton = dialogView.findViewById(R.id.purpose_mix)
        purposeNotApplicableButton = dialogView.findViewById(R.id.purpose_not_applicable)

        triggerExternalButton = dialogView.findViewById(R.id.trigger_external)
        triggerInnerButton = dialogView.findViewById(R.id.trigger_inner)
        triggerPasstimeButton = dialogView.findViewById(R.id.trigger_passtime)
        triggerNoReasonButton = dialogView.findViewById(R.id.trigger_noreason)

        closeButton.text = "Skip ${skipCount}/${MAXIMUM_SKIPS}"

        val drawable = ContextCompat.getDrawable(c, R.drawable.dialog_corners)?.mutate()
        val wrapped = DrawableCompat.wrap(drawable!!)
        closeButton.background = wrapped // set background first
        if (skipCount in (MAXIMUM_SKIPS-2)..(MAXIMUM_SKIPS-1)) {
            closeButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(c, R.color.yellow))
            infoText.setText(R.string.dialog_unlocked_skip4)
        } else if (skipCount >= MAXIMUM_SKIPS) {
            closeButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(c, R.color.red))
            closeButton.setTextColor(ContextCompat.getColor(c, R.color.white))
            infoText.setText(R.string.dialog_unlocked_skip6)
        }

        /*
        val infoTextRes = when (type) {
            DIALOG_TYPE_USAGE_INITIATED -> R.string.dialog_unlocked_info
            DIALOG_TYPE_USAGE_CONTINUED -> R.string.dialog_unlocked_info_continued
            else -> R.string.dialog_unlocked_info // optional fallback
        }

        infoText.setText(infoTextRes)
*/
        setupButtonGroups(c)

        // Set up the layout parameters for the WindowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER // Center the view on the screen

        closeButton.setOnClickListener {
            Log.d("dialog", "closebutton clicked")
            if (!localTestDialogVariable) {
                val data = IgnoredResponse(
                    dialogType = type,
                    dialogClosedTimestamp = System.currentTimeMillis(),
                    dialogCreatedTimestamp = dialogCreatedTimestamp,
                    response = DIALOG_RESPONSE_IGNORED
                )
                FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/dialog_responses/${System.currentTimeMillis()}",data) // does this need onSuccesss?
            }
            val currentSkipCount = getDailySkipCount(c)
            setDailySkipCount(c, currentSkipCount + 1)

            // keeps track of required screenOffQuestionnaires
            setStudyVariable(c, DIALOG_RESPONSE, 0)

            dialogAnswered = true
            close(c)
        }

        submitButton.setOnClickListener {
            Log.d("dialog", "submitbutton clicked")

            val row1Selection = getSelectedButtonText(c, row1)
            val row2Selection = getSelectedButtonText(c, row2)
            val row3Selection = getSelectedButtonText(c, row3)

            if (row1Selection == null || row2Selection == null || row3Selection == null) {
                infoText.setText(R.string.dialog_unlocked_info)
                val animator = ofArgb(infoText, "textColor",
                    ContextCompat.getColor(c, R.color.red),
                    ContextCompat.getColor(c, R.color.black)) // Replace default_text_color with your normal color
                animator.duration = 500
                animator.repeatCount = 3
                animator.repeatMode = ValueAnimator.REVERSE
                animator.start()
            }
            else {
                // Create a Response object
                val data = Response(
                    whatdoinkSelection = row1Selection,
                    purposeSelection = row2Selection,
                    triggerSelection = row3Selection,
                    dialogType = type,
                    dialogClosedTimestamp = System.currentTimeMillis(),
                    dialogCreatedTimestamp = dialogCreatedTimestamp,
                    response = DIALOG_RESPONSE_SUBMITTED
                )
                // keeps track of required screenOffQuestionnaires
                setStudyVariable(c, DIALOG_RESPONSE, 1)

                if (!localTestDialogVariable) FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/dialog_responses//${System.currentTimeMillis()}",data) // does this need onSuccesss?

                dialogAnswered = true

                close(c)
            }
        }

        setStudyVariable(c, STUDY_DIALOG_LASTSHOWN_TIMESTAMP, System.currentTimeMillis())

        // Set up a timer to close the dialog automatically after 5 minutes
        Handler(Looper.getMainLooper()).postDelayed({
            if (!localTestDialogVariable && !dialogAnswered) {
                val sentData = IgnoredResponse(
                    dialogType = type,
                    dialogClosedTimestamp = System.currentTimeMillis(),
                    dialogCreatedTimestamp = dialogCreatedTimestamp,
                    response = DIALOG_RESPONSE_AUTOMATICALLY_CLOSED
                )
                FirebaseUtils.sendEntryToDatabase("users/${FirebaseUtils.getCurrentUserUID()}/dialog_responses//${System.currentTimeMillis()}",sentData) // does this need onSuccesss?
            }
            close(c)
        }, 60000L) // 1 minute
        // Add the view to the window
        show(dialogView, wm, layoutParams, c)
    }

    private fun show(dialogView : View, wm : WindowManager, layoutParams : WindowManager.LayoutParams, c : Context) {
        wm.addView(dialogView, layoutParams)
    }

    fun close(c: Context?) {
        if (c == null || !::dialogView.isInitialized) return // Ensure dialogView is initialized

        val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (dialogView.isAttachedToWindow) {
            dialogView.animate()
                .alpha(0f)
                .setDuration(550)
                .setStartDelay(50)
                .withEndAction {
                    wm.removeView(dialogView)
                }
                .start()
        }
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
        val originalBackgroundTints = buttons.associateWith { it.backgroundTintList }

        buttons.forEach { button ->
            button.setOnClickListener {
                // Reset all buttons in the row to their original background tint
                buttons.forEach { btn ->
                    btn.backgroundTintList = originalBackgroundTints[btn]
                    btn.setTextColor(ContextCompat.getColor(c, R.color.white))
                }
                // Highlight the selected button by setting the background tint to white
                button.backgroundTintList = ContextCompat.getColorStateList(c, R.color.white)
                button.setTextColor(ContextCompat.getColor(c, R.color.blue_gray_900))
            }
        }
    }

    private fun getSelectedButtonText(c: Context, buttons: List<Button>): String? {
        val whiteColorStateList = ContextCompat.getColorStateList(c, R.color.white)
        return buttons.find { it.backgroundTintList == whiteColorStateList }?.text?.toString()
    }

    data class Response(
        val whatdoinkSelection: String?,
        val purposeSelection: String?,
        val triggerSelection: String?,
        val dialogType: String,
        val dialogClosedTimestamp: Long,
        val dialogCreatedTimestamp: Long,
        val response : String
    )

    data class IgnoredResponse(
        val dialogType: String,
        val dialogClosedTimestamp: Long,
        val dialogCreatedTimestamp: Long,
        val response : String
    )

    data class AdheredResponse(
        val dialogType: String,
        val dialogClosedTimestamp: Long,
        val dialogCreatedTimestamp: Long,
        val response : String
    )

}


fun setDailySkipCount(c: Context?, count: Int) {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val editor = sharedPrefs?.edit()
    val key = "skips_${getTodayString()}"
    editor?.putInt(key, count)
    editor?.apply()
}

fun getDailySkipCount(c: Context?): Int {
    val sharedPrefs = getStudyStateSharedPreferences(c)
    val key = "skips_${getTodayString()}"
    return sharedPrefs?.getInt(key, 0) ?: 0
}

private fun getTodayString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}

/*

Screen event tracking

 */
fun getScreenSharedPreferences(c: Context?): SharedPreferences? {
    return c?.getSharedPreferences("screen_prefs", Context.MODE_PRIVATE)
}

fun getDailyUsageSharedPreferences(c: Context?): SharedPreferences? {
    return c?.getSharedPreferences("daily_usage_prefs", Context.MODE_PRIVATE)
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

fun storeDailyUsage(duration: Long, c: Context?) {
    val sharedPreferences = getDailyUsageSharedPreferences(c)
    val editor = sharedPreferences?.edit()

    val today = getTodayAsString() // Get today's date as the key

    // Store the usage with the date as the key
    Log.d("storeDailyUsage()", "Storing: $duration for $today")
    editor?.putLong(today, duration)
    editor?.apply()
    Log.d("storeDailyUsage()", "Stored: ${sharedPreferences?.getLong(today, -99L)} for $today")
}

fun getDailyUsage(c: Context?, call_from : String): Long {
    val sharedPreferences = getDailyUsageSharedPreferences(c)

    val dur = sharedPreferences?.getLong(getTodayAsString(), -1L) ?: 0L
    val key = getTodayAsString()
    // Retrieve the usage value for today, defaulting to 0 if not found
    Log.d("getDailyUsage()", "today: $key value: $dur call_from: $call_from hash: ${c.hashCode()}")

    return dur
}

fun clearTodayUsage(c: Context?) {
    val sharedPreferences = getDailyUsageSharedPreferences(c)
    val editor = sharedPreferences?.edit()

    val today = getCurrentDateString()

    // Remove today's entry by using the date as the key
    editor?.remove(today)
    editor?.apply()
}

fun getTodayAsInt(): Int {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return dateFormat.format(Date()).toInt()
}

fun getTodayAsString(): String {
    return getTodayAsInt().toString()
}


fun getCurrentDateString(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return dateFormat.format(Date())
}

/*
    random utils
 */

@SuppressLint("DefaultLocale")
fun formatMinutesToTime(minutes: Int): String {
    val hours = minutes / 60
    val minutes = minutes % 60
    return String.format("%02d:%02d", hours, minutes)
}

@SuppressLint("DefaultLocale")
fun formatTimestampToReadableTime(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    return String.format(
        "%02d-%02d %02d:%02d:%02d",
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1, // Months are 0-based
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND)
    )
}

fun calculateMinutesUntilBedtime(bedtimeInMinutes: Int): Int {
    val now = LocalTime.now()
    val currentMinutes = (now.hour * 60) + now.minute
    val minutesUntilBedtime = if (currentMinutes <= bedtimeInMinutes) {
        bedtimeInMinutes - currentMinutes
    } else {
        // If bedtime is on the next day
        // calculate time to next midnight, then add bedtime minutes
        // this also works for e.g., 1AM bedtime.
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

fun convertMinutesToBedtime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return "${hours}h ${remainingMinutes}m"
}


fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(Int.MAX_VALUE).any { it.service.className == serviceClass.name }
}

fun checkServicesRunning(context: Context) {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

    val serviceClasses = listOf(
        BaselineService::class.java,
        ApplicationTrackingService::class.java
    )

    for (serviceClass in serviceClasses) {
        var isServiceRunning = false
        for (service in runningServices) {
            if (service.service.className == serviceClass.name) {
                isServiceRunning = true
                break
            }
        }
        Log.d("ServiceStatus", "${serviceClass.simpleName} is running: $isServiceRunning")
    }
}


// Worktime intervals
/*
// Save
saveWorkIntervals(this, listOf(TimeInterval("08:00", "12:00")))

// Fetch
val savedIntervals = getWorkIntervals(this)

 */
private const val KEY_WORK_INTERVALS = "work_intervals"
private const val KEY_STUDY_STARTED_TIMESTAMP = "study_started_timestamp"

fun saveWorkIntervals(context: Context?, intervals: List<TimeInterval>) {
    val sharedPrefs = getStudyStateSharedPreferences(context) ?: return
    val json = Gson().toJson(intervals)
    Log.d("work_intervals", json)

    with(sharedPrefs.edit()) {
        putString(KEY_WORK_INTERVALS, json)

        // Save start timestamp if not already set
        if (!sharedPrefs.contains(KEY_STUDY_STARTED_TIMESTAMP)) {
            putLong(KEY_STUDY_STARTED_TIMESTAMP, System.currentTimeMillis())
        }

        apply()
    }
}

fun getWorkIntervals(context: Context?): List<TimeInterval> {
    val sharedPrefs = getStudyStateSharedPreferences(context)
    val json = sharedPrefs?.getString(KEY_WORK_INTERVALS, null) ?: return emptyList()
    val type = object : TypeToken<List<TimeInterval>>() {}.type
    return Gson().fromJson(json, type)
}

private const val KEY_LAST_RESUME = "lastResume"
fun saveLastResume(context: Context?, timestamp: Long) {
    val sharedPrefs = getStudyStateSharedPreferences(context) ?: return
    with(sharedPrefs.edit()) {
        putLong(KEY_LAST_RESUME, timestamp)
        apply()
    }
}

// Get the last resume timestamp
fun getLastResume(context: Context?): Long {
    val sharedPrefs = getStudyStateSharedPreferences(context)
    return sharedPrefs?.getLong(KEY_LAST_RESUME, 0) ?: 0
}

fun getStudyStartedTimestamp(context: Context?): Long? {
    val timestamp = getStudyStateSharedPreferences(context)
        ?.getLong(KEY_STUDY_STARTED_TIMESTAMP, -1L) ?: -1L
    return if (timestamp != -1L) timestamp else null
}

const val MAXIMUM_SKIPS = 6
fun calculateStudyDay(context: Context?): Int {
    val startTimestamp = getStudyStartedTimestamp(context) ?: return -1
    val now = System.currentTimeMillis()
    val elapsed = now - startTimestamp
    val totalDays = (elapsed / (24 * 60 * 60 * 1000)).toInt()

    // Load skip counts from SharedPreferences
    val sharedPrefs = getStudyStateSharedPreferences(context)
    val allEntries = sharedPrefs?.all ?: return totalDays

    var daysToSubtract = 0

    for ((key, value) in allEntries) {
        if (key.startsWith("skipCount_") && value is Int) {
            val dateStr = key.removePrefix("skipCount_")
            val dateMillis = dateStr.toLongOrNull() ?: continue

            // Only consider dates after study start
            if (dateMillis >= startTimestamp && value > MAXIMUM_SKIPS) {
                daysToSubtract++
            }
        }
    }

    return (totalDays - daysToSubtract).coerceAtLeast(0)
}

private const val KEY_PARTICIPANT_ID = "participant_id"

fun saveParticipantId(context: Context, id: String) {
    val prefs = getStudyStateSharedPreferences(context)
    prefs?.edit()?.putString(KEY_PARTICIPANT_ID, id)?.apply()
}

fun getParticipantId(context: Context): String? {
    return getStudyStateSharedPreferences(context)?.getString(KEY_PARTICIPANT_ID, "")
}


data class TimeInterval(val start: String, val end: String)