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
    private val maxIntervals = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worktimeselector)

        intervalContainer = findViewById(R.id.intervalContainer)
        addIntervalButton = findViewById(R.id.addIntervalButton)

        addIntervalButton.setOnClickListener {
            if (intervalContainer.childCount < maxIntervals) {
                addIntervalRow()
            } else {
                Toast.makeText(this, "Max 3 intervals allowed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Add a new interval row dynamically
    private fun addIntervalRow() {
        val row = layoutInflater.inflate(R.layout.worktimeinterval_row, null)

        val startButton = row.findViewById<Button>(R.id.startTimeButton)
        val endButton = row.findViewById<Button>(R.id.endTimeButton)
        val deleteButton = row.findViewById<ImageButton>(R.id.deleteButton)

        startButton.setOnClickListener {
            showTimePicker { time -> startButton.text = time }
        }

        endButton.setOnClickListener {
            showTimePicker { time -> endButton.text = time }
        }

        deleteButton.setOnClickListener {
            intervalContainer.removeView(row)
        }

        intervalContainer.addView(row)
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
