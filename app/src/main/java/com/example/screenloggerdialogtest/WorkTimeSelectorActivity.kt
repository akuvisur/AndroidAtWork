package com.example.screenloggerdialogtest

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class WorkTimeSelectorActivity : AppCompatActivity() {

    private lateinit var intervalContainer: LinearLayout
    private lateinit var addIntervalButton: Button
    private val maxIntervals = 5

    private val intervalList = mutableListOf<TimeInterval>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worktimeselector)

        intervalContainer = findViewById(R.id.intervalContainer)
        addIntervalButton = findViewById(R.id.addIntervalButton)

        addIntervalButton.setOnClickListener {
            if (intervalContainer.childCount < maxIntervals) {
                addIntervalRow()
            } else {
                Toast.makeText(this, "Max 5 intervals allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Clear any existing views
        intervalContainer.removeAllViews()
        intervalList.clear()

        // Load saved intervals
        val savedIntervals = getWorkIntervals(this)

        for (interval in savedIntervals) {
            addIntervalRow(interval.start, interval.end)
        }
    }


    override fun onStop() {
        super.onStop()
        // store intervals
        updateIntervalList()
        saveWorkIntervals(this, intervalList)
    }

    // Add a new interval row dynamically
    private fun addIntervalRow(startTime: String = "", endTime: String = "") {
        val row = layoutInflater.inflate(R.layout.worktimeinterval_row, null)

        val startButton = row.findViewById<Button>(R.id.startTimeButton)
        val endButton = row.findViewById<Button>(R.id.endTimeButton)
        val deleteButton = row.findViewById<ImageButton>(R.id.deleteButton)

        startButton.text = if (startTime.isNotEmpty()) startTime else "Start"
        endButton.text = if (endTime.isNotEmpty()) endTime else "End"

        startButton.setOnClickListener {
            showTimePicker { time ->
                startButton.text = time
                updateIntervalList()
            }
        }

        endButton.setOnClickListener {
            showTimePicker { time ->
                endButton.text = time
                updateIntervalList()
            }
        }

        deleteButton.setOnClickListener {
            intervalContainer.removeView(row)
            updateIntervalList()
        }

        intervalContainer.addView(row)
        updateIntervalList()
    }


    private fun updateIntervalList() {
        intervalList.clear()
        for (i in 0 until intervalContainer.childCount) {
            val row = intervalContainer.getChildAt(i)
            val startButton = row.findViewById<Button>(R.id.startTimeButton)
            val endButton = row.findViewById<Button>(R.id.endTimeButton)
            val start = startButton.text.toString()
            val end = endButton.text.toString()
            if (start.contains(":") && end.contains(":")) {
                intervalList.add(TimeInterval(start, end))
            }
        }
    }


    // Show a time picker dialog and return the selected time
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this,
            { _, hour, minute ->
                onTimeSelected(String.format("%02d:%02d", hour, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }
}
