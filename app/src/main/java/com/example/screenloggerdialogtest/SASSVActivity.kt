package com.example.screenloggerdialogtest

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SASSVActivity : AppCompatActivity() {

    // Define SAS-SV questions as a map
    private val listOfQuestions = mapOf(
        "SASSV1" to "Missing planned work due to smartphone use.",
        "SASSV2" to "Having a hard time concentrating in class, while doing assignments, or while working due to smartphone use.",
        "SASSV3" to "Feeling pain in the wrists or at the back of the neck while using a smartphone.",
        "SASSV4" to "Will not be able to stand not having a smartphone.",
        "SASSV5" to "Feeling impatient and fretful when I am not holding my smartphone.",
        "SASSV6" to "Having my smartphone in my mind even when I am not using it.",
        "SASSV7" to "I will never give up using my smartphone even when my daily life is already greatly affected by it.",
        "SASSV8" to "Constantly checking my smartphone so as not to miss conversations on social media.",
        "SASSV9" to "Using my smartphone longer than I had intended.",
        "SASSV10" to "The people around me tell me that I use my smartphone too much."
    )

    private val responses = mutableMapOf<String, SASSVAnswer>()
    private lateinit var submitButton: Button
    private lateinit var source: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sassv)

        source = intent.getStringExtra("source").toString()

        val questionsContainer = findViewById<LinearLayout>(R.id.questions_container)
        submitButton = findViewById(R.id.submit_button)

        // Loop through each question in the list
        listOfQuestions.forEach { (sassv_id, questionText) ->
            val options = arrayOf(
                "1", // Strongly Disagree
                "2", // Disagree
                "3", // Weakly Disagree
                "4", // Weakly Agree
                "5", // Agree
                "6"  // Strongly Agree
            )

            val questionLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
                setPadding(0, 16, 0, 16)

                background = GradientDrawable().apply {
                    setColor(Color.WHITE) // Background color
                    setStroke(1, Color.LTGRAY) // Border color
                    cornerRadius = 8f // Rounded corners
                }
                elevation = 4f // Shadow elevation
            }

            // TextView with question and red asterisk
            val questionTextView = TextView(this).apply {
                text = "* $questionText"
                setTextColor(Color.RED)
                setPadding(8, 0, 8, 0)
            }
            questionLayout.addView(questionTextView)

            // RadioGroup for response options
            val radioGroup = RadioGroup(this).apply {
                orientation = RadioGroup.HORIZONTAL
                setOnCheckedChangeListener { _, checkedId ->
                    val response = checkedId
                    val SASSVAnswer = SASSVAnswer(sassv_id, response)
                    updateResponse(sassv_id, SASSVAnswer)
                    updateQuestionText(questionTextView, questionText, true)
                    checkAllAnswered()
                }
            }

            // Add four radio buttons to the radio group
            var but_id = 0
            options.forEachIndexed { i, option ->
                val radioButton = RadioButton(this).apply {
                    but_id += 1
                    id = but_id
                    text = option
                }
                radioButton.setOnClickListener {
                    Log.d("SASSV", "clicked + $questionText")
                }
                radioGroup.addView(radioButton)
            }

            val preText = TextView(this).apply {
                text = "1 = strongly disagree, 2 = disagree, 3 = weakly disagree, ..., 6 = strongly agree"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f) // Set size in SP
                setTypeface(null, Typeface.ITALIC) // Set text to italic
            }

            questionLayout.addView(preText)
            questionLayout.addView(radioGroup)
            questionsContainer.addView(questionLayout)
        }

        submitButton.setOnClickListener {
            // Save responses logic here, e.g., storing them to database or SharedPreferences
            Toast.makeText(this, "Responses submitted!", Toast.LENGTH_SHORT).show()

            Log.d("SPAI", responses.toString())

            if (source == "consent_given") {
                setStudyVariable(this, SASSV_1_SUBMITTED, 1)
            }
            else {
                setStudyVariable(this, SASSV_2_SUBMITTED, 1)
            }
            finish()
        }
    }

    private fun updateResponse(id: String, SASSVAnswer: SASSVAnswer) {
        responses[id] = SASSVAnswer  // Add or update the response in the map by ID
    }

    private fun updateQuestionText(textView: TextView, question: String, answered: Boolean) {
        textView.text = if (answered) question else "* $question"
        textView.setTextColor(if (answered) Color.BLACK else Color.RED)
    }

    private fun checkAllAnswered() {
        // Enable submit button only if all questions are answered
        submitButton.isEnabled = responses.size == listOfQuestions.size
    }
}

data class SASSVAnswer(val questionId: String, val response: Int)