package com.example.screenloggerdialogtest

import android.app.Service.WINDOW_SERVICE
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast

// variable name for Shared Preferences filename
const val STUDY_STATE_SHAREDPREFS : String = "STUDY_STATE_SHAREDPREFS"

// variable names for STUDY_STATE sharedPrefs objects
const val CONSENT_GIVEN: String = "CONSENT_GIVEN"
const val SPAI_1_SUBMITTED: String = "SPAI_1_SUBMITTED"
const val SASSV_1_SUBMITTED: String = "SASSV_1_SUBMITTED"
const val ONBOARDING_COMPLETED: String = "ONBOARDING_COMPLETED"
const val BASELINE_START_TIMESTAMP: String = "BASELINE_START_TIMESTAMP"
const val BASELINE_COMPLETED: String = "BASELINE_COMPLETED"
const val INT_SMARTPHONE_USAGE_LIMIT_GOAL: String = "INT_SMARTPHONE_USAGE_LIMIT_GOAL"
const val INT_BEDTIME: String = "INT_BEDTIME"
const val INT1_START_TIMESTAMP: String = "INT1_START_TIMESTAMP"
const val INT2_START_TIMESTAMP: String = "INT2_START_TIMESTAMP"
const val EXIT_SURVEY_COMPLETED: String = "EXIT_SURVEY_COMPLETED"
const val SPAI_2_SUBMITTED: String = "SPAI_2_SUBMITTED"
const val SASSV_2_SUBMITTED: String = "SASSV_2_SUBMITTED"
const val STUDY_COMPLETE_TIMESTAMP: String = "STUDY_COMPLETE_TIMESTAMP"

fun setStudyVariable(c : Context?, variable : String, value : Int) {
    var sharedPrefs = c?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
    var editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putInt(variable, value)
        editor.apply()
    }
    if (c != null) Toast.makeText(c, "Study variable $variable changed to $value", Toast.LENGTH_SHORT).show()
}

fun setStudyTimestamp(c: Context?, variable: String, value: Long) {
    val sharedPrefs = c?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
    val editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putLong(variable, value)
        editor.apply()
    }
    if (c != null) {
        Toast.makeText(c, "Study variable $variable changed to $value", Toast.LENGTH_SHORT).show()
    }
}

fun getStudyStateVariables(c: Context?): Map<String, Int?> {
    val sharedPrefs = c?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
    val studyVariables = mapOf(
        CONSENT_GIVEN to sharedPrefs?.getInt(CONSENT_GIVEN, 0),
        SPAI_1_SUBMITTED to sharedPrefs?.getInt(SPAI_1_SUBMITTED, 0),
        ONBOARDING_COMPLETED to sharedPrefs?.getInt(ONBOARDING_COMPLETED, 0),
        BASELINE_COMPLETED to sharedPrefs?.getInt(BASELINE_COMPLETED, 0),
        INT_SMARTPHONE_USAGE_LIMIT_GOAL to sharedPrefs?.getInt(INT_SMARTPHONE_USAGE_LIMIT_GOAL, 0),
        INT_BEDTIME to sharedPrefs?.getInt(INT_BEDTIME, 0),
        EXIT_SURVEY_COMPLETED to sharedPrefs?.getInt(EXIT_SURVEY_COMPLETED, 0),
        SPAI_2_SUBMITTED to sharedPrefs?.getInt(SPAI_2_SUBMITTED, 0),
    )
    return studyVariables
}

fun getStudyStateTimestamps(c: Context?): Map<String, Long?> {
    val sharedPrefs = c?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
    val studyVariables = mapOf(
        BASELINE_START_TIMESTAMP to sharedPrefs?.getLong(BASELINE_START_TIMESTAMP, 0L),
        INT1_START_TIMESTAMP to sharedPrefs?.getLong(INT1_START_TIMESTAMP, 0L),
        INT2_START_TIMESTAMP to sharedPrefs?.getLong(INT2_START_TIMESTAMP, 0L),
        STUDY_COMPLETE_TIMESTAMP to sharedPrefs?.getLong(STUDY_COMPLETE_TIMESTAMP, 0L)
    )
    return studyVariables
}

fun dayDifference(timestamp1: Long, timestamp2: Long): Int {
    return (kotlin.math.abs(timestamp1 - timestamp2) / (1000 * 60 * 60 * 24)).toInt()
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
    var sharedPrefs = c?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
    var editor = sharedPrefs?.edit()
    if (editor != null) {
        editor.putInt(STUDY_STATE, state)
        editor.apply()
    }
    if (c != null) Toast.makeText(c, "Study state changed to $state", Toast.LENGTH_SHORT).show()
}

fun getStudyState(c : Context?): Int {
    var sharedPrefs = c?.getSharedPreferences(STUDY_STATE_SHAREDPREFS, Context.MODE_PRIVATE)
    if (sharedPrefs != null) {
        return sharedPrefs.getInt(STUDY_STATE, 0)
    }
    else return 0
}

fun showDialog(c : Context?) {
    if (c == null) {
        return // Exit the function if context is null
    }

    val wm = c.getSystemService(WINDOW_SERVICE) as WindowManager
        ?: return // Exit the function if WindowManager is null

    val inflater = LayoutInflater.from(c)
    val dialogView = inflater.inflate(R.layout.dialog_layout_unlocked, null)
    val closeButton = dialogView.findViewById<Button>(R.id.close_button)
    // Set up the layout parameters for the WindowManager
    val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    )

    layoutParams.gravity = Gravity.CENTER // Center the view on the screen

    closeButton.setOnClickListener {
        wm.removeView(dialogView)
    }
    // Add the view to the window
    wm.addView(dialogView, layoutParams)

    Handler(Looper.getMainLooper()).postDelayed({
       wm.removeView(dialogView)
   }, 10000)
}

