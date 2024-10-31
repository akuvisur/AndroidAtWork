package com.example.screenloggerdialogtest

import android.app.Service.WINDOW_SERVICE
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast

const val STUDY_STATE_SHAREDPREFS : String = "STUDY_STATE_SHAREDPREFS"
const val CONSENT_GIVEN : String = "CONSENT_GIVEN"
const val STUDY_STATE : String = "STUDY_STATE"

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
}

